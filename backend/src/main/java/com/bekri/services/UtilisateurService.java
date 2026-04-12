package com.bekri.services;

import com.bekri.dto.request.LoginDTO;
import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.request.UpdateMeRequest;
import com.bekri.dto.request.UtilisateurRequestDTO;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.enums.UtilisateurStatut;
import com.bekri.exceptions.ResourceNotFoundException;
import com.bekri.utils.MyDataBase;
import com.bekri.utils.UtilisateurResultSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final Connection cnx = MyDataBase.getInstance().getCnx();
    private final PasswordEncoder passwordEncoder;

    @Value("${app.files.avatar-dir:./data/avatars}")
    private String avatarStorageDir;

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws Exception;
    }

    private void inTransaction(SqlRunnable r) {
        boolean prevAuto = true;
        try {
            prevAuto = cnx.getAutoCommit();
            cnx.setAutoCommit(false);
            r.run();
            cnx.commit();
        } catch (Exception e) {
            try {
                cnx.rollback();
            } catch (SQLException ignored) {
                // ignore
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        } finally {
            try {
                cnx.setAutoCommit(prevAuto);
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

    public UtilisateurResponseDTO getUtilisateurById(Integer id) {
        try {
            String sql = "SELECT * FROM utilisateur WHERE id = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
            }
            Utilisateur u = UtilisateurResultSetMapper.mapRow(rs);
            rs.close();
            ps.close();
            return toResponseDTO(u);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO createUtilisateur(UtilisateurRequestDTO dto) {
        try {
            if (existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }
            String roleDb = dto.getRole() != null ? dto.getRole().getValue() : "user";

            String sql = """
                    INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, telephone, date_naissance, role, statut, created_at)
                    VALUES (?,?,?,?,?,?,?,?,NOW())
                    """;
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getNom());
            ps.setString(2, dto.getPrenom());
            ps.setString(3, dto.getEmail());
            ps.setString(4, passwordEncoder.encode(dto.getMotDePasse()));
            ps.setString(5, dto.getTelephone());
            if (dto.getDateNaissance() != null) {
                ps.setDate(6, Date.valueOf(dto.getDateNaissance()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setString(7, roleDb);
            ps.setString(8, UtilisateurStatut.ACTIF.getValue());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) {
                throw new IllegalStateException("Impossible de récupérer l'id généré");
            }
            int newId = keys.getInt(1);
            keys.close();
            ps.close();
            return getUtilisateurById(newId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO updateUtilisateur(Integer id, UtilisateurRequestDTO dto) {
        try {
            Utilisateur entity = findByIdOrThrow(id);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Utilisateur current
                    && current.getId() != null && current.getId().equals(id)
                    && !isAdminPrincipal(current)
                    && dto.getRole() != null
                    && !dto.getRole().getValue().equalsIgnoreCase(entity.getRoleValue())) {
                throw new IllegalArgumentException("Vous ne pouvez pas modifier votre rôle");
            }
            if (existsByEmailAndIdNot(dto.getEmail(), id)) {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }

            boolean changePwd = dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank();
            String sql = changePwd
                    ? """
                    UPDATE utilisateur SET nom=?, prenom=?, email=?, mot_de_passe=?, telephone=?, date_naissance=?, updated_at=NOW()
                    WHERE id=?
                    """
                    : """
                    UPDATE utilisateur SET nom=?, prenom=?, email=?, telephone=?, date_naissance=?, updated_at=NOW()
                    WHERE id=?
                    """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, dto.getNom());
            ps.setString(2, dto.getPrenom());
            ps.setString(3, dto.getEmail());
            int idx = 4;
            if (changePwd) {
                ps.setString(idx++, passwordEncoder.encode(dto.getMotDePasse()));
            }
            ps.setString(idx++, dto.getTelephone());
            if (dto.getDateNaissance() != null) {
                ps.setDate(idx++, Date.valueOf(dto.getDateNaissance()));
            } else {
                ps.setNull(idx++, Types.DATE);
            }
            ps.setInt(idx, id);
            int n = ps.executeUpdate();
            ps.close();
            if (n == 0) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
            }
            return getUtilisateurById(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteUtilisateur(Integer id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Utilisateur current
                    && current.getId() != null && current.getId().equals(id)) {
                throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
            }
            if (!existsById(id)) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
            }
            String sql = """
                    UPDATE utilisateur SET statut='supprime', deactivated_at=NOW(), deactivated_by=?, updated_at=NOW()
                    WHERE id=?
                    """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, currentActorLabel());
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void hardDeleteUtilisateur(Integer id) {
        inTransaction(() -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Utilisateur current
                    && current.getId() != null && current.getId().equals(id)) {
                throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
            }
            if (!existsById(id)) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
            }
            String delProfil = "DELETE FROM profil_psychologique WHERE utilisateur_id=?";
            try (PreparedStatement ps1 = cnx.prepareStatement(delProfil)) {
                ps1.setInt(1, id);
                ps1.executeUpdate();
            }
            String delUser = "DELETE FROM utilisateur WHERE id=?";
            try (PreparedStatement ps2 = cnx.prepareStatement(delUser)) {
                ps2.setInt(1, id);
                int n = ps2.executeUpdate();
                if (n == 0) {
                    throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
                }
            }
        });
    }

    public boolean isSelf(Integer userId) {
        if (userId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (auth.getPrincipal() instanceof Utilisateur u) {
            return u.getId() != null && u.getId().equals(userId);
        }
        return false;
    }

    private static boolean isAdminPrincipal(Utilisateur u) {
        return u.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public UtilisateurResponseDTO register(RegisterDTO dto) {
        inTransaction(() -> {
            try {
                if (existsByEmail(dto.getEmail())) {
                    throw new IllegalArgumentException("Cet email est déjà utilisé");
                }
                String sql = """
                        INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, telephone, date_naissance, role, statut, created_at)
                        VALUES (?,?,?,?,?,?,?,?,NOW())
                        """;
                PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, dto.getNom());
                ps.setString(2, dto.getPrenom());
                ps.setString(3, dto.getEmail());
                ps.setString(4, passwordEncoder.encode(dto.getMotDePasse()));
                ps.setString(5, dto.getTelephone());
                if (dto.getDateNaissance() != null) {
                    ps.setDate(6, Date.valueOf(dto.getDateNaissance()));
                } else {
                    ps.setNull(6, Types.DATE);
                }
                ps.setString(7, "user");
                ps.setString(8, UtilisateurStatut.ACTIF.getValue());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return loginDtoToResponse(dto.getEmail());
    }

    public UtilisateurResponseDTO login(LoginDTO dto) {
        try {
            String sql = "SELECT * FROM utilisateur WHERE email = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, dto.getEmail());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ResourceNotFoundException("Utilisateur introuvable");
            }
            Utilisateur u = UtilisateurResultSetMapper.mapRow(rs);
            rs.close();
            ps.close();
            if (!passwordEncoder.matches(dto.getMotDePasse(), u.getMotDePasse())) {
                throw new IllegalArgumentException("Email ou mot de passe incorrect");
            }
            return toResponseDTO(u);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private UtilisateurResponseDTO loginDtoToResponse(String email) {
        try {
            String sql = "SELECT * FROM utilisateur WHERE email = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new ResourceNotFoundException("Utilisateur introuvable");
            }
            Utilisateur u = UtilisateurResultSetMapper.mapRow(rs);
            rs.close();
            ps.close();
            return toResponseDTO(u);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<UtilisateurResponseDTO> search(String search, String role, String statut) {
        try {
            UtilisateurStatut s = (statut == null || statut.isBlank()) ? null : UtilisateurStatut.fromDbValue(statut);
            String r = (role == null || role.isBlank()) ? null : role.trim().toLowerCase(Locale.ROOT);

            StringBuilder sql = new StringBuilder("SELECT * FROM utilisateur WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search + "%";
                sql.append(" AND (nom LIKE ? OR prenom LIKE ? OR email LIKE ?)");
                params.add(like);
                params.add(like);
                params.add(like);
            }
            if (r != null) {
                sql.append(" AND LOWER(role) = LOWER(?)");
                params.add(r);
            }
            if (s != null) {
                sql.append(" AND statut = ?");
                params.add(s.getValue());
            }
            sql.append(" ORDER BY id DESC");

            PreparedStatement ps = cnx.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            List<UtilisateurResponseDTO> out = new ArrayList<>();
            while (rs.next()) {
                out.add(toResponseDTO(UtilisateurResultSetMapper.mapRow(rs)));
            }
            rs.close();
            ps.close();
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO updateRole(Integer id, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role est requis");
        }
        String r = role.trim().toLowerCase(Locale.ROOT);
        if (!r.equals("user") && !r.equals("coach") && !r.equals("admin")) {
            throw new IllegalArgumentException("role invalide: " + role);
        }
        try {
            if (!existsById(id)) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
            }
            String sql = "UPDATE utilisateur SET role=?, updated_at=NOW() WHERE id=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, r);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
            return getUtilisateurById(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO updateStatut(Integer id, String statut) {
        try {
            if (!existsById(id)) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
            }
            UtilisateurStatut s = UtilisateurStatut.fromDbValue(statut);
            if (s == UtilisateurStatut.SUPPRIME) {
                String sql = """
                        UPDATE utilisateur SET statut=?, deactivated_at=NOW(), deactivated_by=?, updated_at=NOW()
                        WHERE id=?
                        """;
                PreparedStatement ps = cnx.prepareStatement(sql);
                ps.setString(1, s.getValue());
                ps.setString(2, currentActorLabel());
                ps.setInt(3, id);
                ps.executeUpdate();
                ps.close();
            } else if (s == UtilisateurStatut.ACTIF) {
                String sql = """
                        UPDATE utilisateur SET statut=?, deactivated_at=NULL, deactivated_by=NULL, updated_at=NOW()
                        WHERE id=?
                        """;
                PreparedStatement ps = cnx.prepareStatement(sql);
                ps.setString(1, s.getValue());
                ps.setInt(2, id);
                ps.executeUpdate();
                ps.close();
            } else {
                String sql = "UPDATE utilisateur SET statut=?, updated_at=NOW() WHERE id=?";
                PreparedStatement ps = cnx.prepareStatement(sql);
                ps.setString(1, s.getValue());
                ps.setInt(2, id);
                ps.executeUpdate();
                ps.close();
            }
            return getUtilisateurById(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO me(Utilisateur principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Non authentifié");
        }
        return getUtilisateurById(principal.getId());
    }

    public UtilisateurResponseDTO updateMe(Utilisateur principal, UpdateMeRequest dto) {
        try {
            if (principal == null || principal.getId() == null) {
                throw new IllegalArgumentException("Non authentifié");
            }
            if (existsByEmailAndIdNot(dto.getEmail(), principal.getId())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }
            boolean changePwd = dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank();
            String sql = changePwd
                    ? """
                    UPDATE utilisateur SET nom=?, prenom=?, email=?, telephone=?, date_naissance=?, avatar=?, mot_de_passe=?, updated_at=NOW()
                    WHERE id=?
                    """
                    : """
                    UPDATE utilisateur SET nom=?, prenom=?, email=?, telephone=?, date_naissance=?, avatar=?, updated_at=NOW()
                    WHERE id=?
                    """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, dto.getNom());
            ps.setString(2, dto.getPrenom());
            ps.setString(3, dto.getEmail());
            ps.setString(4, dto.getTelephone());
            if (dto.getDateNaissance() != null) {
                ps.setDate(5, Date.valueOf(dto.getDateNaissance()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, dto.getAvatar());
            int idx = 7;
            if (changePwd) {
                ps.setString(idx++, passwordEncoder.encode(dto.getMotDePasse()));
            }
            ps.setInt(idx, principal.getId());
            int n = ps.executeUpdate();
            ps.close();
            if (n == 0) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + principal.getId());
            }
            return getUtilisateurById(principal.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO uploadMyAvatar(Utilisateur principal, MultipartFile file) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Non authentifié");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier requis");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("L'image ne doit pas dépasser 2 Mo.");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_AVATAR_TYPES.contains(ct.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Format d'image non supporté (JPEG, PNG, GIF, WEBP uniquement).");
        }
        String ext = switch (ct.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
        Path dir = Paths.get(avatarStorageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de stockage", e);
        }
        String filename = "avatar_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Chemin de fichier invalide");
        }
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Échec de l'enregistrement du fichier", e);
        }
        String publicPath = "/uploads/avatars/" + filename;
        try {
            String sql = "UPDATE utilisateur SET avatar=?, updated_at=NOW() WHERE id=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, publicPath);
            ps.setInt(2, principal.getId());
            int n = ps.executeUpdate();
            ps.close();
            if (n == 0) {
                throw new ResourceNotFoundException("Utilisateur introuvable : " + principal.getId());
            }
            return getUtilisateurById(principal.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UtilisateurResponseDTO toResponseDTO(Utilisateur u) {
        return UtilisateurResponseDTO.builder()
                .id(u.getId())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .email(u.getEmail())
                .telephone(u.getTelephone())
                .dateNaissance(u.getDateNaissance())
                .role(u.getRoleValue())
                .statut(u.getStatut())
                .createdAt(u.getCreatedAt())
                .avatar(u.getAvatar())
                .build();
    }

    private Utilisateur findByIdOrThrow(Integer id) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            rs.close();
            ps.close();
            throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
        }
        Utilisateur u = UtilisateurResultSetMapper.mapRow(rs);
        rs.close();
        ps.close();
        return u;
    }

    private boolean existsById(Integer id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        boolean ok = rs.getInt(1) > 0;
        rs.close();
        ps.close();
        return ok;
    }

    private boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        rs.next();
        boolean ok = rs.getInt(1) > 0;
        rs.close();
        ps.close();
        return ok;
    }

    private boolean existsByEmailAndIdNot(String email, Integer id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email=? AND id != ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ps.setInt(2, id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        boolean ok = rs.getInt(1) > 0;
        rs.close();
        ps.close();
        return ok;
    }

    private static String currentActorLabel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object p = auth.getPrincipal();
        if (p instanceof Utilisateur u) {
            return u.getEmail();
        }
        return auth.getName();
    }
}
