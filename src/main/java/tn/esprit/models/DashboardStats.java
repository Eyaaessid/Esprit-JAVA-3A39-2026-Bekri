package tn.esprit.models;

public class DashboardStats {
    private int totalEvenements;
    private int evenementsOuverts;
    private int evenementsFermes;
    private int evenementsPlanifies;
    private int totalParticipations;
    private String evenementPopulaire;
    private String typePopulaire;
    private double tauxRemplissageMoyen;
    private String meilleurTauxRemplissage;
    
    public DashboardStats() {}
    
    // Getters et Setters
    public int getTotalEvenements() { return totalEvenements; }
    public void setTotalEvenements(int totalEvenements) { this.totalEvenements = totalEvenements; }
    
    public int getEvenementsOuverts() { return evenementsOuverts; }
    public void setEvenementsOuverts(int evenementsOuverts) { this.evenementsOuverts = evenementsOuverts; }
    
    public int getEvenementsFermes() { return evenementsFermes; }
    public void setEvenementsFermes(int evenementsFermes) { this.evenementsFermes = evenementsFermes; }
    
    public int getEvenementsPlanifies() { return evenementsPlanifies; }
    public void setEvenementsPlanifies(int evenementsPlanifies) { this.evenementsPlanifies = evenementsPlanifies; }
    
    public int getTotalParticipations() { return totalParticipations; }
    public void setTotalParticipations(int totalParticipations) { this.totalParticipations = totalParticipations; }
    
    public String getEvenementPopulaire() { return evenementPopulaire; }
    public void setEvenementPopulaire(String evenementPopulaire) { this.evenementPopulaire = evenementPopulaire; }
    
    public String getTypePopulaire() { return typePopulaire; }
    public void setTypePopulaire(String typePopulaire) { this.typePopulaire = typePopulaire; }
    
    public double getTauxRemplissageMoyen() { return tauxRemplissageMoyen; }
    public void setTauxRemplissageMoyen(double tauxRemplissageMoyen) { this.tauxRemplissageMoyen = tauxRemplissageMoyen; }

    public String getMeilleurTauxRemplissage() { return meilleurTauxRemplissage; }
    public void setMeilleurTauxRemplissage(String meilleurTauxRemplissage) { this.meilleurTauxRemplissage = meilleurTauxRemplissage; }
}
