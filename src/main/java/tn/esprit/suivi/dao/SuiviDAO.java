package tn.esprit.suivi.dao;

import tn.esprit.suivi.model.SuiviQuotidien;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class SuiviDAO {
    private final Connection cnx = MyDataBase.getInstance().getCnx();

    public SuiviQuotidien findTodayByUser(int userId, LocalDate today) {
        String sql = """
                SELECT id, date, commentaire, utilisateur_id, soumis_at
                FROM suivi_quotidien
                WHERE utilisateur_id = ? AND date = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                SuiviQuotidien suivi = new SuiviQuotidien();
                suivi.setId(rs.getInt("id"));
                Date date = rs.getDate("date");
                suivi.setDate(date != null ? date.toLocalDate() : null);
                suivi.setCommentaire(rs.getString("commentaire"));
                suivi.setUtilisateurId(rs.getInt("utilisateur_id"));
                Timestamp ts = rs.getTimestamp("soumis_at");
                suivi.setSoumisAt(ts != null ? ts.toLocalDateTime() : null);
                return suivi;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasResponses(int suiviId) {
        String sql = "SELECT COUNT(*) FROM reponse_suivi WHERE suivi_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, suiviId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transaction-safe: prevents duplicates as much as possible without schema constraints.
     * Uses SELECT ... FOR UPDATE to lock the (userId,date) row when it exists.
     */
    public int findOrCreateToday(int userId, LocalDate date) throws SQLException {
        boolean previousAutoCommit = cnx.getAutoCommit();
        try {
            cnx.setAutoCommit(false);
            RowLock row = findTodayRowForUpdate(cnx, userId, date);
            if (row != null && row.id > 0) {
                cnx.commit();
                return row.id;
            }

            String insertSql = "INSERT INTO suivi_quotidien (date, utilisateur_id, commentaire, soumis_at) VALUES (?, ?, NULL, NULL)";
            try (PreparedStatement ps = cnx.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, Date.valueOf(date));
                ps.setInt(2, userId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        cnx.commit();
                        return newId;
                    }
                }
            }
            throw new SQLException("Creation du suivi quotidien echouee (no generated key).");
        } catch (SQLException e) {
            try {
                cnx.rollback();
            } catch (SQLException ignored) {
                // keep original exception
            }
            throw e;
        } finally {
            try {
                cnx.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // ignore restore failure
            }
        }
    }

    public void updateCommentaire(int suiviId, String commentaire) {
        String sql = "UPDATE suivi_quotidien SET commentaire = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, commentaire);
            ps.setInt(2, suiviId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsertResponse(int suiviId, int questionId, String valeur) {
        try {
            upsertResponse(cnx, suiviId, questionId, valeur);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void upsertResponse(Connection connection, int suiviId, int questionId, String valeur) throws SQLException {
        String checkSql = "SELECT id FROM reponse_suivi WHERE suivi_id = ? AND question_id = ? LIMIT 1";
        try (PreparedStatement checkPs = connection.prepareStatement(checkSql)) {
            checkPs.setInt(1, suiviId);
            checkPs.setInt(2, questionId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    String updateSql = "UPDATE reponse_suivi SET valeur = ? WHERE id = ?";
                    try (PreparedStatement updatePs = connection.prepareStatement(updateSql)) {
                        updatePs.setString(1, valeur);
                        updatePs.setInt(2, rs.getInt("id"));
                        updatePs.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO reponse_suivi (valeur, suivi_id, question_id) VALUES (?, ?, ?)";
                    try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                        insertPs.setString(1, valeur);
                        insertPs.setInt(2, suiviId);
                        insertPs.setInt(3, questionId);
                        insertPs.executeUpdate();
                    }
                }
            }
        }
    }

    public Map<Integer, String> getAnswersBySuiviId(int suiviId) {
        String sql = "SELECT question_id, valeur FROM reponse_suivi WHERE suivi_id = ?";
        Map<Integer, String> map = new LinkedHashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, suiviId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("question_id"), rs.getString("valeur"));
                }
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSubmitted(int userId, LocalDate date) {
        String sql = """
                SELECT 1
                FROM suivi_quotidien
                WHERE utilisateur_id = ? AND date = ? AND soumis_at IS NOT NULL
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void markSoumisAtNow(int suiviId) throws SQLException {
        String sql = "UPDATE suivi_quotidien SET soumis_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, suiviId);
            ps.executeUpdate();
        }
    }

    public void submitCheckInAtomic(int userId, LocalDate date, String commentaire, Map<Integer, String> answers) throws SQLException {
        boolean previousAutoCommit = cnx.getAutoCommit();
        try {
            cnx.setAutoCommit(false);

            RowLock row = findOrCreateTodayInTx(cnx, userId, date);
            if (row.soumisAt != null) {
                throw new SQLException("ALREADY_SUBMITTED");
            }
            int suiviId = row.id;
            String updateSql = "UPDATE suivi_quotidien SET commentaire = ?, soumis_at = NOW() WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
                ps.setString(1, commentaire);
                ps.setInt(2, suiviId);
                ps.executeUpdate();
            }

            if (answers != null) {
                for (Map.Entry<Integer, String> e : answers.entrySet()) {
                    upsertResponse(cnx, suiviId, e.getKey(), e.getValue());
                }
            }

            cnx.commit();
        } catch (SQLException e) {
            try {
                cnx.rollback();
            } catch (SQLException ignored) {
                // keep original exception
            }
            throw e;
        } finally {
            try {
                cnx.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // ignore restore failure
            }
        }
    }

    private RowLock findTodayRowForUpdate(Connection connection, int userId, LocalDate date) throws SQLException {
        String sql = "SELECT id, soumis_at FROM suivi_quotidien WHERE utilisateur_id = ? AND date = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                RowLock row = new RowLock();
                row.id = rs.getInt("id");
                Timestamp ts = rs.getTimestamp("soumis_at");
                row.soumisAt = ts == null ? null : ts.toLocalDateTime();
                return row;
            }
        }
    }

    private RowLock findOrCreateTodayInTx(Connection connection, int userId, LocalDate date) throws SQLException {
        RowLock existing = findTodayRowForUpdate(connection, userId, date);
        if (existing != null && existing.id > 0) {
            return existing;
        }

        String insertSql = "INSERT INTO suivi_quotidien (date, utilisateur_id, commentaire, soumis_at) VALUES (?, ?, NULL, NULL)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    RowLock created = new RowLock();
                    created.id = rs.getInt(1);
                    created.soumisAt = null;
                    return created;
                }
            }
        }
        throw new SQLException("Creation du suivi quotidien echouee (no generated key).");
    }

    private static final class RowLock {
        private int id;
        private java.time.LocalDateTime soumisAt;
    }
}
