package tn.esprit.models;

import java.time.LocalDateTime;

public class ParticipationDisplay {
    private int participationId;
    private String nomParticipant;
    private String emailParticipant;
    private LocalDateTime dateInscription;
    private String statut;
    private String nomEvenement;
    private String lieuEvenement;
    
    public ParticipationDisplay(int participationId, String nomParticipant, String emailParticipant,
                                LocalDateTime dateInscription, String statut, String nomEvenement, String lieuEvenement) {
        this.participationId = participationId;
        this.nomParticipant = nomParticipant;
        this.emailParticipant = emailParticipant;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.nomEvenement = nomEvenement;
        this.lieuEvenement = lieuEvenement;
    }
    
    // Getters et Setters
    public int getParticipationId() { return participationId; }
    public void setParticipationId(int participationId) { this.participationId = participationId; }
    
    public String getNomParticipant() { return nomParticipant; }
    public void setNomParticipant(String nomParticipant) { this.nomParticipant = nomParticipant; }
    
    public String getEmailParticipant() { return emailParticipant; }
    public void setEmailParticipant(String emailParticipant) { this.emailParticipant = emailParticipant; }
    
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public String getNomEvenement() { return nomEvenement; }
    public void setNomEvenement(String nomEvenement) { this.nomEvenement = nomEvenement; }
    
    public String getLieuEvenement() { return lieuEvenement; }
    public void setLieuEvenement(String lieuEvenement) { this.lieuEvenement = lieuEvenement; }
}
