package tn.esprit.services;

import tn.esprit.models.ParticipationDisplay;
import tn.esprit.models.ParticipationEvenement;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private Connection connection;
    
    public ParticipationService() {
        connection = MyConnection.getInstance().getConnection();
    }
    
    public void ajouterParticipation(ParticipationEvenement participation) throws SQLException {
        String query = "INSERT INTO participation_evenement (evenement_id, nom_participant, email_participant, date_inscription, statut) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, participation.getEvenementId());
            ps.setString(2, participation.getNomParticipant());
            ps.setString(3, participation.getEmailParticipant());
            ps.setTimestamp(4, Timestamp.valueOf(participation.getDateInscription()));
            ps.setString(5, participation.getStatut());
            ps.executeUpdate();
        }
    }
    
    public List<ParticipationDisplay> afficherParticipations() throws SQLException {
        List<ParticipationDisplay> participations = new ArrayList<>();
        String query = "SELECT p.id, p.nom_participant, p.email_participant, p.date_inscription, p.statut, " +
                      "e.nom as nom_evenement, e.lieu as lieu_evenement " +
                      "FROM participation_evenement p " +
                      "JOIN evenement e ON p.evenement_id = e.id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                ParticipationDisplay pd = new ParticipationDisplay(
                    rs.getInt("id"),
                    rs.getString("nom_participant"),
                    rs.getString("email_participant"),
                    rs.getTimestamp("date_inscription").toLocalDateTime(),
                    rs.getString("statut"),
                    rs.getString("nom_evenement"),
                    rs.getString("lieu_evenement")
                );
                participations.add(pd);
            }
        }
        return participations;
    }
    
    public ParticipationEvenement getParticipationById(int id) throws SQLException {
        String query = "SELECT * FROM participation_evenement WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ParticipationEvenement(
                        rs.getInt("id"),
                        rs.getInt("evenement_id"),
                        rs.getString("nom_participant"),
                        rs.getString("email_participant"),
                        rs.getTimestamp("date_inscription").toLocalDateTime(),
                        rs.getString("statut")
                    );
                }
            }
        }
        return null;
    }
    
    public void modifierParticipation(ParticipationEvenement participation) throws SQLException {
        String query = "UPDATE participation_evenement SET evenement_id = ?, nom_participant = ?, email_participant = ?, statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, participation.getEvenementId());
            ps.setString(2, participation.getNomParticipant());
            ps.setString(3, participation.getEmailParticipant());
            ps.setString(4, participation.getStatut());
            ps.setInt(5, participation.getId());
            ps.executeUpdate();
        }
    }
    
    public void supprimerParticipation(int id) throws SQLException {
        String query = "DELETE FROM participation_evenement WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
