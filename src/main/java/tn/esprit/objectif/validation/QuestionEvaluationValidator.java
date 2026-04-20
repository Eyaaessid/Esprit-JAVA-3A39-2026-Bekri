package tn.esprit.objectif.validation;

import tn.esprit.objectif.entity.QuestionEvaluation;

public class QuestionEvaluationValidator {
    public void validate(QuestionEvaluation q) {
        if (q.getTexte() == null || q.getTexte().isBlank()) {
            throw new ValidationException("Le texte de la question est obligatoire.");
        }
        int textLength = q.getTexte().trim().length();
        if (textLength < 5 || textLength > 255) {
            throw new ValidationException("Le texte doit contenir entre 5 et 255 caractères.");
        }
        if (q.getCategory() == null || q.getCategory().isBlank()) {
            throw new ValidationException("La catégorie est obligatoire.");
        }
        if (q.getTypeReponse() == null || q.getTypeReponse().isBlank()) {
            throw new ValidationException("Le type de réponse est obligatoire.");
        }
        validateOption(q.getOption1(), "L'option 1 est obligatoire.", "L'option 1 ne peut pas dépasser 100 caractères.");
        validateOption(q.getOption2(), "L'option 2 est obligatoire.", "L'option 2 ne peut pas dépasser 100 caractères.");
        validateOption(q.getOption3(), "L'option 3 est obligatoire.", "L'option 3 ne peut pas dépasser 100 caractères.");
    }

    private void validateOption(String value, String requiredMessage, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(requiredMessage);
        }
        if (value.length() > 100) {
            throw new ValidationException(tooLongMessage);
        }
    }
}
