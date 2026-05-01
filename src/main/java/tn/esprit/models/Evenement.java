package tn.esprit.models;

import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private LocalDateTime date_debut;
    private LocalDateTime date_fin;
    private String lieu;
    private int capacite_max;
    private String type;
    private String statut;
    private String image;
    private LocalDateTime created_at;
    private int coach_id;

    public Evenement() {
    }

    public Evenement(String titre, String description, LocalDateTime date_debut, 
                     LocalDateTime date_fin, String lieu, int capacite_max, String type, 
                     String statut, String image, LocalDateTime created_at, int coach_id) {
        this.titre = titre;
        this.description = description;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.lieu = lieu;
        this.capacite_max = capacite_max;
        this.type = type;
        this.statut = statut;
        this.image = image;
        this.created_at = created_at;
        this.coach_id = coach_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDate_debut() {
        return date_debut;
    }

    public void setDate_debut(LocalDateTime date_debut) {
        this.date_debut = date_debut;
    }

    public LocalDateTime getDate_fin() {
        return date_fin;
    }

    public void setDate_fin(LocalDateTime date_fin) {
        this.date_fin = date_fin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getCapacite_max() {
        return capacite_max;
    }

    public void setCapacite_max(int capacite_max) {
        this.capacite_max = capacite_max;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public int getCoach_id() {
        return coach_id;
    }

    public void setCoach_id(int coach_id) {
        this.coach_id = coach_id;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", date_debut=" + date_debut +
                ", date_fin=" + date_fin +
                ", lieu='" + lieu + '\'' +
                ", capacite_max=" + capacite_max +
                ", type='" + type + '\'' +
                ", statut='" + statut + '\'' +
                ", image='" + image + '\'' +
                ", created_at=" + created_at +
                ", coach_id=" + coach_id +
                '}';
    }
}
