package tn.esprit.testmental.dao;

import tn.esprit.testmental.model.Question;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    public void ajouter(Question q) {
        String sql = "INSERT INTO question (contenu, choix_a, choix_b, choix_c, bonne_reponse, test_mental_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getContenu());
            ps.setString(2, q.getChoixA());
            ps.setString(3, q.getChoixB());
            ps.setString(4, q.getChoixC());
            ps.setString(5, q.getBonneReponse());
            ps.setInt(6, q.getTestMentalId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Question> afficher() {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM question";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                questions.add(new Question(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getString("choix_a"),
                        rs.getString("choix_b"),
                        rs.getString("choix_c"),
                        rs.getString("bonne_reponse"),
                        rs.getInt("test_mental_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }

    public void modifier(Question q) {
        String sql = "UPDATE question SET contenu=?, choix_a=?, choix_b=?, choix_c=?, bonne_reponse=?, test_mental_id=? WHERE id=?";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getContenu());
            ps.setString(2, q.getChoixA());
            ps.setString(3, q.getChoixB());
            ps.setString(4, q.getChoixC());
            ps.setString(5, q.getBonneReponse());
            ps.setInt(6, q.getTestMentalId());
            ps.setInt(7, q.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM question WHERE id=?";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}