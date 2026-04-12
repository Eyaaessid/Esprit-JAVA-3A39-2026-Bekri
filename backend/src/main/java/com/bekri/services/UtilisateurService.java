package com.bekri.services;

import com.bekri.dto.request.LoginDTO;
import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.request.UpdateMeRequest;
import com.bekri.dto.request.UtilisateurRequestDTO;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.Admin;
import com.bekri.entities.Coach;
import com.bekri.entities.Utilisateur;
import com.bekri.entities.User;
import com.bekri.enums.UtilisateurRole;
import com.bekri.enums.UtilisateurStatut;
import com.bekri.exceptions.ResourceNotFoundException;
import com.bekri.repositories.ProfilPsychologiqueRepository;
import com.bekri.repositories.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilPsychologiqueRepository profilPsychologiqueRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Value("${app.files.avatar-dir:./data/avatars}")
    private String avatarStorageDir;

    @Transactional(readOnly = true)
    public UtilisateurResponseDTO getUtilisateurById(Integer id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        return toResponseDTO(u);
    }

    @Transactional
    public UtilisateurResponseDTO createUtilisateur(UtilisateurRequestDTO dto) {
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        Utilisateur entity = newUtilisateurForRole(dto.getRole());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        entity.setTelephone(dto.getTelephone());
        entity.setDateNaissance(dto.getDateNaissance());
        entity.setStatut(UtilisateurStatut.ACTIF);
        return toResponseDTO(utilisateurRepository.save(entity));
    }

    @Transactional
    public UtilisateurResponseDTO updateUtilisateur(Integer id, UtilisateurRequestDTO dto) {
        Utilisateur entity = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur current
                && current.getId() != null && current.getId().equals(id)
                && !isAdminPrincipal(current)
                && dto.getRole() != null
                && !dto.getRole().getValue().equalsIgnoreCase(entity.getRoleValue())) {
            throw new IllegalArgumentException("Vous ne pouvez pas modifier votre rôle");
        }
        if (utilisateurRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setDateNaissance(dto.getDateNaissance());
        if (dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank()) {
            entity.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }
        return toResponseDTO(utilisateurRepository.save(entity));
    }

    @Transactional
    public void deleteUtilisateur(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur current
                && current.getId() != null && current.getId().equals(id)) {
            throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
        }
        Utilisateur entity = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        entity.setStatut(UtilisateurStatut.SUPPRIME);
        entity.setDeactivatedAt(LocalDateTime.now());
        entity.setDeactivatedBy(currentActorLabel());
        utilisateurRepository.save(entity);
    }

    /**
     * Suppression physique (admin) : profil psychologique puis utilisateur.
     */
    @Transactional
    public void hardDeleteUtilisateur(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur current
                && current.getId() != null && current.getId().equals(id)) {
            throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
        }
        Utilisateur entity = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        profilPsychologiqueRepository.findByUtilisateurId(id).ifPresent(profilPsychologiqueRepository::delete);
        entityManager.flush();
        utilisateurRepository.delete(entity);
    }

    public boolean isSelf(Integer userId) {
        if (userId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getPrincipal() instanceof Utilisateur u) {
            return u.getId() != null && u.getId().equals(userId);
        }
        return false;
    }

    private static boolean isAdminPrincipal(Utilisateur u) {
        return u.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Transactional
    public UtilisateurResponseDTO register(RegisterDTO dto) {
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        Utilisateur entity = new User();
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        entity.setTelephone(dto.getTelephone());
        entity.setDateNaissance(dto.getDateNaissance());
        entity.setStatut(UtilisateurStatut.ACTIF);
        return toResponseDTO(utilisateurRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public UtilisateurResponseDTO login(LoginDTO dto) {
        Utilisateur u = utilisateurRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (!passwordEncoder.matches(dto.getMotDePasse(), u.getMotDePasse())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }
        return toResponseDTO(u);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponseDTO> search(String search, String role, String statut) {
        UtilisateurStatut s = (statut == null || statut.isBlank()) ? null : UtilisateurStatut.fromDbValue(statut);
        String r = (role == null || role.isBlank()) ? null : role.trim().toLowerCase(Locale.ROOT);
        return utilisateurRepository.search(search, r, s).stream().map(this::toResponseDTO).toList();
    }

    /**
     * Updates the role discriminator column via a native SQL query.
     * JPA SINGLE_TABLE does not allow updating the discriminator column via entity save,
     * so we bypass JPA entirely and flush the cache afterwards.
     */
    @Transactional
    public UtilisateurResponseDTO updateRole(Integer id, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role est requis");
        }
        String r = role.trim().toLowerCase(Locale.ROOT);
        if (!r.equals("user") && !r.equals("coach") && !r.equals("admin")) {
            throw new IllegalArgumentException("role invalide: " + role);
        }
        if (!utilisateurRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable : " + id);
        }
        utilisateurRepository.updateRoleById(id, r);
        // Clear the JPA first-level cache so the next read reflects the new role value
        entityManager.flush();
        entityManager.clear();
        return getUtilisateurById(id);
    }

    @Transactional
    public UtilisateurResponseDTO updateStatut(Integer id, String statut) {
        Utilisateur entity = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        UtilisateurStatut s = UtilisateurStatut.fromDbValue(statut);
        entity.setStatut(s);
        if (s == UtilisateurStatut.SUPPRIME) {
            entity.setDeactivatedAt(LocalDateTime.now());
            entity.setDeactivatedBy(currentActorLabel());
        } else {
            entity.setDeactivatedAt(null);
            entity.setDeactivatedBy(null);
        }
        return toResponseDTO(utilisateurRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public UtilisateurResponseDTO me(Utilisateur principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Non authentifié");
        }
        return getUtilisateurById(principal.getId());
    }

    @Transactional
    public UtilisateurResponseDTO updateMe(Utilisateur principal, UpdateMeRequest dto) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Non authentifié");
        }
        Utilisateur entity = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + principal.getId()));
        if (utilisateurRepository.existsByEmailAndIdNot(dto.getEmail(), entity.getId())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setDateNaissance(dto.getDateNaissance());
        entity.setAvatar(dto.getAvatar());
        if (dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank()) {
            entity.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }
        return toResponseDTO(utilisateurRepository.save(entity));
    }

    @Transactional
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
        Utilisateur entity = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + principal.getId()));
        entity.setAvatar(publicPath);
        return toResponseDTO(utilisateurRepository.save(entity));
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

    private static Utilisateur newUtilisateurForRole(UtilisateurRole role) {
        if (role == null) return new User();
        return switch (role) {
            case ADMIN -> new Admin();
            case COACH -> new Coach();
            case USER -> new User();
        };
    }

    private static String currentActorLabel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (p instanceof Utilisateur u) return u.getEmail();
        return auth.getName();
    }
}