package tn.esprit.suivi.dao;

import tn.esprit.suivi.model.QuestionEvaluation;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionEvaluationDAO {
    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }

    public List<QuestionEvaluation> findAll() {
        Connection cnx = getCnx();
        String sql = """
                SELECT id, texte, category, type_reponse, option1, option2, option3, min_value, max_value
                FROM question_evaluation
                ORDER BY id ASC
                """;
        List<QuestionEvaluation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<QuestionEvaluation> findByCategories(List<String> categories) {
        Connection cnx = getCnx();
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<String> normalized = categories.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());

        if (normalized.isEmpty()) {
            return List.of();
        }

        String placeholders = normalized.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT id, texte, category, type_reponse, option1, option2, option3, min_value, max_value
                FROM question_evaluation
                WHERE LOWER(TRIM(category)) IN (%s)
                ORDER BY id ASC
                """.formatted(placeholders);

        List<QuestionEvaluation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (int i = 0; i < normalized.size(); i++) {
                ps.setString(i + 1, normalized.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
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
        q.setMinValue((Integer) rs.getObject("min_value"));
        q.setMaxValue((Integer) rs.getObject("max_value"));
        return q;
    }
}
