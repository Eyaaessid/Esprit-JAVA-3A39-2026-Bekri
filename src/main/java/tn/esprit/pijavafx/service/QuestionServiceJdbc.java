package tn.esprit.pijavafx.service;

import tn.esprit.pijavafx.model.QuestionEvaluationDto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of IQuestionService.
 * Connects directly to MySQL — no Spring Boot backend needed.
 */
public class QuestionServiceJdbc implements IQuestionService {

    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/bekri_db"
            + "?useSSL=false&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────
    @Override
    public List<QuestionEvaluationDto> getAll() throws Exception {
        String sql = "SELECT * FROM question_evaluation ORDER BY id DESC";
        List<QuestionEvaluationDto> list = new ArrayList<>();
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    @Override
    public QuestionEvaluationDto create(QuestionEvaluationDto dto) throws Exception {
        validate(dto);

        String sql = "INSERT INTO question_evaluation "
                + "(texte, category, type_reponse, option1, option2, option3, min_value, max_value) "
                + "VALUES (?,?,?,?,?,?,?,?)";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dto.getTexte());
            ps.setString(2, dto.getCategory());
            ps.setString(3, dto.getTypeReponse() != null ? dto.getTypeReponse() : "choice");
            ps.setString(4, dto.getOption1());
            ps.setString(5, dto.getOption2());
            ps.setString(6, dto.getOption3());
            ps.setObject(7, null); // min_value not used
            ps.setObject(8, null); // max_value not used
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) dto.setId(keys.getInt(1));
            }
        }
        return dto;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public QuestionEvaluationDto update(Integer id, QuestionEvaluationDto dto) throws Exception {
        validate(dto);

        String sql = "UPDATE question_evaluation "
                + "SET texte=?, category=?, type_reponse=?, "
                + "option1=?, option2=?, option3=?, min_value=?, max_value=? "
                + "WHERE id=?";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, dto.getTexte());
            ps.setString(2, dto.getCategory());
            ps.setString(3, dto.getTypeReponse() != null ? dto.getTypeReponse() : "choice");
            ps.setString(4, dto.getOption1());
            ps.setString(5, dto.getOption2());
            ps.setString(6, dto.getOption3());
            ps.setObject(7, null);
            ps.setObject(8, null);
            ps.setInt(9, id);
            ps.executeUpdate();
        }
        dto.setId(id);
        return dto;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public void delete(Integer id) throws Exception {
        String sql = "DELETE FROM question_evaluation WHERE id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── ResultSet → DTO ───────────────────────────────────────────────────────
    private QuestionEvaluationDto mapRow(ResultSet rs) throws SQLException {
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setId(rs.getInt("id"));
        dto.setTexte(rs.getString("texte"));
        dto.setCategory(rs.getString("category"));
        dto.setTypeReponse(rs.getString("type_reponse"));
        dto.setOption1(rs.getString("option1"));
        dto.setOption2(rs.getString("option2"));
        dto.setOption3(rs.getString("option3"));
        return dto;
    }

    // ── Validation (replaces @Valid from Spring) ───────────────────────────────
    private void validate(QuestionEvaluationDto dto) throws Exception {
        if (dto.getTexte() == null || dto.getTexte().isBlank())
            throw new Exception("Le texte de la question est obligatoire.");
        if (dto.getTexte().trim().length() < 5)
            throw new Exception("La question doit contenir au moins 5 caractères.");
        if (dto.getTexte().trim().length() > 255)
            throw new Exception("La question ne peut pas dépasser 255 caractères.");
        if (dto.getCategory() == null || dto.getCategory().isBlank())
            throw new Exception("La catégorie est obligatoire.");
        if (dto.getOption1() == null || dto.getOption1().isBlank())
            throw new Exception("L'option 1 est obligatoire.");
        if (dto.getOption2() == null || dto.getOption2().isBlank())
            throw new Exception("L'option 2 est obligatoire.");
        if (dto.getOption3() == null || dto.getOption3().isBlank())
            throw new Exception("L'option 3 est obligatoire.");
        if (dto.getOption1().length() > 100)
            throw new Exception("L'option 1 ne peut pas dépasser 100 caractères.");
        if (dto.getOption2().length() > 100)
            throw new Exception("L'option 2 ne peut pas dépasser 100 caractères.");
        if (dto.getOption3().length() > 100)
            throw new Exception("L'option 3 ne peut pas dépasser 100 caractères.");
    }
}