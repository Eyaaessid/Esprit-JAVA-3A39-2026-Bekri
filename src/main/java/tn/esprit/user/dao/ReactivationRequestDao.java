package tn.esprit.user.dao;

import tn.esprit.user.entity.ReactivationRequest;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReactivationRequestDao {
    private final Connection cnx = MyDataBase.getInstance().getCnx();

    public void save(ReactivationRequest rr) {
        String sql = """
                INSERT INTO reactivation_request (utilisateur_id, reason, status, requested_at)
                VALUES (?, ?, 'PENDING', NOW())
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rr.getUtilisateurId());
            ps.setString(2, rr.getReason());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    rr.setId(keys.getInt(1));
                }
            }
            rr.setStatus("PENDING");
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.save] userId=" + rr.getUtilisateurId() + " " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'enregistrement de la demande de réactivation.", e);
        }
    }

    public List<ReactivationRequest> findPendingWithUsers() {
        String sql = """
                SELECT rr.*, u.nom, u.prenom, u.email, u.role, u.statut, u.deactivated_by
                FROM reactivation_request rr
                JOIN utilisateur u ON rr.utilisateur_id = u.id
                WHERE rr.status = 'PENDING'
                ORDER BY rr.requested_at ASC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ReactivationRequest> requests = new ArrayList<>();
            while (rs.next()) {
                requests.add(mapRow(rs, true));
            }
            return requests;
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.findPendingWithUsers] " + e.getMessage());
            throw new RuntimeException("Erreur lors du chargement des demandes de réactivation.", e);
        }
    }

    public Optional<ReactivationRequest> findPendingByUserId(int userId) {
        String sql = """
                SELECT rr.*, u.nom, u.prenom, u.email, u.role, u.statut, u.deactivated_by
                FROM reactivation_request rr
                JOIN utilisateur u ON rr.utilisateur_id = u.id
                WHERE rr.utilisateur_id = ? AND rr.status = 'PENDING'
                ORDER BY rr.requested_at DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs, true));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.findPendingByUserId] userId=" + userId + " " + e.getMessage());
            throw new RuntimeException("Erreur lors de la recherche d'une demande en attente.", e);
        }
    }

    public Optional<ReactivationRequest> findById(int requestId) {
        String sql = """
                SELECT rr.*, u.nom, u.prenom, u.email, u.role, u.statut, u.deactivated_by
                FROM reactivation_request rr
                JOIN utilisateur u ON rr.utilisateur_id = u.id
                WHERE rr.id = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs, true));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.findById] requestId=" + requestId + " " + e.getMessage());
            throw new RuntimeException("Erreur lors du chargement de la demande.", e);
        }
    }

    public void approve(int requestId) {
        String sql = "UPDATE reactivation_request SET status = 'APPROVED', processed_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.approve] requestId=" + requestId + " " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'approbation de la demande.", e);
        }
    }

    public void deny(int requestId, String adminNote) {
        String sql = """
                UPDATE reactivation_request
                SET status = 'DENIED', admin_note = ?, processed_at = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, adminNote);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.deny] requestId=" + requestId + " " + e.getMessage());
            throw new RuntimeException("Erreur lors du refus de la demande.", e);
        }
    }

    public List<ReactivationRequest> findByUserId(int userId) {
        String sql = """
                SELECT rr.*, u.nom, u.prenom, u.email, u.role, u.statut, u.deactivated_by
                FROM reactivation_request rr
                JOIN utilisateur u ON rr.utilisateur_id = u.id
                WHERE rr.utilisateur_id = ?
                ORDER BY rr.requested_at DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReactivationRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(mapRow(rs, true));
                }
                return requests;
            }
        } catch (SQLException e) {
            System.err.println("[ReactivationRequestDao.findByUserId] userId=" + userId + " " + e.getMessage());
            throw new RuntimeException("Erreur lors du chargement de l'historique des demandes.", e);
        }
    }

    private ReactivationRequest mapRow(ResultSet rs, boolean withUser) throws SQLException {
        ReactivationRequest request = new ReactivationRequest();
        request.setId(rs.getInt("id"));
        request.setUtilisateurId(rs.getInt("utilisateur_id"));
        request.setReason(rs.getString("reason"));
        request.setStatus(rs.getString("status"));
        Timestamp requestedAt = rs.getTimestamp("requested_at");
        request.setRequestedAt(requestedAt != null ? requestedAt.toLocalDateTime() : null);
        Timestamp processedAt = rs.getTimestamp("processed_at");
        request.setProcessedAt(processedAt != null ? processedAt.toLocalDateTime() : null);
        request.setAdminNote(rs.getString("admin_note"));

        if (withUser) {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setId(rs.getInt("utilisateur_id"));
            utilisateur.setNom(rs.getString("nom"));
            utilisateur.setPrenom(rs.getString("prenom"));
            utilisateur.setEmail(rs.getString("email"));
            try {
                utilisateur.setDeactivatedBy(rs.getString("deactivated_by"));
            } catch (SQLException ignored) {
                utilisateur.setDeactivatedBy(null);
            }
            request.setUtilisateur(utilisateur);
        }
        return request;
    }
}
