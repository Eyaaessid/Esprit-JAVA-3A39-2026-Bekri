package tn.esprit.user.dao;

import tn.esprit.user.entity.Admin;
import tn.esprit.user.entity.Coach;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UtilisateurDao {
    private final TwoFactorDAO twoFactorDAO = new TwoFactorDAO();

    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public Optional<Utilisateur> findByEmail(String email) {
        String sql = "SELECT * FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Utilisateur user = mapRow(rs);
                    twoFactorDAO.loadTwoFactorFields(user);
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Utilisateur> findById(Integer id) {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Utilisateur user = mapRow(rs);
                    twoFactorDAO.loadTwoFactorFields(user);
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Utilisateur> findAll() {
        String sql = "SELECT * FROM utilisateur ORDER BY id DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Utilisateur> list = new ArrayList<>();
            while (rs.next()) {
                Utilisateur user = mapRow(rs);
                twoFactorDAO.loadTwoFactorFields(user);
                list.add(user);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Utilisateur save(Utilisateur u) {
        try {
            if (u.getId() == null) {
                String sql = "INSERT INTO utilisateur "
                        + "(nom, prenom, email, mot_de_passe, role, statut, avatar, telephone, date_naissance, created_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,NOW())";
                try (PreparedStatement ps = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u.getNom());
                    ps.setString(2, u.getPrenom());
                    ps.setString(3, u.getEmail());
                    ps.setString(4, u.getMotDePasse());
                    ps.setString(5, u.getRole() != null ? u.getRole().name() : null);
                    ps.setString(6, u.getStatut() != null ? u.getStatut().name() : null);
                    ps.setString(7, u.getPhotoProfil());
                    ps.setString(8, u.getTelephone());
                    if (u.getDateNaissance() != null) {
                        ps.setDate(9, Date.valueOf(u.getDateNaissance()));
                    } else {
                        ps.setObject(9, null);
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            u.setId(keys.getInt(1));
                        }
                    }
                }
                return u;
            }

            String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mot_de_passe=?, role=?, "
                    + "statut=?, avatar=?, telephone=?, date_naissance=?, updated_at=NOW() WHERE id=?";
            try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
                ps.setString(1, u.getNom());
                ps.setString(2, u.getPrenom());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getMotDePasse());
                ps.setString(5, u.getRole() != null ? u.getRole().name() : null);
                ps.setString(6, u.getStatut() != null ? u.getStatut().name() : null);
                ps.setString(7, u.getPhotoProfil());
                ps.setString(8, u.getTelephone());
                if (u.getDateNaissance() != null) {
                    ps.setDate(9, Date.valueOf(u.getDateNaissance()));
                } else {
                    ps.setObject(9, null);
                }
                ps.setInt(10, u.getId());
                ps.executeUpdate();
            }
            return u;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        String roleStr = rs.getString("role");
        if (roleStr != null && !roleStr.isBlank()) {
            u.setRole(UtilisateurRole.fromString(roleStr.trim()));
        }
        String statutStr = rs.getString("statut");
        if (statutStr != null && !statutStr.isBlank()) {
            u.setStatut(UtilisateurStatut.fromString(statutStr.trim()));
        }
        u.setPhotoProfil(rs.getString("avatar"));
        Timestamp ca = rs.getTimestamp("created_at");
        u.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        u.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        try {
            u.setResetToken(rs.getString("reset_token"));
        } catch (SQLException ignored) {
            u.setResetToken(null);
        }
        try {
            Timestamp rt = rs.getTimestamp("reset_token_expires_at");
            u.setResetTokenExpiresAt(rt != null ? rt.toLocalDateTime() : null);
        } catch (SQLException ignored) {
            u.setResetTokenExpiresAt(null);
        }
        try {
            u.setVerified(rs.getBoolean("is_verified"));
        } catch (SQLException ignored) {
            u.setVerified(false);
        }
        try {
            Timestamp ll = rs.getTimestamp("last_login_at");
            u.setLastLoginAt(ll != null ? ll.toLocalDateTime() : null);
        } catch (SQLException ignored) {
            u.setLastLoginAt(null);
        }
        try {
            u.setFaceDescriptor(rs.getString("face_descriptor"));
        } catch (SQLException ignored) {
            u.setFaceDescriptor(null);
        }
        try {
            u.setFaceAuthEnabled(rs.getBoolean("face_auth_enabled"));
        } catch (SQLException ignored) {
            u.setFaceAuthEnabled(false);
        }
        try {
            Timestamp fr = rs.getTimestamp("face_registered_at");
            u.setFaceRegisteredAt(fr != null ? fr.toLocalDateTime() : null);
        } catch (SQLException ignored) {
            u.setFaceRegisteredAt(null);
        }
        try {
            u.setFaceAuthFailedAttempts(rs.getInt("face_auth_failed_attempts"));
        } catch (SQLException ignored) {
            u.setFaceAuthFailedAttempts(0);
        }
        try {
            Timestamp la = rs.getTimestamp("last_face_auth_attempt_at");
            u.setLastFaceAuthAttemptAt(la != null ? la.toLocalDateTime() : null);
        } catch (SQLException ignored) {
            u.setLastFaceAuthAttemptAt(null);
        }
        try {
            u.setTelephone(rs.getString("telephone"));
        } catch (SQLException ignored) {
            u.setTelephone(null);
        }
        try {
            Date d = rs.getDate("date_naissance");
            u.setDateNaissance(d != null ? d.toLocalDate() : null);
        } catch (SQLException ignored) {
            u.setDateNaissance(null);
        }
        return u;
    }

    public Utilisateur findByEmailWithSubtype(String email) {
        Utilisateur user = findByEmail(email).orElse(null);
        if (user == null || user.getRole() == null) {
            return user;
        }
        return switch (user.getRole()) {
            case ADMIN -> {
                Admin admin = new Admin();
                copyFields(user, admin);
                yield admin;
            }
            case COACH -> {
                Coach coach = new Coach();
                copyFields(user, coach);
                yield coach;
            }
            default -> user;
        };
    }

    public void updateRoleAndStatus(int userId, UtilisateurRole role, UtilisateurStatut statut) {
        String sql = "UPDATE utilisateur SET role = ?, statut = ? WHERE id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, role != null ? role.name() : null);
            ps.setString(2, statut != null ? statut.name() : null);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyFields(Utilisateur src, Utilisateur dst) {
        dst.setId(src.getId());
        dst.setNom(src.getNom());
        dst.setPrenom(src.getPrenom());
        dst.setEmail(src.getEmail());
        dst.setMotDePasse(src.getMotDePasse());
        dst.setRole(src.getRole());
        dst.setStatut(src.getStatut());
        dst.setPhotoProfil(src.getPhotoProfil());
        dst.setCreatedAt(src.getCreatedAt());
        dst.setUpdatedAt(src.getUpdatedAt());
        dst.setTelephone(src.getTelephone());
        dst.setDateNaissance(src.getDateNaissance());
        dst.setResetToken(src.getResetToken());
        dst.setResetTokenExpiresAt(src.getResetTokenExpiresAt());
        dst.setVerified(src.isVerified());
        dst.setLastLoginAt(src.getLastLoginAt());
        dst.setFaceDescriptor(src.getFaceDescriptor());
        dst.setFaceAuthEnabled(src.isFaceAuthEnabled());
        dst.setFaceRegisteredAt(src.getFaceRegisteredAt());
        dst.setFaceAuthFailedAttempts(src.getFaceAuthFailedAttempts());
        dst.setLastFaceAuthAttemptAt(src.getLastFaceAuthAttemptAt());
        dst.setTotpSecret(src.getTotpSecret());
        dst.setTwoFactorEnabled(src.isTwoFactorEnabled());
        dst.setBackupCodes(src.getBackupCodes());
        dst.setTwoFactorEnabledAt(src.getTwoFactorEnabledAt());
    }

    // Reset password
    public void saveResetToken(int userId, String token, LocalDateTime expiresAt) {
        String sql = "UPDATE utilisateur SET reset_token=?, reset_token_expires_at=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, expiresAt != null ? Timestamp.valueOf(expiresAt) : null);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Utilisateur findByResetToken(String token) {
        String sql = "SELECT * FROM utilisateur WHERE reset_token=? LIMIT 1";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Utilisateur user = mapRow(rs);
                    twoFactorDAO.loadTwoFactorFields(user);
                    return user;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearResetToken(int userId) {
        String sql = "UPDATE utilisateur SET reset_token=NULL, reset_token_expires_at=NULL, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updatePassword(int userId, String hashedPassword) {
        String sql = "UPDATE utilisateur SET mot_de_passe=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Email verification (reuses reset_token column while is_verified=0)
    public void saveVerificationToken(int userId, String token, LocalDateTime expiresAt) {
        String sql = "UPDATE utilisateur SET reset_token=?, reset_token_expires_at=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, expiresAt != null ? Timestamp.valueOf(expiresAt) : null);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Utilisateur findByVerificationToken(String token) {
        String sql = "SELECT * FROM utilisateur WHERE reset_token=? AND is_verified=0 LIMIT 1";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Utilisateur user = mapRow(rs);
                    twoFactorDAO.loadTwoFactorFields(user);
                    return user;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setVerified(int userId) {
        String sql = "UPDATE utilisateur SET is_verified=1, reset_token=NULL, reset_token_expires_at=NULL, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE utilisateur SET last_login_at=NOW() WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> fetchRoleStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("total", countBySql("SELECT COALESCE(COUNT(*), 0) FROM utilisateur"));
        stats.put("user", countBySql("SELECT COALESCE(COUNT(*), 0) FROM utilisateur WHERE UPPER(role) = 'USER'"));
        stats.put("coach", countBySql("SELECT COALESCE(COUNT(*), 0) FROM utilisateur WHERE UPPER(role) = 'COACH'"));
        stats.put("admin", countBySql("SELECT COALESCE(COUNT(*), 0) FROM utilisateur WHERE UPPER(role) = 'ADMIN'"));
        return stats;
    }

    private int countBySql(String sql) {
        try (PreparedStatement ps = getCnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(int userId) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new RuntimeException("Aucun utilisateur supprimé — id introuvable : " + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression (id=" + userId + ") : " + e.getMessage(), e);
        }
    }
}
