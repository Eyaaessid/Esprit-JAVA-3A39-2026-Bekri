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
