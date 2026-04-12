package tn.esprit.pijava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "objectif_bien_etre")
public class ObjectifBienEtre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(min = 3, max = 255, message = "Le titre doit contenir entre 3 et 255 caractères.")
    @Column(nullable = false, length = 255)
    private String titre;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @NotBlank(message = "Le type est obligatoire.")
    @Column(nullable = false, length = 100)
    private String type;

    @NotNull(message = "La valeur cible est obligatoire.")
    @Positive(message = "La valeur cible doit être un nombre positif.")
    @Column(name = "valeur_cible", nullable = false)
    private Double valeurCible;

    @Column(name = "valeur_actuelle")
    private Double valeurActuelle;

    @NotNull(message = "La date de début est obligatoire.")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire.")
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @NotBlank(message = "Le statut est obligatoire.")
    @Column(nullable = false, length = 50)
    private String statut;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "utilisateur_id", nullable = false)
    private Integer utilisateurId;

    @Column(length = 255)
    private String slug;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (utilisateurId == null) utilisateurId = 1;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters/Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getValeurCible() { return valeurCible; }
    public void setValeurCible(Double valeurCible) { this.valeurCible = valeurCible; }

    public Double getValeurActuelle() { return valeurActuelle; }
    public void setValeurActuelle(Double valeurActuelle) { this.valeurActuelle = valeurActuelle; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Integer utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}