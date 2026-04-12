package com.bekri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Correspond à UtilisateurResponseDTO côté API Spring Boot.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UtilisateurResponse {

    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String dateNaissance; // String pour simplifier désérialisation
    private String role;          // "user" | "coach" | "admin"
    private String statut;        // "actif" | "bloque" | "inactif" | "supprime"
    private String createdAt;
    private String avatar;

    // Constructeur vide requis par Jackson
    public UtilisateurResponse() {}

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
