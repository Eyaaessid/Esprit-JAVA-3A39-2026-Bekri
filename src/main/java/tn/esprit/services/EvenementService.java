package tn.esprit.services;

import tn.esprit.models.Evenement;
import tn.esprit.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;

public class EvenementService {

    public void ajouter(Evenement e) {
        String query = "INSERT INTO evenement (titre, description, date_debut, date_fin, lieu, " +
                "capacite_max, type, statut, image, created_at, coach_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDate_debut()));
            ps.setTimestamp(4, Timestamp.valueOf(e.getDate_fin()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getCapacite_max());
            ps.setString(7, e.getType());
            ps.setString(8, e.getStatut());
            ps.setString(9, e.getImage());
            ps.setTimestamp(10, Timestamp.valueOf(e.getCreated_at()));
            ps.setInt(11, e.getCoach_id());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    e.setId(rs.getInt(1));
                }
                System.out.println("✓ Événement ajouté avec succès ! ID: " + e.getId());
            } else {
                System.err.println("✗ Aucune ligne insérée");
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de l'ajout de l'événement : " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout de l'événement", ex);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public void modifier(Evenement e) {
        System.out.println("DEBUG modifier id=" + e.getId());

        // Gérer les valeurs null
        if (e.getImage() == null) e.setImage("");
        if (e.getLieu() == null) e.setLieu("");
        if (e.getDescription() == null) e.setDescription("");
        if (e.getTitre() == null) e.setTitre("");

        // ✅ coach_id RETIRÉ du UPDATE pour éviter la violation de clé étrangère
        String query = "UPDATE evenement SET titre = ?, description = ?, date_debut = ?, " +
                "date_fin = ?, lieu = ?, capacite_max = ?, type = ?, statut = ? " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);

            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDate_debut()));
            ps.setTimestamp(4, Timestamp.valueOf(e.getDate_fin()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getCapacite_max());
            ps.setString(7, e.getType());
            ps.setString(8, e.getStatut());
            ps.setInt(9, e.getId());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Événement modifié avec succès !");
            } else {
                System.err.println("✗ Aucun événement trouvé avec l'ID : " + e.getId());
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de la modification : " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors de la modification de l'événement", ex);
        } finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public void supprimer(int id) {
        String query = "DELETE FROM evenement WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            ps.setInt(1, id);

            System.out.println("DEBUG: Tentative de suppression de l'événement ID: " + id);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Événement supprimé avec succès ! Lignes affectées: " + rowsAffected);
            } else {
                System.err.println("✗ Aucun événement trouvé avec l'ID : " + id);
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de la suppression : " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de l'événement", ex);
        } finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<Evenement> afficherAll() {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement ORDER BY date_debut DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getInstance();
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs.next()) {
                Evenement evenement = new Evenement();

                evenement.setId(rs.getInt("id"));
                evenement.setTitre(rs.getString("titre") != null ? rs.getString("titre") : "");
                evenement.setDescription(rs.getString("description") != null ? rs.getString("description") : "");

                evenement.setDate_debut(rs.getTimestamp("date_debut") != null
                        ? rs.getTimestamp("date_debut").toLocalDateTime()
                        : LocalDateTime.now());
                evenement.setDate_fin(rs.getTimestamp("date_fin") != null
                        ? rs.getTimestamp("date_fin").toLocalDateTime()
                        : LocalDateTime.now());

                evenement.setLieu(rs.getString("lieu") != null ? rs.getString("lieu") : "");
                evenement.setCapacite_max(rs.getInt("capacite_max"));
                evenement.setType(rs.getString("type") != null ? rs.getString("type") : "");
                evenement.setStatut(rs.getString("statut") != null ? rs.getString("statut") : "");
                evenement.setImage(rs.getString("image") != null ? rs.getString("image") : "");

                evenement.setCreated_at(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                        : LocalDateTime.now());

                evenement.setCoach_id(rs.getInt("coach_id"));

                evenements.add(evenement);
            }

            System.out.println("✓ " + evenements.size() + " événement(s) récupéré(s) depuis la base");
        } catch (SQLException ex) {
            System.err.println("✗ Erreur lors de la récupération : " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }

        return evenements;
    }

    public List<Evenement> getEvenementsParMois(YearMonth yearMonth) {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement WHERE date_debut >= ? AND date_debut < ? ORDER BY date_debut ASC";

        if (yearMonth == null) {
            return evenements;
        }

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.plusMonths(1).atDay(1);

        try (PreparedStatement ps = MyConnection.getInstance().prepareStatement(query)) {
            ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(end.atStartOfDay()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evenements.add(mapResultSetToEvenement(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur récupération événements du mois: " + ex.getMessage());
        }

        return evenements;
    }

    public List<Evenement> getEvenementsParDate(LocalDate date) {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement WHERE date(date_debut) = ? ORDER BY date_debut ASC";

        if (date == null) {
            return evenements;
        }

        try (PreparedStatement ps = MyConnection.getInstance().prepareStatement(query)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evenements.add(mapResultSetToEvenement(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("✗ Erreur récupération événements du jour: " + ex.getMessage());
        }

        return evenements;
    }

    private Evenement mapResultSetToEvenement(ResultSet rs) throws SQLException {
        Evenement evenement = new Evenement();
        evenement.setId(rs.getInt("id"));
        evenement.setTitre(rs.getString("titre") != null ? rs.getString("titre") : "");
        evenement.setDescription(rs.getString("description") != null ? rs.getString("description") : "");
        evenement.setDate_debut(rs.getTimestamp("date_debut") != null
                ? rs.getTimestamp("date_debut").toLocalDateTime()
                : LocalDateTime.now());
        evenement.setDate_fin(rs.getTimestamp("date_fin") != null
                ? rs.getTimestamp("date_fin").toLocalDateTime()
                : LocalDateTime.now());
        evenement.setLieu(rs.getString("lieu") != null ? rs.getString("lieu") : "");
        evenement.setCapacite_max(rs.getInt("capacite_max"));
        evenement.setType(rs.getString("type") != null ? rs.getString("type") : "");
        evenement.setStatut(rs.getString("statut") != null ? rs.getString("statut") : "");
        evenement.setImage(rs.getString("image") != null ? rs.getString("image") : "");
        evenement.setCreated_at(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : LocalDateTime.now());
        evenement.setCoach_id(rs.getInt("coach_id"));
        return evenement;
    }
}