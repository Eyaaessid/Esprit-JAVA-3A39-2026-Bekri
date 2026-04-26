package org.example.community.dao;

import org.example.community.model.UserSummary;
import org.example.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String FIND_ALL_SQL = """
            SELECT id, nom, prenom, role
            FROM utilisateur
            WHERE statut = 'actif'
            ORDER BY role, prenom, nom
            """;

    public List<UserSummary> findAllActiveUsers() throws SQLException {
        List<UserSummary> users = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(new UserSummary(
                        resultSet.getInt("id"),
                        resultSet.getString("nom"),
                        resultSet.getString("prenom"),
                        resultSet.getString("role")
                ));
            }
        }
        return users;
    }

    public List<Integer> findAdminIds() throws SQLException {
        String sql = """
                SELECT id
                FROM utilisateur
                WHERE statut = 'actif' AND LOWER(role) = 'admin'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Integer> ids = new ArrayList<>();
            while (resultSet.next()) {
                ids.add(resultSet.getInt("id"));
            }
            return ids;
        }
    }
}
