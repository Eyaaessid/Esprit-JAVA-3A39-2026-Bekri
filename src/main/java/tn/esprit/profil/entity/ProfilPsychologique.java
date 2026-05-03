package tn.esprit.profil.entity;

import java.time.LocalDateTime;

public class ProfilPsychologique {
    private Integer id;
    private Integer utilisateurId;
    private Integer scoreGlobal;
    private String profilType;
    private LocalDateTime dateEvaluation;
    private String aiFeedback;

    public ProfilPsychologique() {}

    public ProfilPsychologique(Integer id, Integer utilisateurId, Integer scoreGlobal, String profilType,
                               LocalDateTime dateEvaluation, String aiFeedback) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.scoreGlobal = scoreGlobal;
        this.profilType = profilType;
        this.dateEvaluation = dateEvaluation;
        this.aiFeedback = aiFeedback;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Integer utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public Integer getScoreGlobal() {
        return scoreGlobal;
    }

    public void setScoreGlobal(Integer scoreGlobal) {
        this.scoreGlobal = scoreGlobal;
    }

    public String getProfilType() {
        return profilType;
    }

    public void setProfilType(String profilType) {
        this.profilType = profilType;
    }

    public LocalDateTime getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDateTime dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }
}
