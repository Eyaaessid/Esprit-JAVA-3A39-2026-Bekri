package tn.esprit.testmental.model;

public class Question {

    private int id;
    private String contenu;
    private String choixA;
    private String choixB;
    private String choixC;
    private String bonneReponse;
    private int testMentalId; //objet de test mental

    public Question() {}

    public Question(String contenu, String choixA, String choixB,
                    String choixC, String bonneReponse, int testMentalId) {
        this.contenu = contenu;
        this.choixA = choixA;
        this.choixB = choixB;
        this.choixC = choixC;
        this.bonneReponse = bonneReponse;
        this.testMentalId = testMentalId;
    }

    public Question(int id, String contenu, String choixA, String choixB,
                    String choixC, String bonneReponse, int testMentalId) {
        this.id = id;
        this.contenu = contenu;
        this.choixA = choixA;
        this.choixB = choixB;
        this.choixC = choixC;
        this.bonneReponse = bonneReponse;
        this.testMentalId = testMentalId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getChoixA() { return choixA; }
    public void setChoixA(String choixA) { this.choixA = choixA; }

    public String getChoixB() { return choixB; }
    public void setChoixB(String choixB) { this.choixB = choixB; }

    public String getChoixC() { return choixC; }
    public void setChoixC(String choixC) { this.choixC = choixC; }

    public String getBonneReponse() { return bonneReponse; }
    public void setBonneReponse(String bonneReponse) { this.bonneReponse = bonneReponse; }

    public int getTestMentalId() { return testMentalId; }
    public void setTestMentalId(int testMentalId) { this.testMentalId = testMentalId; }
}