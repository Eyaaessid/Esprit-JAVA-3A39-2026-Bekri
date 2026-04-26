package tn.esprit.suivi.model;

public class ReponseSuivi {
    private int id;
    private int suiviId;
    private int questionId;
    private String valeur;
    private String category;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSuiviId() {
        return suiviId;
    }

    public void setSuiviId(int suiviId) {
        this.suiviId = suiviId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
