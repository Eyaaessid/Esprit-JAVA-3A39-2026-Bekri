package tn.esprit.services;

import tn.esprit.models.Evenement;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {
    private Connection connection;
    
    public EvenementService() {
        connection = MyConnection.getInstance().getConnection();
    }
    
    public void ajouterEvenement(Evenement evenement) throws SQLException {
        String query = "INSERT INTO evenement (nom, description, date_debut, date_fin, lieu, capacite) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, evenement.getNom());
            ps.setString(2, evenement.getDescription());
            ps.setDate(3, Date.valueOf(evenement.getDateDebut()));
            ps.setDate(4, Date.valueOf(evenement.getDateFin()));
            ps.setString(5, evenement.getLieu());
            ps.setInt(6, evenement.getCapacite());
            ps.executeUpdate();
        }
    }
    
    public List<Evenement> afficherEvenements() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Evenement e = new Evenement(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin").toLocalDate(),
                    rs.getString("lieu"),
                    rs.getInt("capacite")
                );
                evenements.add(e);
            }
        }
        return evenements;
    }
    
    public Evenement getEvenementById(int id) throws SQLException {
        String query = "SELECT * FROM evenement WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Evenement(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getInt("capacite")
                    );
                }
            }
        }
        return null;
    }
    
    public void modifierEvenement(Evenement evenement) throws SQLException {
        String query = "UPDATE evenement SET nom = ?, description = ?, date_debut = ?, date_fin = ?, lieu = ?, capacite = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, evenement.getNom());
            ps.setString(2, evenement.getDescription());
            ps.setDate(3, Date.valueOf(evenement.getDateDebut()));
            ps.setDate(4, Date.valueOf(evenement.getDateFin()));
            ps.setString(5, evenement.getLieu());
            ps.setInt(6, evenement.getCapacite());
            ps.setInt(7, evenement.getId());
            ps.executeUpdate();
        }
    }
    
    public void supprimerEvenement(int id) throws SQLException {
        String query = "DELETE FROM evenement WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
