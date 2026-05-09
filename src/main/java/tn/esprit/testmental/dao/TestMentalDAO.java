package tn.esprit.testmental.dao;

import tn.esprit.testmental.model.TestMental;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestMentalDAO {

    public void ajouter(TestMental test) {
        String sql = "INSERT INTO test_mental (titre, description, niveau, duree, type_test) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, test.getTitre());
            ps.setString(2, test.getDescription());
            ps.setString(3, test.getNiveau());
            ps.setInt(4, test.getDuree());
            ps.setString(5, test.getTypeTest());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<TestMental> afficher() {
        List<TestMental> liste = new ArrayList<>();
        String sql = "SELECT * FROM test_mental";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(new TestMental(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("niveau"),
                        rs.getInt("duree"),
                        rs.getString("type_test")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

    public void modifier(TestMental test) {
        String sql = "UPDATE test_mental SET titre=?, description=?, niveau=?, duree=?, type_test=? WHERE id=?";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, test.getTitre());
            ps.setString(2, test.getDescription());
            ps.setString(3, test.getNiveau());
            ps.setInt(4, test.getDuree());
            ps.setString(5, test.getTypeTest());
            ps.setInt(6, test.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM test_mental WHERE id=?";
        try (Connection conn = MyDataBase.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}