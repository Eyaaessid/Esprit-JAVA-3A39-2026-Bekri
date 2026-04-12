package tn.esprit.pijava.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pijava.entity.QuestionEvaluation;

public interface QuestionEvaluationRepository extends JpaRepository<QuestionEvaluation, Integer> {
}