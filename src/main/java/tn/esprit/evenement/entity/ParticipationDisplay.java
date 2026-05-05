package tn.esprit.evenement.entity;

import java.time.LocalDateTime;

public class ParticipationDisplay {
    private int participationId;
    private String statutParticipation;
    private LocalDateTime dateInscription;
    private Evenement evenement;

    public int getParticipationId() { return participationId; }
    public void setParticipationId(int participationId) { this.participationId = participationId; }
    public String getStatutParticipation() { return statutParticipation; }
    public void setStatutParticipation(String statutParticipation) { this.statutParticipation = statutParticipation; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    public Evenement getEvenement() { return evenement; }
    public void setEvenement(Evenement evenement) { this.evenement = evenement; }
}
