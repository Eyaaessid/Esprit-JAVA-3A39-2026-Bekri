package tn.esprit.objectif.dao;

import tn.esprit.objectif.entity.QuestionEvaluation;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionEvaluationDao {
    private final Connection cnx = MyDataBase.getInstance().getCnx();

    public List<QuestionEvaluation> findAll() {
        String sql = "SELECT * FROM question_evaluation ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<QuestionEvaluation> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<QuestionEvaluation> findById(Integer id) {
        String sql = "SELECT * FROM question_evaluation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public QuestionEvaluation save(QuestionEvaluation q) {
        try {
            if (q.getId() == null) {
                String sql = "INSERT INTO question_evaluation "
                        + "(texte, category, type_reponse, option1, option2, option3, min_value, max_value) "
                        + "VALUES (?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, q.getTexte());
                    ps.setString(2, q.getCategory());
                    ps.setString(3, q.getTypeReponse());
                    ps.setString(4, q.getOption1());
                    ps.setString(5, q.getOption2());
                    ps.setString(6, q.getOption3());
                    if (q.getMinValue() != null) {
                        ps.setInt(7, q.getMinValue());
                    } else {
                        ps.setObject(7, null);
                    }
                    if (q.getMaxValue() != null) {
                        ps.setInt(8, q.getMaxValue());
                    } else {
                        ps.setObject(8, null);
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            q.setId(keys.getInt(1));
                        }
                    }
                }
                return q;
            }

            String sql = "UPDATE question_evaluation "
                    + "SET texte=?, category=?, type_reponse=?, "
                    + "option1=?, option2=?, option3=?, "
                    + "min_value=?, max_value=? "
                    + "WHERE id=?";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, q.getTexte());
                ps.setString(2, q.getCategory());
                ps.setString(3, q.getTypeReponse());
                ps.setString(4, q.getOption1());
                ps.setString(5, q.getOption2());
                ps.setString(6, q.getOption3());
                if (q.getMinValue() != null) {
                    ps.setInt(7, q.getMinValue());
                } else {
                    ps.setObject(7, null);
                }
                if (q.getMaxValue() != null) {
                    ps.setInt(8, q.getMaxValue());
                } else {
                    ps.setObject(8, null);
                }
                ps.setInt(9, q.getId());
                ps.executeUpdate();
            }
            return q;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsById(Integer id) {
        String sql = "SELECT COUNT(*) FROM question_evaluation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(Integer id) {
        String sql = "DELETE FROM question_evaluation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private QuestionEvaluation mapRow(ResultSet rs) throws SQLException {
        QuestionEvaluation q = new QuestionEvaluation();
        q.setId(rs.getInt("id"));
        q.setTexte(rs.getString("texte"));
        q.setCategory(rs.getString("category"));
        q.setTypeReponse(rs.getString("type_reponse"));
        q.setOption1(rs.getString("option1"));
        q.setOption2(rs.getString("option2"));
        q.setOption3(rs.getString("option3"));
        q.setMinValue(rs.getObject("min_value") != null ? rs.getInt("min_value") : null);
        q.setMaxValue(rs.getObject("max_value") != null ? rs.getInt("max_value") : null);
        return q;
    }
}
