package tn.esprit.suivi.service;

import tn.esprit.suivi.dao.ObjectifBienEtreDAO;
import tn.esprit.suivi.dao.QuestionEvaluationDAO;
import tn.esprit.suivi.dao.SuiviDAO;
import tn.esprit.suivi.model.QuestionEvaluation;
import tn.esprit.suivi.model.SuiviQuotidien;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CheckInService {
    private final QuestionEvaluationDAO questionDAO = new QuestionEvaluationDAO();
    private final ObjectifBienEtreDAO objectifDAO = new ObjectifBienEtreDAO();
    private final SuiviDAO suiviDAO = new SuiviDAO();

    public List<QuestionEvaluation> loadQuestionsForUser(int userId) {
        List<String> types = objectifDAO.findActiveTypesByUser(userId);
        if (types.isEmpty()) {
            return questionDAO.findAll();
        }
        List<QuestionEvaluation> filtered = questionDAO.findByCategories(types);
        if (filtered.isEmpty()) {
            return questionDAO.findAll();
        }
        return filtered;
    }

    public boolean isAlreadySubmitted(int userId, LocalDate date) {
        SuiviQuotidien existing = suiviDAO.findTodayByUser(userId, date);
        return existing != null && existing.getSoumisAt() != null;
    }

    public void submitCheckIn(int userId, LocalDate date, String commentaire, Map<Integer, String> answers) {
        if (commentaire != null && commentaire.length() > 1000) {
            throw new IllegalArgumentException("Le commentaire ne doit pas depasser 1000 caracteres.");
        }
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Veuillez repondre a toutes les questions.");
        }
        for (Map.Entry<Integer, String> answer : answers.entrySet()) {
            if (answer.getValue() == null || answer.getValue().isBlank()) {
                throw new IllegalArgumentException("Chaque question doit avoir une reponse valide.");
            }
        }

        SuiviQuotidien existing = suiviDAO.findTodayByUser(userId, date);
        if (existing != null && existing.getSoumisAt() != null) {
            throw new IllegalStateException("duplicate");
        }

        String safeComment = commentaire == null ? "" : commentaire.trim();
        try {
            suiviDAO.submitCheckInAtomic(userId, date, safeComment, answers);
        } catch (SQLException e) {
            if ("ALREADY_SUBMITTED".equalsIgnoreCase(e.getMessage())) {
                throw new IllegalStateException("duplicate");
            }
            throw new RuntimeException(e);
        }
    }
}
