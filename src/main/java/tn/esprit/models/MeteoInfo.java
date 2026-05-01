package tn.esprit.models;

public class MeteoInfo {
    private String ville;
    private double temperature;
    private String description;
    private String icone;
    private String conseil;
    private boolean disponible;
    
    public MeteoInfo() {}
    
    public MeteoInfo(String ville, double temperature, String description, String icone, String conseil) {
        this.ville = ville;
        this.temperature = temperature;
        this.description = description;
        this.icone = icone;
        this.conseil = conseil;
        this.disponible = true;
    }

    public MeteoInfo(String ville, double temperature, String description, String icone, String conseil, boolean disponible) {
        this.ville = ville;
        this.temperature = temperature;
        this.description = description;
        this.icone = icone;
        this.conseil = conseil;
        this.disponible = disponible;
    }
    
    // Getters et Setters
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    
    public String getConseil() { return conseil; }
    public void setConseil(String conseil) { this.conseil = conseil; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}
