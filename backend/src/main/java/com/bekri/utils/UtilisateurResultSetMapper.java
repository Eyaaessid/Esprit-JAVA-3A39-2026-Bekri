package com.bekri.utils;

import com.bekri.entities.Admin;
import com.bekri.entities.Coach;
import com.bekri.entities.User;
import com.bekri.entities.Utilisateur;
import com.bekri.enums.UtilisateurStatut;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Maps a row from {@code utilisateur} (SELECT *) to the correct {@link Utilisateur} subclass.
 */
public final class UtilisateurResultSetMapper {

    private UtilisateurResultSetMapper() {
    }

    public static Utilisateur mapRow(ResultSet rs) throws SQLException {
        String roleDb = rs.getString("role");
        String r = roleDb == null || roleDb.isBlank() ? "user" : roleDb.trim().toLowerCase(Locale.ROOT);
        Utilisateur u = switch (r) {
            case "admin" -> new Admin();
            case "coach" -> new Coach();
            default -> new User();
        };

        u.setId(rs.getObject("id", Integer.class));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setTelephone(rs.getString("telephone"));
        var dn = rs.getDate("date_naissance");
        u.setDateNaissance(dn != null ? dn.toLocalDate() : null);
        u.setAvatar(rs.getString("avatar"));
        u.setRole(rs.getString("role"));
        u.setStatut(UtilisateurStatut.fromDbValue(rs.getString("statut")));
        u.setScoreInitial(rs.getObject("score_initial") != null ? rs.getInt("score_initial") : null);
        var dei = rs.getDate("date_evaluation_initiale");
        u.setDateEvaluationInitiale(dei != null ? dei.toLocalDate() : null);
        var cat = rs.getTimestamp("created_at");
        u.setCreatedAt(cat != null ? cat.toLocalDateTime() : null);
        var uat = rs.getTimestamp("updated_at");
        u.setUpdatedAt(uat != null ? uat.toLocalDateTime() : null);
        u.setResetToken(rs.getString("reset_token"));
        var rte = rs.getTimestamp("reset_token_expires_at");
        u.setResetTokenExpiresAt(rte != null ? rte.toLocalDateTime() : null);
        var da = rs.getTimestamp("deactivated_at");
        u.setDeactivatedAt(da != null ? da.toLocalDateTime() : null);
        u.setDeactivatedBy(rs.getString("deactivated_by"));
        u.setReactivationToken(rs.getString("reactivation_token"));
        var rte2 = rs.getTimestamp("reactivation_token_expires_at");
        u.setReactivationTokenExpiresAt(rte2 != null ? rte2.toLocalDateTime() : null);
        var lla = rs.getTimestamp("last_login_at");
        u.setLastLoginAt(lla != null ? lla.toLocalDateTime() : null);
        u.setVerified(rs.getBoolean("is_verified"));
        u.setFaceDescriptor(rs.getString("face_descriptor"));
        u.setFaceAuthEnabled(rs.getBoolean("face_auth_enabled"));
        var fra = rs.getTimestamp("face_registered_at");
        u.setFaceRegisteredAt(fra != null ? fra.toLocalDateTime() : null);
        Integer faceFails = rs.getObject("face_auth_failed_attempts", Integer.class);
        u.setFaceAuthFailedAttempts(faceFails != null ? faceFails : 0);
        var lfa = rs.getTimestamp("last_face_auth_attempt_at");
        u.setLastFaceAuthAttemptAt(lfa != null ? lfa.toLocalDateTime() : null);
        u.setTotpSecret(rs.getString("totp_secret"));
        u.setTwoFactorEnabled(rs.getBoolean("is_two_factor_enabled"));
        u.setBackupCodes(rs.getString("backup_codes"));
        var tfa = rs.getTimestamp("two_factor_enabled_at");
        u.setTwoFactorEnabledAt(tfa != null ? tfa.toLocalDateTime() : null);

        return u;
    }
}
