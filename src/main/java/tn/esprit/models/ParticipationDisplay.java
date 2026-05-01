package tn.esprit.models;

import java.time.LocalDateTime;

/**
 * DTO pour afficher les participations enrichies avec les informations de l'événement
 * Utilisé uniquement pour l'affichage dans le TableView
 */
public class ParticipationDisplay {
    private int participationId;
    private String evenementTitre;
    private String evenementType;
    private String evenementLieu;
    private LocalDateTime evenementDateDebut;
    private int evenementCapaciteMax;
    private String participationStatut;
    private LocalDateTime dateInscription;
    
    // Référence à l'objet participation original pour les opérations CRUD
    private ParticipationEvenement participationOriginal;
    
    public ParticipationDisplay() {
    }
    
    public ParticipationDisplay(int participationId, String evenementTitre, String evenementType,
                               String evenementLieu, LocalDateTime evenementDateDebut, 
                               int evenementCapaciteMax, String participationStatut, 
                               LocalDateTime dateInscription, ParticipationEvenement participationOriginal) {
        this.participationId = participationId;
        this.evenementTitre = evenementTitre;
        this.evenementType = evenementType;
        this.evenementLieu = evenementLieu;
        this.evenementDateDebut = evenementDateDebut;
        this.evenementCapaciteMax = evenementCapaciteMax;
        this.participationStatut = participationStatut;
        this.dateInscription = dateInscription;
        this.participationOriginal = participationOriginal;
    }
    
    public int getParticipationId() {
        return participationId;
    }
    
    public void setParticipationId(int participationId) {
        this.participationId = participationId;
    }
    
    public String getEvenementTitre() {
        return evenementTitre;
    }
    
    public void setEvenementTitre(String evenementTitre) {
        this.evenementTitre = evenementTitre;
    }
    
    public String getEvenementType() {
        return evenementType;
    }
    
    public void setEvenementType(String evenementType) {
        this.evenementType = evenementType;
    }
    
    public String getEvenementLieu() {
        return evenementLieu;
    }
    
    public void setEvenementLieu(String evenementLieu) {
        this.evenementLieu = evenementLieu;
    }
    
    public LocalDateTime getEvenementDateDebut() {
        return evenementDateDebut;
    }
    
    public void setEvenementDateDebut(LocalDateTime evenementDateDebut) {
        this.evenementDateDebut = evenementDateDebut;
    }
    
    public int getEvenementCapaciteMax() {
        return evenementCapaciteMax;
    }
    
    public void setEvenementCapaciteMax(int evenementCapaciteMax) {
        this.evenementCapaciteMax = evenementCapaciteMax;
    }
    
    public String getParticipationStatut() {
        return participationStatut;
    }
    
    public void setParticipationStatut(String participationStatut) {
        this.participationStatut = participationStatut;
    }
    
    public LocalDateTime getDateInscription() {
        return dateInscription;
    }
    
    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }
    
    public ParticipationEvenement getParticipationOriginal() {
        return participationOriginal;
    }
    
    public void setParticipationOriginal(ParticipationEvenement participationOriginal) {
        this.participationOriginal = participationOriginal;
    }
    
    @Override
    public String toString() {
        return "ParticipationDisplay{" +
                "participationId=" + participationId +
                ", evenementTitre='" + evenementTitre + '\'' +
                ", evenementType='" + evenementType + '\'' +
                ", evenementLieu='" + evenementLieu + '\'' +
                ", evenementDateDebut=" + evenementDateDebut +
                ", evenementCapaciteMax=" + evenementCapaciteMax +
                ", participationStatut='" + participationStatut + '\'' +
                ", dateInscription=" + dateInscription +
                '}';
    }
}
