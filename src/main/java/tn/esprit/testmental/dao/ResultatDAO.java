package tn.esprit.testmental.dao;

import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ResultatDAO {

    public void saveResult(int id, double score, String resultat) {
        String sql = "INSERT INTO resultat_test (id, score, resultat) VALUES (?, ?, ?)";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setDouble(2, score);
            ps.setString(3, resultat);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}