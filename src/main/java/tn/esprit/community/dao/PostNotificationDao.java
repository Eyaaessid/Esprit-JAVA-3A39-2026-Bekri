package tn.esprit.community.dao;

import tn.esprit.community.model.PostNotification;
import tn.esprit.community.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostNotificationDao {
    public void insert(int recipientId, int actorId, int postId, String type, String message) throws SQLException {
        String sql = """
                INSERT INTO post_notification (recipient_id, actor_id, post_id, type, message, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, 0, NOW())
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recipientId);
            statement.setInt(2, actorId);
            statement.setInt(3, postId);
            statement.setString(4, type);
            statement.setString(5, message);
            statement.executeUpdate();
        }
    }

    public List<PostNotification> findForRecipient(int recipientId, int limit) throws SQLException {
        String sql = """
                SELECT n.id, n.recipient_id, n.actor_id, n.post_id, n.type, n.message, n.is_read, n.created_at,
                       a.nom AS actor_nom, a.prenom AS actor_prenom, p.titre AS post_titre
                FROM post_notification n
                INNER JOIN utilisateur a ON a.id = n.actor_id
                INNER JOIN post p ON p.id = n.post_id
                WHERE n.recipient_id = ?
                ORDER BY n.created_at DESC
                LIMIT ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recipientId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PostNotification> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
                return notifications;
            }
        }
    }

    public int countUnread(int recipientId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM post_notification WHERE recipient_id = ? AND is_read = 0";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recipientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
                return 0;
            }
        }
    }

    public void markAllRead(int recipientId) throws SQLException {
        String sql = "UPDATE post_notification SET is_read = 1 WHERE recipient_id = ? AND is_read = 0";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recipientId);
            statement.executeUpdate();
        }
    }

    private PostNotification mapNotification(ResultSet resultSet) throws SQLException {
        PostNotification notification = new PostNotification();
        notification.setId(resultSet.getInt("id"));
        notification.setRecipientId(resultSet.getInt("recipient_id"));
        notification.setActorId(resultSet.getInt("actor_id"));
        notification.setPostId(resultSet.getInt("post_id"));
        notification.setType(resultSet.getString("type"));
        notification.setMessage(resultSet.getString("message"));
        notification.setRead(resultSet.getBoolean("is_read"));
        notification.setActorDisplayName(resultSet.getString("actor_prenom") + " " + resultSet.getString("actor_nom"));
        notification.setPostTitle(resultSet.getString("post_titre"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        return notification;
    }
}

