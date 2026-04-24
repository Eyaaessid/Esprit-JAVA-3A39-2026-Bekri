package tn.esprit.user.dao;

import tn.esprit.user.entity.Utilisateur;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TwoFactorDAO {
    private final Connection connection = MyDataBase.getInstance().getCnx();

    public void saveTotpSecret(int userId, String secret) {
        String sql = "UPDATE utilisateur SET totp_secret=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, secret);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void enableTwoFactor(int userId, String backupCodesJson) {
        String sql = "UPDATE utilisateur SET is_two_factor_enabled=1, two_factor_enabled_at=NOW(), backup_codes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, backupCodesJson);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void disableTwoFactor(int userId) {
        String sql = "UPDATE utilisateur SET is_two_factor_enabled=0, totp_secret=NULL, backup_codes=NULL, two_factor_enabled_at=NULL WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateBackupCodes(int userId, String backupCodesJson) {
        String sql = "UPDATE utilisateur SET backup_codes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, backupCodesJson);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadTwoFactorFields(Utilisateur user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String sql = "SELECT totp_secret, is_two_factor_enabled, backup_codes, two_factor_enabled_at FROM utilisateur WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setTotpSecret(rs.getString("totp_secret"));
                    user.setTwoFactorEnabled(rs.getBoolean("is_two_factor_enabled"));
                    user.setBackupCodes(rs.getString("backup_codes"));
                    Timestamp enabledAt = rs.getTimestamp("two_factor_enabled_at");
                    user.setTwoFactorEnabledAt(enabledAt != null ? enabledAt.toLocalDateTime() : null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
