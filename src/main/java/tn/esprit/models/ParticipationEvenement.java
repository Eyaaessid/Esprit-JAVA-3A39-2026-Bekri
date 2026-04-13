package tn.esprit.models;

import java.time.LocalDateTime;

public class ParticipationEvenement {
    private int id;
    private int evenementId;
    private String nomParticipant;
    private String emailParticipant;
    private LocalDateTime dateInscription;
    private String statut;
    
    public ParticipationEvenement() {}
    
    public ParticipationEvenement(int evenementId, String nomParticipant, String emailParticipant, String statut) {
        this.evenementId = evenementId;
        this.nomParticipant = nomParticipant;
        this.emailParticipant = emailParticipant;
        this.dateInscription = LocalDateTime.now();
        this.statut = statut;
    }
    
    public ParticipationEvenement(int id, int evenementId, String nomParticipant, String emailParticipant, 
                                  LocalDateTime dateInscription, String statut) {
        this.id = id;
        this.evenementId = evenementId;
        this.nomParticipant = nomParticipant;
        this.emailParticipant = emailParticipant;
        this.dateInscription = dateInscription;
        this.statut = statut;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }
    
    public String getNomParticipant() { return nomParticipant; }
    public void setNomParticipant(String nomParticipant) { this.nomParticipant = nomParticipant; }
    
    public String getEmailParticipant() { return emailParticipant; }
    public void setEmailParticipant(String emailParticipant) { this.emailParticipant = emailParticipant; }
    
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    @Override
    public String toString() {
        return "ParticipationEvenement{" +
                "id=" + id +
                ", evenementId=" + evenementId +
                ", nomParticipant='" + nomParticipant + '\'' +
                ", emailParticipant='" + emailParticipant + '\'' +
                ", dateInscription=" + dateInscription +
                ", statut='" + statut + '\'' +
                '}';
    }
}
