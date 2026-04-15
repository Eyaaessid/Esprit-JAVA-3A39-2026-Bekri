package tn.esprit.pijava.service;

import tn.esprit.pijava.entity.QuestionEvaluation;
import tn.esprit.pijava.repository.QuestionEvaluationDao;
import tn.esprit.pijava.validation.QuestionEvaluationValidator;

import java.util.List;
import java.util.Optional;

public class QuestionEvaluationService {
    private final QuestionEvaluationDao dao;
    private final QuestionEvaluationValidator validator;

    public QuestionEvaluationService(QuestionEvaluationDao dao, QuestionEvaluationValidator validator) {
        this.dao = dao;
        this.validator = validator;
    }

    public List<QuestionEvaluation> findAll() {
        return dao.findAll();
    }

    public Optional<QuestionEvaluation> findById(Integer id) {
        return dao.findById(id);
    }

    public QuestionEvaluation save(QuestionEvaluation q) {
        validator.validate(q);
        return dao.save(q);
    }

    public boolean existsById(Integer id) {
        return dao.existsById(id);
    }

    public void deleteById(Integer id) {
        dao.deleteById(id);
    }
}
