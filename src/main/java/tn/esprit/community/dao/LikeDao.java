package tn.esprit.community.dao;

import tn.esprit.community.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LikeDao {
    public boolean hasUserLiked(int postId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM `like` WHERE post_id = ? AND utilisateur_id = ? LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean toggleLike(int postId, int userId) throws SQLException {
        if (hasUserLiked(postId, userId)) {
            String deleteSql = "DELETE FROM `like` WHERE post_id = ? AND utilisateur_id = ?";
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setInt(1, postId);
                statement.setInt(2, userId);
                statement.executeUpdate();
            }
            return false;
        }

        String insertSql = "INSERT INTO `like` (post_id, utilisateur_id, created_at) VALUES (?, ?, NOW())";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setInt(1, postId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
        return true;
    }

    public List<Integer> findLikedPostIdsByUser(int userId) throws SQLException {
        String sql = "SELECT post_id FROM `like` WHERE utilisateur_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("post_id"));
                }
                return ids;
            }
        }
    }
}

