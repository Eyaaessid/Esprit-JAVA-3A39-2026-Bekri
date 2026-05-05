package tn.esprit.testmental.model;

public class TestMental {
    private int id;
    private String titre;
    private String description;
    private String niveau;
    private int duree;
    private String typeTest;

    public TestMental() {}

    public TestMental(int id, String titre, String description, String niveau, int duree, String typeTest) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.niveau = niveau;
        this.duree = duree;
        this.typeTest = typeTest;
    }

    public TestMental(String titre, String description, String niveau, int duree, String typeTest) {
        this.titre = titre;
        this.description = description;
        this.niveau = niveau;
        this.duree = duree;
        this.typeTest = typeTest;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
    public String getTypeTest() { return typeTest; }
    public void setTypeTest(String typeTest) { this.typeTest = typeTest; }
}