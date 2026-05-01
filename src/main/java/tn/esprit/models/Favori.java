package tn.esprit.models;

import java.time.LocalDateTime;

public class Favori {
    private int id;
    private int userId;
    private int evenementId;
    private LocalDateTime dateAjout;
    
    public Favori() {}
    
    public Favori(int userId, int evenementId) {
        this.userId = userId;
        this.evenementId = evenementId;
        this.dateAjout = LocalDateTime.now();
    }
    
    public Favori(int id, int userId, int evenementId, LocalDateTime dateAjout) {
        this.id = id;
        this.userId = userId;
        this.evenementId = evenementId;
        this.dateAjout = dateAjout;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }
    
    public LocalDateTime getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDateTime dateAjout) { this.dateAjout = dateAjout; }
}
