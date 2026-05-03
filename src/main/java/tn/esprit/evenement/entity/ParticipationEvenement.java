package tn.esprit.evenement.entity;

import java.time.LocalDateTime;

public class ParticipationEvenement {
    private int id;
    private LocalDateTime date_inscription;
    private String statut;
    private int utilisateur_id;
    private int evenement_id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getDate_inscription() { return date_inscription; }
    public void setDate_inscription(LocalDateTime date_inscription) { this.date_inscription = date_inscription; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getUtilisateur_id() { return utilisateur_id; }
    public void setUtilisateur_id(int utilisateur_id) { this.utilisateur_id = utilisateur_id; }
    public int getEvenement_id() { return evenement_id; }
    public void setEvenement_id(int evenement_id) { this.evenement_id = evenement_id; }
}
