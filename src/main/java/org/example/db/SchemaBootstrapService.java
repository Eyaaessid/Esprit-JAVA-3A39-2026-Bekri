package org.example.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaBootstrapService {
    public void ensureAdvancedCommunityTables() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS saved_post (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        utilisateur_id INT NOT NULL,
                        post_id INT NOT NULL,
                        created_at DATETIME NOT NULL,
                        UNIQUE KEY uniq_saved_post_user_post (utilisateur_id, post_id),
                        INDEX idx_saved_user (utilisateur_id),
                        INDEX idx_saved_post (post_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS post_notification (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        recipient_id INT NOT NULL,
                        actor_id INT NOT NULL,
                        post_id INT NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        message LONGTEXT NOT NULL,
                        is_read TINYINT(1) NOT NULL DEFAULT 0,
                        created_at DATETIME NOT NULL,
                        INDEX idx_post_notification_recipient (recipient_id),
                        INDEX idx_post_notification_actor (actor_id),
                        INDEX idx_post_notification_post (post_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }
}
