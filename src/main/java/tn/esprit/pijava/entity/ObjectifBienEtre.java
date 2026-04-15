package tn.esprit.pijava.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ObjectifBienEtre {

    private Integer id;
    private String titre;

    private String description;
    private String type;
    private Double valeurCible;

    private Double valeurActuelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer utilisateurId;

    private String slug;

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
