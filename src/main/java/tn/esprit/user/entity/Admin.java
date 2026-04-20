package tn.esprit.user.entity;

import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Admin extends Utilisateur {
    private String department;

    public Admin() {}

    public Admin(Integer id, String nom, String prenom, String email, String motDePasse,
                 UtilisateurRole role, UtilisateurStatut statut, String photoProfil,
                 LocalDateTime createdAt, LocalDateTime updatedAt, String telephone,
                 LocalDate dateNaissance, String department) {
        super(id, nom, prenom, email, motDePasse, role, statut, photoProfil, createdAt, updatedAt,
                telephone, dateNaissance);
        this.department = department;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
