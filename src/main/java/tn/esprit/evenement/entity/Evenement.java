package tn.esprit.evenement.entity;

import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private LocalDateTime date_debut;
    private LocalDateTime date_fin;
    private int capacite_max;
    private String type;
    private String statut;
    private String lien_session;
    private LocalDateTime created_at;
    private int coach_id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDate_debut() { return date_debut; }
    public void setDate_debut(LocalDateTime date_debut) { this.date_debut = date_debut; }
    public LocalDateTime getDate_fin() { return date_fin; }
    public void setDate_fin(LocalDateTime date_fin) { this.date_fin = date_fin; }
    public int getCapacite_max() { return capacite_max; }
    public void setCapacite_max(int capacite_max) { this.capacite_max = capacite_max; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getLien_session() { return lien_session; }
    public void setLien_session(String lien_session) { this.lien_session = lien_session; }
    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }
    public int getCoach_id() { return coach_id; }
    public void setCoach_id(int coach_id) { this.coach_id = coach_id; }
}
