package tn.esprit.evenement.entity;

public class MeteoInfo {
    private String ville;
    private double temperature;
    private String description;
    private String conseil;

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getConseil() { return conseil; }
    public void setConseil(String conseil) { this.conseil = conseil; }
}
