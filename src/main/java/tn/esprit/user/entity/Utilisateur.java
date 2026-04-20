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
            case BANNI -> "bloque";
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
}
