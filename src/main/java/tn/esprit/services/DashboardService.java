package tn.esprit.services;

import tn.esprit.models.DashboardStats;
import tn.esprit.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardService {
    
    public DashboardStats getStatistiques() {
        DashboardStats stats = new DashboardStats();
        
        Connection conn = MyConnection.getInstance();
        
        try {
            // Total événements
            stats.setTotalEvenements(getTotalEvenements(conn));
            
            // Événements par statut
            stats.setEvenementsOuverts(getEvenementsByStatut(conn, "ouvert"));
            stats.setEvenementsFermes(getEvenementsByStatut(conn, "fermé"));
            stats.setEvenementsPlanifies(getEvenementsByStatut(conn, "planifié"));
            
            // Total participations
            stats.setTotalParticipations(getTotalParticipations(conn));
            
            // Événement populaire
            stats.setEvenementPopulaire(getEvenementPopulaire(conn));
            
            // Type populaire
            stats.setTypePopulaire(getTypePopulaire(conn));
            
            // Taux de remplissage moyen
            stats.setTauxRemplissageMoyen(getTauxRemplissageMoyen(conn));
            stats.setMeilleurTauxRemplissage(getMeilleurTauxRemplissage(conn));
            
            System.out.println("✓ Statistiques récupérées avec succès");
        } catch (SQLException e) {
            System.err.println("✗ Erreur récupération statistiques: " + e.getMessage());
        }
        
        return stats;
    }
    
    private int getTotalEvenements(Connection conn) throws SQLException {
        String query = "SELECT COUNT(*) FROM evenement";
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    private int getEvenementsByStatut(Connection conn, String statut) throws SQLException {
        String query = "SELECT COUNT(*) FROM evenement WHERE statut = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    private int getTotalParticipations(Connection conn) throws SQLException {
        String query = "SELECT COUNT(*) FROM participation_evenement";
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    private String getEvenementPopulaire(Connection conn) throws SQLException {
        String query = "SELECT e.titre, COUNT(p.id) as nb_participants " +
                      "FROM evenement e " +
                      "LEFT JOIN participation_evenement p ON e.id = p.evenement_id " +
                      "GROUP BY e.id, e.titre " +
                      "ORDER BY nb_participants DESC " +
                      "LIMIT 1";
        
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String titre = rs.getString("titre");
                int nbParticipants = rs.getInt("nb_participants");
                return titre + " (" + nbParticipants + " participants)";
            }
        }
        return "Aucun événement";
    }
    
    private String getTypePopulaire(Connection conn) throws SQLException {
        String query = "SELECT type, COUNT(*) as nb " +
                      "FROM evenement " +
                      "GROUP BY type " +
                      "ORDER BY nb DESC " +
                      "LIMIT 1";
        
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String type = rs.getString("type");
                int nb = rs.getInt("nb");
                return type + " (" + nb + " événements)";
            }
        }
        return "Aucun type";
    }
    
    private double getTauxRemplissageMoyen(Connection conn) throws SQLException {
        String query = "SELECT AVG((SELECT COUNT(*) FROM participation_evenement p WHERE p.evenement_id = e.id) * 100.0 / e.capacite_max) as taux " +
                      "FROM evenement e " +
                      "WHERE e.capacite_max > 0";
        
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Math.round(rs.getDouble("taux") * 10.0) / 10.0;
            }
        }
        return 0.0;
    }

    private String getMeilleurTauxRemplissage(Connection conn) throws SQLException {
        String query = "SELECT e.titre, " +
                "ROUND((COUNT(p.id) * 100.0 / e.capacite_max), 1) AS taux " +
                "FROM evenement e " +
                "LEFT JOIN participation_evenement p ON p.evenement_id = e.id " +
                "WHERE e.capacite_max > 0 " +
                "GROUP BY e.id, e.titre, e.capacite_max " +
                "ORDER BY taux DESC " +
                "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("titre") + " (" + rs.getDouble("taux") + "%)";
            }
        }
        return "N/A";
    }
}
