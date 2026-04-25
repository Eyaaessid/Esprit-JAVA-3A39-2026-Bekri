package tn.esprit.suivi.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SuiviQuotidien {
    private int id;
    private LocalDate date;
    private String commentaire;
    private int utilisateurId;
    private LocalDateTime soumisAt;
    private List<ReponseSuivi> reponses = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public LocalDateTime getSoumisAt() {
        return soumisAt;
    }

    public void setSoumisAt(LocalDateTime soumisAt) {
        this.soumisAt = soumisAt;
    }

    public List<ReponseSuivi> getReponses() {
        return reponses;
    }

    public void setReponses(List<ReponseSuivi> reponses) {
        this.reponses = reponses;
    }
}
