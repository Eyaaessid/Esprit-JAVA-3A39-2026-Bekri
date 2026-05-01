package tn.esprit.models;

import java.time.LocalDateTime;

public class ParticipationEvenement {
    private int id;
    private LocalDateTime date_inscription;
    private String statut;
    private String commentaire;
    private int utilisateur_id;
    private int evenement_id;

    public ParticipationEvenement() {
    }

    public ParticipationEvenement(LocalDateTime date_inscription, String statut, 
                                  String commentaire, int utilisateur_id, int evenement_id) {
        this.date_inscription = date_inscription;
        this.statut = statut;
        this.commentaire = commentaire;
        this.utilisateur_id = utilisateur_id;
        this.evenement_id = evenement_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDate_inscription() {
        return date_inscription;
    }

    public void setDate_inscription(LocalDateTime date_inscription) {
        this.date_inscription = date_inscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public int getUtilisateur_id() {
        return utilisateur_id;
    }

    public void setUtilisateur_id(int utilisateur_id) {
        this.utilisateur_id = utilisateur_id;
    }

    public int getEvenement_id() {
        return evenement_id;
    }

    public void setEvenement_id(int evenement_id) {
        this.evenement_id = evenement_id;
    }

    @Override
    public String toString() {
        return "ParticipationEvenement{" +
                "id=" + id +
                ", date_inscription=" + date_inscription +
                ", statut='" + statut + '\'' +
                ", commentaire='" + commentaire + '\'' +
                ", utilisateur_id=" + utilisateur_id +
                ", evenement_id=" + evenement_id +
                '}';
    }
}
