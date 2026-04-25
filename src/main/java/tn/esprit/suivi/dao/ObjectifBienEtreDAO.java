package tn.esprit.suivi.dao;

import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ObjectifBienEtreDAO {
    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public List<String> findActiveTypesByUser(int userId) {
        String sql = """
                SELECT type
                FROM objectif_bien_etre
                WHERE utilisateur_id = ?
                  AND (statut IS NULL OR LOWER(TRIM(statut)) <> 'termine')
                """;
        List<String> types = executeTypeQuery(sql, userId);
        if (!types.isEmpty()) {
            return types;
        }

        String fallbackSql = """
                SELECT type
                FROM objectif_bien_etre
                WHERE utilisateur_id = ?
                """;
        return executeTypeQuery(fallbackSql, userId);
    }

    private List<String> executeTypeQuery(String sql, int userId) {
        Connection cnx = getCnx();
        List<String> types = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    if (type != null && !type.isBlank()) {
                        String normalized = type.trim().toLowerCase();
                        if (!types.contains(normalized)) {
                            types.add(normalized);
                        }
                    }
                }
            }
            return types;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
