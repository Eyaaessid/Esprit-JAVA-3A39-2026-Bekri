package tn.esprit.community.dao;

import tn.esprit.community.model.Comment;
import tn.esprit.community.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommentDao {
    private static final String BASE_SELECT = """
            SELECT c.id, c.post_id, c.utilisateur_id, c.contenu, c.created_at, c.updated_at,
                   u.nom, u.prenom, u.role
            FROM commentaire c
            INNER JOIN utilisateur u ON u.id = c.utilisateur_id
            """;

    private static final String FIND_BY_POST_SQL = BASE_SELECT + """
            WHERE c.post_id = ? AND c.deleted_at IS NULL
            ORDER BY c.created_at ASC
            """;

    private static final String FIND_BY_POST_SQL_LEGACY = BASE_SELECT + """
            WHERE c.post_id = ?
            ORDER BY c.created_at ASC
            """;

    public List<Comment> findByPostId(int postId) throws SQLException {
        try {
            return executeFindByPostId(FIND_BY_POST_SQL, postId);
        } catch (SQLException exception) {
            if (!isUnknownDeletedAtColumn(exception)) {
                throw exception;
            }
            return executeFindByPostId(FIND_BY_POST_SQL_LEGACY, postId);
        }
    }

    public Comment insert(Comment comment) throws SQLException {
        String sql = """
                INSERT INTO commentaire (contenu, created_at, post_id, utilisateur_id)
                VALUES (?, NOW(), ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, comment.getContenu());
            statement.setInt(2, comment.getPostId());
            statement.setInt(3, comment.getUserId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    comment.setId(keys.getInt(1));
                }
            }
        }
        return comment;
    }

    public void update(Comment comment) throws SQLException {
        String sql = "UPDATE commentaire SET contenu = ?, updated_at = NOW() WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, comment.getContenu());
            statement.setInt(2, comment.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int commentId) throws SQLException {
        String softDeleteSql = "UPDATE commentaire SET deleted_at = NOW() WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(softDeleteSql)) {
            statement.setInt(1, commentId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (!isUnknownDeletedAtColumn(exception)) {
                throw exception;
            }
            String hardDeleteSql = "DELETE FROM commentaire WHERE id = ?";
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(hardDeleteSql)) {
                statement.setInt(1, commentId);
                statement.executeUpdate();
            }
        }
    }

    public List<Comment> findVisible(Integer postId, String sort) throws SQLException {
        try {
            return executeFindVisible(postId, sort, true);
        } catch (SQLException exception) {
            if (!isUnknownDeletedAtColumn(exception)) {
                throw exception;
            }
            return executeFindVisible(postId, sort, false);
        }
    }

    private List<Comment> executeFindByPostId(String sql, int postId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Comment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(mapComment(resultSet));
                }
                return comments;
            }
        }
    }

    private List<Comment> executeFindVisible(Integer postId, String sort, boolean includeSoftDeleteFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> parameters = new ArrayList<>();
        if (includeSoftDeleteFilter) {
            sql.append(" AND c.deleted_at IS NULL");
        }
        if (postId != null) {
            sql.append(" AND c.post_id = ?");
            parameters.add(postId);
        }
        sql.append(" ORDER BY c.created_at DESC");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Comment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(mapComment(resultSet));
                }
                if ("oldest".equalsIgnoreCase(sort)) {
                    comments.sort(Comparator.comparing(Comment::getCreatedAt));
                } else {
                    comments.sort(Comparator.comparing(Comment::getCreatedAt).reversed());
                }
                return comments;
            }
        }
    }

    private boolean isUnknownDeletedAtColumn(SQLException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("deleted_at") && normalized.contains("unknown column");
    }

    private Comment mapComment(ResultSet resultSet) throws SQLException {
        Comment comment = new Comment();
        comment.setId(resultSet.getInt("id"));
        comment.setPostId(resultSet.getInt("post_id"));
        comment.setUserId(resultSet.getInt("utilisateur_id"));
        comment.setContenu(resultSet.getString("contenu"));
        comment.setAuthorNom(resultSet.getString("nom"));
        comment.setAuthorPrenom(resultSet.getString("prenom"));
        comment.setAuthorRole(resultSet.getString("role"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            comment.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            comment.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return comment;
    }
}
