package com.bekri.entities;

import com.bekri.converters.UtilisateurStatutConverter;
import com.bekri.enums.UtilisateurStatut;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 50)
public abstract class Utilisateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 100, nullable = false)
    private String prenom;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe", length = 255, nullable = false)
    private String motDePasse;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "avatar", length = 500)
    private String avatar;

    /**
     * Colonne DB utilisée aussi comme discriminator. Lecture seule côté entité.
     * La valeur réelle est pilotée par le type JPA (User/Coach/Admin) ou par une requête SQL dédiée.
     */
    @Column(name = "role", length = 50, insertable = false, updatable = false)
    private String role;

    @Convert(converter = UtilisateurStatutConverter.class)
    @Column(name = "statut", length = 50)
    private UtilisateurStatut statut = UtilisateurStatut.ACTIF;

    @Column(name = "score_initial")
    private Integer scoreInitial;

    @Column(name = "date_evaluation_initiale")
    private LocalDate dateEvaluationInitiale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivated_by", length = 50)
    private String deactivatedBy;

    @Column(name = "reactivation_token", length = 100)
    private String reactivationToken;

    @Column(name = "reactivation_token_expires_at")
    private LocalDateTime reactivationTokenExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_verified")
    private boolean isVerified = false;

    @Column(name = "face_descriptor", columnDefinition = "TEXT")
    private String faceDescriptor;

    @Column(name = "face_auth_enabled")
    private boolean faceAuthEnabled = false;

    @Column(name = "face_registered_at")
    private LocalDateTime faceRegisteredAt;

    @Column(name = "face_auth_failed_attempts")
    private int faceAuthFailedAttempts = 0;

    @Column(name = "last_face_auth_attempt_at")
    private LocalDateTime lastFaceAuthAttemptAt;

    @Column(name = "totp_secret", length = 255)
    private String totpSecret;

    @Column(name = "is_two_factor_enabled")
    private boolean isTwoFactorEnabled = false;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;

    @Column(name = "two_factor_enabled_at")
    private LocalDateTime twoFactorEnabledAt;

    @OneToOne(mappedBy = "utilisateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProfilPsychologique profilPsychologique;

    public String getRoleValue() {
        if (role == null || role.isBlank()) {
            return "user";
        }
        return role.toLowerCase(Locale.ROOT);
    }

    /**
     * Copie l'état métier vers une autre instance de sous-classe (transition de rôle / discriminator).
     * Ne copie pas {@code id} ni {@code profilPsychologique} (évite doublons d'association ; le FK profil reste sur la même ligne utilisateur).
     */
    public void copyPersistableStateTo(Utilisateur target) {
        target.setNom(nom);
        target.setPrenom(prenom);
        target.setEmail(email);
        target.setMotDePasse(motDePasse);
        target.setTelephone(telephone);
        target.setDateNaissance(dateNaissance);
        target.setAvatar(avatar);
        target.setStatut(statut);
        target.setScoreInitial(scoreInitial);
        target.setDateEvaluationInitiale(dateEvaluationInitiale);
        target.setCreatedAt(createdAt);
        target.setUpdatedAt(updatedAt);
        target.setResetToken(resetToken);
        target.setResetTokenExpiresAt(resetTokenExpiresAt);
        target.setDeactivatedAt(deactivatedAt);
        target.setDeactivatedBy(deactivatedBy);
        target.setReactivationToken(reactivationToken);
        target.setReactivationTokenExpiresAt(reactivationTokenExpiresAt);
        target.setLastLoginAt(lastLoginAt);
        target.setVerified(isVerified);
        target.setFaceDescriptor(faceDescriptor);
        target.setFaceAuthEnabled(faceAuthEnabled);
        target.setFaceRegisteredAt(faceRegisteredAt);
        target.setFaceAuthFailedAttempts(faceAuthFailedAttempts);
        target.setLastFaceAuthAttemptAt(lastFaceAuthAttemptAt);
        target.setTotpSecret(totpSecret);
        target.setTwoFactorEnabled(isTwoFactorEnabled);
        target.setBackupCodes(backupCodes);
        target.setTwoFactorEnabledAt(twoFactorEnabledAt);
    }

    @PrePersist
    protected void onPersist() {
        createdAt = LocalDateTime.now();
        if (statut == null) {
            statut = UtilisateurStatut.ACTIF;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return statut == UtilisateurStatut.ACTIF;
    }

    @Override
    public boolean isAccountNonLocked() {
        return statut != UtilisateurStatut.BLOQUE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String r = getRoleValue();
        return switch (r) {
            case "admin" -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "coach" -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_COACH"));
            case "user" -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
            default -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
        };
    }
}
