package com.bekri.services;

import com.bekri.dto.request.ProfilPsychologiqueRequestDTO;
import com.bekri.dto.response.ProfilPsychologiqueResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.exceptions.ResourceNotFoundException;
import com.bekri.utils.MyDataBase;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

@Service
public class ProfilPsychologiqueService {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    public ProfilPsychologiqueResponseDTO getForCurrentUser(Utilisateur utilisateur) {
        if (utilisateur.getId() == null) {
            throw new ResourceNotFoundException("Profil psychologique introuvable");
        }
        try {
            String sql = "SELECT * FROM profil_psychologique WHERE utilisateur_id=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, utilisateur.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new ResourceNotFoundException("Profil psychologique introuvable");
            }
            ProfilPsychologiqueResponseDTO dto = mapRowToResponse(rs);
            rs.close();
            ps.close();
            return dto;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ProfilPsychologiqueResponseDTO upsertForCurrentUser(
            Utilisateur utilisateur, ProfilPsychologiqueRequestDTO dto) {
        if (utilisateur.getId() == null) {
            throw new IllegalArgumentException("Utilisateur non persisté");
        }
        try {
            String cntSql = "SELECT COUNT(*) FROM profil_psychologique WHERE utilisateur_id=?";
            PreparedStatement cntPs = cnx.prepareStatement(cntSql);
            cntPs.setInt(1, utilisateur.getId());
            ResultSet cntRs = cntPs.executeQuery();
            cntRs.next();
            int count = cntRs.getInt(1);
            cntRs.close();
            cntPs.close();

            LocalDateTime eval = dto.getDateEvaluation() != null ? dto.getDateEvaluation() : LocalDateTime.now();

            if (count == 0) {
                String ins = """
                        INSERT INTO profil_psychologique
                        (utilisateur_id, score_global, profil_type, date_evaluation, ai_feedback)
                        VALUES (?,?,?,?,?)
                        """;
                PreparedStatement ps = cnx.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, utilisateur.getId());
                ps.setInt(2, dto.getScoreGlobal());
                ps.setString(3, dto.getProfilType());
                ps.setTimestamp(4, Timestamp.valueOf(eval));
                if (dto.getAiFeedback() != null) {
                    ps.setString(5, dto.getAiFeedback());
                } else {
                    ps.setNull(5, Types.VARCHAR);
                }
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) {
                    keys.close();
                    ps.close();
                    throw new IllegalStateException("Impossible de récupérer l'id du profil");
                }
                int newId = keys.getInt(1);
                keys.close();
                ps.close();
                return ProfilPsychologiqueResponseDTO.builder()
                        .id(newId)
                        .utilisateurId(utilisateur.getId())
                        .scoreGlobal(dto.getScoreGlobal())
                        .profilType(dto.getProfilType())
                        .dateEvaluation(eval)
                        .aiFeedback(dto.getAiFeedback())
                        .build();
            }

            String upd = """
                    UPDATE profil_psychologique
                    SET score_global=?, profil_type=?, date_evaluation=?, ai_feedback=?
                    WHERE utilisateur_id=?
                    """;
            PreparedStatement ps = cnx.prepareStatement(upd);
            ps.setInt(1, dto.getScoreGlobal());
            ps.setString(2, dto.getProfilType());
            ps.setTimestamp(3, Timestamp.valueOf(eval));
            if (dto.getAiFeedback() != null) {
                ps.setString(4, dto.getAiFeedback());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            ps.setInt(5, utilisateur.getId());
            ps.executeUpdate();
            ps.close();

            return loadResponseByUtilisateurId(utilisateur.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProfilPsychologiqueResponseDTO loadResponseByUtilisateurId(Integer utilisateurId) throws SQLException {
        String sql = "SELECT * FROM profil_psychologique WHERE utilisateur_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            rs.close();
            ps.close();
            throw new ResourceNotFoundException("Profil psychologique introuvable");
        }
        ProfilPsychologiqueResponseDTO dto = mapRowToResponse(rs);
        rs.close();
        ps.close();
        return dto;
    }

    private static ProfilPsychologiqueResponseDTO mapRowToResponse(ResultSet rs) throws SQLException {
        Timestamp de = rs.getTimestamp("date_evaluation");
        return ProfilPsychologiqueResponseDTO.builder()
                .id(rs.getObject("id", Integer.class))
                .utilisateurId(rs.getObject("utilisateur_id", Integer.class))
                .scoreGlobal(rs.getObject("score_global", Integer.class))
                .profilType(rs.getString("profil_type"))
                .dateEvaluation(de != null ? de.toLocalDateTime() : null)
                .aiFeedback(rs.getString("ai_feedback"))
                .build();
    }
}
