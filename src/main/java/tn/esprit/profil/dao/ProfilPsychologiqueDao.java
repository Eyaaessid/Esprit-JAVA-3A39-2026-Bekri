package tn.esprit.profil.dao;

import tn.esprit.profil.entity.ProfilPsychologique;
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

public class ProfilPsychologiqueDao {

    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public ProfilPsychologique save(ProfilPsychologique p) {
        Connection cnx = getCnx();
        try {
            if (p.getId() == null) {
                String sql = "INSERT INTO profil_psychologique "
                        + "(utilisateur_id, score_global, profil_type, date_evaluation, ai_feedback) "
                        + "VALUES (?, ?, ?, NOW(), ?)";
                try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, p.getUtilisateurId());
                    ps.setInt(2, p.getScoreGlobal());
                    ps.setString(3, p.getProfilType());
                    ps.setString(4, p.getAiFeedback());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            p.setId(keys.getInt(1));
                        }
                    }
                }
                return reloadById(p.getId());
            } else {
                String sql = "UPDATE profil_psychologique "
                        + "SET score_global=?, profil_type=?, date_evaluation=NOW(), ai_feedback=? "
                        + "WHERE id=?";
                try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                    ps.setInt(1, p.getScoreGlobal());
                    ps.setString(2, p.getProfilType());
                    ps.setString(3, p.getAiFeedback());
                    ps.setInt(4, p.getId());
                    ps.executeUpdate();
                }
                return reloadById(p.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProfilPsychologique reloadById(Integer id) {
        if (id == null) {
            return null;
        }
        return findById(id).orElse(null);
    }

    private Optional<ProfilPsychologique> findById(Integer id) {
        Connection cnx = getCnx();
        String sql = "SELECT * FROM profil_psychologique WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<ProfilPsychologique> findByUtilisateurId(Integer utilisateurId) {
        Connection cnx = getCnx();
        String sql = "SELECT * FROM profil_psychologique WHERE utilisateur_id = ? "
                + "ORDER BY date_evaluation DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsByUtilisateurId(Integer utilisateurId) {
        if (utilisateurId == null) {
            return false;
        }

        Connection cnx = getCnx();
        String sql = "SELECT 1 FROM profil_psychologique WHERE utilisateur_id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ProfilPsychologique> findAll() {
        Connection cnx = getCnx();
        String sql = "SELECT * FROM profil_psychologique ORDER BY date_evaluation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ProfilPsychologique> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProfilPsychologique mapRow(ResultSet rs) throws SQLException {
        ProfilPsychologique p = new ProfilPsychologique();
        p.setId(rs.getInt("id"));
        p.setUtilisateurId(rs.getInt("utilisateur_id"));
        p.setScoreGlobal(rs.getInt("score_global"));
        p.setProfilType(rs.getString("profil_type"));
        Timestamp t = rs.getTimestamp("date_evaluation");
        p.setDateEvaluation(t != null ? t.toLocalDateTime() : null);
        p.setAiFeedback(rs.getString("ai_feedback"));
        return p;
    }
}
