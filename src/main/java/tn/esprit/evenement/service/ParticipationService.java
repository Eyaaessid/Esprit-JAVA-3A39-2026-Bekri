package tn.esprit.evenement.service;

import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.entity.ParticipationDisplay;
import tn.esprit.evenement.entity.ParticipationEvenement;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private Connection cnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public void ajouter(ParticipationEvenement p) {
        String sql = "INSERT INTO participation_evenement (date_inscription, statut, utilisateur_id, evenement_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(p.getDate_inscription()));
            ps.setString(2, p.getStatut());
            ps.setInt(3, p.getUtilisateur_id());
            ps.setInt(4, p.getEvenement_id());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Erreur ajout participation: " + ex.getMessage(), ex);
        }
    }

    public void modifier(ParticipationEvenement p) {
        String sql = "UPDATE participation_evenement SET statut=? WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, p.getStatut());
            ps.setInt(2, p.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Erreur modification participation: " + ex.getMessage(), ex);
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM participation_evenement WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Erreur suppression participation: " + ex.getMessage(), ex);
        }
    }

    public List<ParticipationEvenement> afficherAll() {
        String sql = "SELECT * FROM participation_evenement ORDER BY date_inscription DESC";
        List<ParticipationEvenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ParticipationEvenement p = new ParticipationEvenement();
                p.setId(rs.getInt("id"));
                p.setStatut(rs.getString("statut"));
                Timestamp t = rs.getTimestamp("date_inscription");
                p.setDate_inscription(t != null ? t.toLocalDateTime() : null);
                p.setUtilisateur_id(rs.getInt("utilisateur_id"));
                p.setEvenement_id(rs.getInt("evenement_id"));
                list.add(p);
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lecture participations: " + ex.getMessage(), ex);
        }
    }

    public List<ParticipationDisplay> afficherAllEnriched(int userId) {
        String sql = "SELECT p.id as p_id, p.date_inscription, p.statut as p_statut, " +
                "e.id as e_id, e.titre, e.description, e.date_debut, e.date_fin, " +
                "e.capacite_max, e.type, e.statut as e_statut, e.lien_session, e.created_at, e.coach_id " +
                "FROM participation_evenement p JOIN evenement e ON e.id = p.evenement_id " +
                "WHERE p.utilisateur_id=? ORDER BY p.date_inscription DESC";
        List<ParticipationDisplay> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParticipationDisplay d = new ParticipationDisplay();
                    d.setParticipationId(rs.getInt("p_id"));
                    d.setStatutParticipation(rs.getString("p_statut"));
                    Timestamp inscriptionTs = rs.getTimestamp("date_inscription");
                    d.setDateInscription(inscriptionTs != null ? inscriptionTs.toLocalDateTime() : null);
                    d.setEvenement(mapEvenement(rs));
                    list.add(d);
                }
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lecture participations enrichies: " + ex.getMessage(), ex);
        }
    }

    public boolean estDejaInscrit(int userId, int evenementId) {
        String sql = "SELECT COUNT(*) FROM participation_evenement WHERE utilisateur_id=? AND evenement_id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Erreur verification inscription: " + ex.getMessage(), ex);
        }
    }

    private Evenement mapEvenement(ResultSet rs) throws Exception {
        Evenement e = new Evenement();
        e.setId(rs.getInt("e_id"));
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
        e.setStatut(rs.getString("e_statut"));
        e.setLien_session(rs.getString("lien_session"));
        e.setCoach_id(rs.getInt("coach_id"));
        return e;
    }
}
