package com.bekri.entities;

import com.bekri.enums.UtilisateurStatut;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Utilisateur implements UserDetails {

    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String telephone;
    private LocalDate dateNaissance;
    private String avatar;

    /**
     * Colonne DB utilisée pour le rôle (user / coach / admin).
     */
    private String role;

    private UtilisateurStatut statut = UtilisateurStatut.ACTIF;

    private Integer scoreInitial;
    private LocalDate dateEvaluationInitiale;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String resetToken;
    private LocalDateTime resetTokenExpiresAt;
    private LocalDateTime deactivatedAt;
    private String deactivatedBy;
    private String reactivationToken;
    private LocalDateTime reactivationTokenExpiresAt;
    private LocalDateTime lastLoginAt;
    private boolean isVerified = false;
    private String faceDescriptor;
    private boolean faceAuthEnabled = false;
    private LocalDateTime faceRegisteredAt;
    private int faceAuthFailedAttempts = 0;
    private LocalDateTime lastFaceAuthAttemptAt;
    private String totpSecret;
    private boolean isTwoFactorEnabled = false;
    private String backupCodes;
    private LocalDateTime twoFactorEnabledAt;

    public String getRoleValue() {
        if (role == null || role.isBlank()) {
            return "user";
        }
        return role.toLowerCase(Locale.ROOT);
    }

    /**
     * Copie l'état métier vers une autre instance de sous-classe (transition de rôle).
     * Ne copie pas {@code id}.
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
