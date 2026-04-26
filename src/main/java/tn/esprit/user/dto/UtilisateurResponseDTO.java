package tn.esprit.user.dto;

import tn.esprit.user.entity.Utilisateur;

import java.time.format.DateTimeFormatter;

/**
 * View/DTO mirroring the former REST payload for JavaFX controllers.
 */
public class UtilisateurResponseDTO {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String dateNaissance;
    private String role;
    private String statut;
    private String createdAt;
    private String avatar;

    public UtilisateurResponseDTO() {}

    public static UtilisateurResponseDTO fromEntity(Utilisateur u) {
        if (u == null) return null;
        UtilisateurResponseDTO d = new UtilisateurResponseDTO();
        d.setId(u.getId());
        d.setNom(u.getNom());
        d.setPrenom(u.getPrenom());
        d.setEmail(u.getEmail());
        d.setTelephone(u.getTelephone());
        if (u.getDateNaissance() != null) {
            d.setDateNaissance(u.getDateNaissance().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        d.setRole(u.getRoleKey());
        d.setStatut(u.getStatutKey());
        if (u.getCreatedAt() != null) {
            d.setCreatedAt(u.getCreatedAt().toString());
        }
        d.setAvatar(u.getPhotoProfil());
        return d;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(String dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getFullName() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    public String getInitials() {
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        String n = (nom != null && !nom.isEmpty()) ? String.valueOf(nom.charAt(0)).toUpperCase() : "";
        return p + n;
    }
}
