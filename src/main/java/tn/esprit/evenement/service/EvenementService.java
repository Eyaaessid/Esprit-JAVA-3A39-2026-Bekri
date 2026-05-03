package tn.esprit.evenement.service;

import tn.esprit.evenement.entity.Evenement;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {
    private Connection cnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public void ajouter(Evenement e) {
        String sql = "INSERT INTO evenement (titre, description, date_debut, date_fin, capacite_max, type, statut, lien_session, created_at, coach_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDate_debut()));
            ps.setTimestamp(4, Timestamp.valueOf(e.getDate_fin()));
            ps.setInt(5, e.getCapacite_max());
            ps.setString(6, e.getType());
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getLien_session());
            ps.setInt(9, e.getCoach_id());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Erreur ajout evenement: " + ex.getMessage(), ex);
        }
    }

    public void modifier(Evenement e) {
        String sql = "UPDATE evenement SET titre=?, description=?, date_debut=?, date_fin=?, capacite_max=?, type=?, statut=?, lien_session=? WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDate_debut()));
            ps.setTimestamp(4, Timestamp.valueOf(e.getDate_fin()));
            ps.setInt(5, e.getCapacite_max());
            ps.setString(6, e.getType());
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getLien_session());
            ps.setInt(9, e.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Erreur modification evenement: " + ex.getMessage(), ex);
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM evenement WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Erreur suppression evenement: " + ex.getMessage(), ex);
        }
    }

    public List<Evenement> afficherAll() {
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lecture evenements: " + ex.getMessage(), ex);
        }
    }

    public List<Evenement> getEvenementsParMois(int year, int month) {
        String sql = "SELECT * FROM evenement WHERE YEAR(date_debut)=? AND MONTH(date_debut)=? ORDER BY date_debut";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lecture evenements par mois: " + ex.getMessage(), ex);
        }
    }

    public List<Evenement> getEvenementsParDate(LocalDate date) {
        String sql = "SELECT * FROM evenement WHERE DATE(date_debut)=? ORDER BY date_debut";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lecture evenements par date: " + ex.getMessage(), ex);
        }
    }

    private Evenement map(ResultSet rs) throws Exception {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        Timestamp d1 = rs.getTimestamp("date_debut");
        Timestamp d2 = rs.getTimestamp("date_fin");
        Timestamp d3 = rs.getTimestamp("created_at");
        e.setDate_debut(d1 != null ? d1.toLocalDateTime() : null);
        e.setDate_fin(d2 != null ? d2.toLocalDateTime() : null);
        e.setCreated_at(d3 != null ? d3.toLocalDateTime() : null);
        e.setCapacite_max(rs.getInt("capacite_max"));
        e.setType(rs.getString("type"));
        e.setStatut(rs.getString("statut"));
        e.setLien_session(rs.getString("lien_session"));
        e.setCoach_id(rs.getInt("coach_id"));
        return e;
    }
}