package tn.esprit.services;

import tn.esprit.models.Evenement;
import tn.esprit.models.Favori;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriService {
    public FavoriService() {
        ensureFavorisTable();
    }

    public void ensureFavorisTable() {
        String query = "CREATE TABLE IF NOT EXISTS favoris (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "evenement_id INT NOT NULL, " +
                "date_ajout TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT uk_favoris_user_event UNIQUE (user_id, evenement_id))";
        try (PreparedStatement ps = MyConnection.getInstance().prepareStatement(query)) {
            ps.execute();
        } catch (SQLException ex) {
            System.err.println("✗ Erreur création table favoris: " + ex.getMessage());
        }
    }

    public boolean ajouterFavori(int userId, int evenementId) {
        String query = "INSERT INTO favoris (user_id, evenement_id, date_ajout) VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, evenementId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✓ Favori ajouté avec succès");
                return true;
            }
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                System.out.println("✗ Événement déjà en favori");
                return false;
            }
            System.err.println("✗ Erreur ajout favori: " + ex.getMessage());
        } finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return false;
    }
    
    public boolean retirerFavori(int userId, int evenementId) {
        String query = "DELETE FROM favoris WHERE user_id = ? AND evenement_id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, evenementId);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✓ Favori retiré avec succès");
                return true;
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur retrait favori: " + ex.getMessage());
        } finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return false;
    }
    
    public boolean estEnFavori(int userId, int evenementId) {
        String query = "SELECT COUNT(*) FROM favoris WHERE user_id = ? AND evenement_id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, evenementId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur vérification favori: " + ex.getMessage());
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return false;
    }
    
    public List<Evenement> getMesFavoris(int userId) {
        List<Evenement> favoris = new ArrayList<>();
        String query = "SELECT e.* FROM evenement e " +
                      "INNER JOIN favoris f ON e.id = f.evenement_id " +
                      "WHERE f.user_id = ? " +
                      "ORDER BY f.date_ajout DESC";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Evenement e = mapResultSetToEvenement(rs);
                favoris.add(e);
            }
            
            System.out.println("✓ " + favoris.size() + " favoris récupérés");
        } catch (SQLException ex) {
            System.err.println("✗ Erreur récupération favoris: " + ex.getMessage());
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return favoris;
    }
    
    public List<Evenement> getEvenementsPopulaires(int limit) {
        List<Evenement> populaires = new ArrayList<>();
        String query = "SELECT e.*, COUNT(f.id) as nb_favoris FROM evenement e " +
                      "LEFT JOIN favoris f ON e.id = f.evenement_id " +
                      "WHERE LOWER(e.statut) NOT IN ('ferme','fermé','annule','annulé') " +
                      "AND e.date_debut >= NOW() " +
                      "GROUP BY e.id " +
                      "ORDER BY nb_favoris DESC, e.date_debut ASC " +
                      "LIMIT ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Evenement e = mapResultSetToEvenement(rs);
                populaires.add(e);
            }
            
            System.out.println("✓ " + populaires.size() + " événements populaires récupérés");
        } catch (SQLException ex) {
            System.err.println("✗ Erreur récupération populaires: " + ex.getMessage());
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return populaires;
    }
    
    public int getNombreFavoris(int evenementId) {
        String query = "SELECT COUNT(*) FROM favoris WHERE evenement_id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, evenementId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur comptage favoris: " + ex.getMessage());
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return 0;
    }
    
    public List<Evenement> getRecommandations(int userId, int limit) {
        List<Evenement> recommandations = new ArrayList<>();

        String query = "SELECT e.* FROM evenement e " +
                      "WHERE e.type IN (" +
                      "  SELECT DISTINCT e2.type FROM evenement e2 " +
                      "  INNER JOIN favoris f ON e2.id = f.evenement_id " +
                      "  WHERE f.user_id = ?" +
                      ") " +
                      "AND e.id NOT IN (" +
                      "  SELECT evenement_id FROM favoris WHERE user_id = ?" +
                      ") " +
                      "AND LOWER(e.statut) NOT IN ('ferme','fermé','annule','annulé') " +
                      "AND e.date_debut > NOW() " +
                      "ORDER BY e.date_debut ASC " +
                      "LIMIT ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Evenement e = mapResultSetToEvenement(rs);
                recommandations.add(e);
            }
            
            System.out.println("✓ " + recommandations.size() + " recommandations générées");
        } catch (SQLException ex) {
            System.err.println("✗ Erreur génération recommandations: " + ex.getMessage());
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return recommandations;
    }

    public Map<Integer, Integer> getFavorisCountMap() {
        Map<Integer, Integer> counts = new HashMap<>();
        String query = "SELECT evenement_id, COUNT(*) as total FROM favoris GROUP BY evenement_id";

        try (PreparedStatement ps = MyConnection.getInstance().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt("evenement_id"), rs.getInt("total"));
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur récupération compteurs favoris: " + ex.getMessage());
        }

        return counts;
    }
    
    private Evenement mapResultSetToEvenement(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setDate_debut(rs.getTimestamp("date_debut").toLocalDateTime());
        e.setDate_fin(rs.getTimestamp("date_fin").toLocalDateTime());
        e.setLieu(rs.getString("lieu"));
        e.setCapacite_max(rs.getInt("capacite_max"));
        e.setType(rs.getString("type"));
        e.setStatut(rs.getString("statut"));
        e.setImage(rs.getString("image"));
        e.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
        e.setCoach_id(rs.getInt("coach_id"));
        return e;
    }
}
