package tn.esprit.user.entity;

import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Coach extends Utilisateur {
    private String specialite;
    private String bio;

    public Coach() {}

    public Coach(Integer id, String nom, String prenom, String email, String motDePasse,
                 UtilisateurRole role, UtilisateurStatut statut, String photoProfil,
                 LocalDateTime createdAt, LocalDateTime updatedAt, String telephone,
                 LocalDate dateNaissance, String specialite, String bio) {
        super(id, nom, prenom, email, motDePasse, role, statut, photoProfil, createdAt, updatedAt,
                telephone, dateNaissance);
        this.specialite = specialite;
        this.bio = bio;
    }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
