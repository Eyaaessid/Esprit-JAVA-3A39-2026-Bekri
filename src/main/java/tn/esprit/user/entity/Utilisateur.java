package tn.esprit.user.entity;

import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Utilisateur {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private UtilisateurRole role;
    private UtilisateurStatut statut;
    private String photoProfil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String telephone;
    private LocalDate dateNaissance;

    // Reset password + email verification.
    // Convention (shared DB with Symfony): `reset_token` is dual-purpose.
    // - If `is_verified = 0`, it is used as a verification token.
    // - If `is_verified = 1`, it is used as a password reset token.
    // Token is always cleared after use.
    private String resetToken;                  // column: reset_token
    private LocalDateTime resetTokenExpiresAt;  // column: reset_token_expires_at
    private boolean isVerified;                 // column: is_verified
    private LocalDateTime lastLoginAt;          // column: last_login_at
    private LocalDateTime deactivatedAt;        // column: deactivated_at
    private String deactivatedBy;               // column: deactivated_by
    private String reactivationToken;           // column: reactivation_token
    private LocalDateTime reactivationTokenExpiresAt; // column: reactivation_token_expires_at

    // Face authentication (shared with Symfony / face-api.js)
    private String faceDescriptor;              // column: face_descriptor (JSON string)
    private boolean faceAuthEnabled;            // column: face_auth_enabled
    private LocalDateTime faceRegisteredAt;     // column: face_registered_at
    private int faceAuthFailedAttempts;         // column: face_auth_failed_attempts
    private LocalDateTime lastFaceAuthAttemptAt;// column: last_face_auth_attempt_at

    // Two-Factor Authentication
    private String totpSecret;                  // column: totp_secret
    private boolean isTwoFactorEnabled;         // column: is_two_factor_enabled
    private String backupCodes;                 // column: backup_codes (JSON string)
    private LocalDateTime twoFactorEnabledAt;   // column: two_factor_enabled_at

    public Utilisateur() {}

    public Utilisateur(Integer id, String nom, String prenom, String email, String motDePasse,
                       UtilisateurRole role, UtilisateurStatut statut, String photoProfil,
                       LocalDateTime createdAt, LocalDateTime updatedAt, String telephone,
                       LocalDate dateNaissance) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.statut = statut;
        this.photoProfil = photoProfil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.telephone = telephone;
        this.dateNaissance = dateNaissance;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public UtilisateurRole getRole() { return role; }
    public void setRole(UtilisateurRole role) { this.role = role; }

    public UtilisateurStatut getStatut() { return statut; }
    public void setStatut(UtilisateurStatut statut) { this.statut = statut; }

    public String getPhotoProfil() { return photoProfil; }
    public void setPhotoProfil(String photoProfil) { this.photoProfil = photoProfil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(LocalDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }

    public String getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(String deactivatedBy) { this.deactivatedBy = deactivatedBy; }

    public String getReactivationToken() { return reactivationToken; }
    public void setReactivationToken(String reactivationToken) { this.reactivationToken = reactivationToken; }

    public LocalDateTime getReactivationTokenExpiresAt() { return reactivationTokenExpiresAt; }
    public void setReactivationTokenExpiresAt(LocalDateTime reactivationTokenExpiresAt) { this.reactivationTokenExpiresAt = reactivationTokenExpiresAt; }

    public String getFaceDescriptor() { return faceDescriptor; }
    public void setFaceDescriptor(String faceDescriptor) { this.faceDescriptor = faceDescriptor; }

    public boolean isFaceAuthEnabled() { return faceAuthEnabled; }
    public void setFaceAuthEnabled(boolean faceAuthEnabled) { this.faceAuthEnabled = faceAuthEnabled; }

    public LocalDateTime getFaceRegisteredAt() { return faceRegisteredAt; }
    public void setFaceRegisteredAt(LocalDateTime faceRegisteredAt) { this.faceRegisteredAt = faceRegisteredAt; }

    public int getFaceAuthFailedAttempts() { return faceAuthFailedAttempts; }
    public void setFaceAuthFailedAttempts(int faceAuthFailedAttempts) { this.faceAuthFailedAttempts = faceAuthFailedAttempts; }

    public LocalDateTime getLastFaceAuthAttemptAt() { return lastFaceAuthAttemptAt; }
    public void setLastFaceAuthAttemptAt(LocalDateTime lastFaceAuthAttemptAt) { this.lastFaceAuthAttemptAt = lastFaceAuthAttemptAt; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public boolean isTwoFactorEnabled() { return isTwoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { isTwoFactorEnabled = twoFactorEnabled; }

    public String getBackupCodes() { return backupCodes; }
    public void setBackupCodes(String backupCodes) { this.backupCodes = backupCodes; }

    public LocalDateTime getTwoFactorEnabledAt() { return twoFactorEnabledAt; }
    public void setTwoFactorEnabledAt(LocalDateTime twoFactorEnabledAt) { this.twoFactorEnabledAt = twoFactorEnabledAt; }

    /** Clears all 2FA data (used when disabling). */
    public void resetTwoFactorAuth() {
        this.totpSecret = null;
        this.isTwoFactorEnabled = false;
        this.backupCodes = null;
        this.twoFactorEnabledAt = null;
    }

    /** Lowercase key for CSS (user, admin, coach). */
    public String getRoleKey() {
        if (role == null) return "";
        return role.name().toLowerCase();
    }

    /** Maps enum to legacy UI strings (actif, inactif, bloque). */
    public String getStatutKey() {
        if (statut == null) return "";
        return switch (statut) {
            case ACTIF -> "actif";
            case INACTIF -> "inactif";
            case BLOQUE -> "bloque";
            case SUPPRIME -> "supprime";
        };
    }

    public String getFullName() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    public String getInitials() {
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        String n = (nom != null && !nom.isEmpty()) ? String.valueOf(nom.charAt(0)).toUpperCase() : "";
        return p + n;
    }

    @Override
    public String toString() {
        return "Utilisateur{"
                + "id=" + id
                + ", nom='" + nom + '\''
                + ", prenom='" + prenom + '\''
                + ", email='" + email + '\''
                + ", role=" + role
                + ", statut=" + statut
                + ", deactivatedBy='" + deactivatedBy + '\''
                + '}';
    }
}
