package tn.esprit.pijava.config;

import tn.esprit.pijava.controller.ObjectifBienEtreController;
import tn.esprit.pijava.controller.QuestionEvaluationController;
import tn.esprit.pijava.repository.ObjectifBienEtreDao;
import tn.esprit.pijava.repository.QuestionEvaluationDao;
import tn.esprit.pijava.service.ObjectifBienEtreService;
import tn.esprit.pijava.service.QuestionEvaluationService;
import tn.esprit.pijava.utils.SimpleDataSource;
import tn.esprit.pijava.validation.ObjectifBienEtreValidator;
import tn.esprit.pijava.validation.QuestionEvaluationValidator;

public class AppContext {
    private final AppProperties properties;
    private final SimpleDataSource dataSource;
    private final ObjectifBienEtreService objectifService;
    private final QuestionEvaluationService questionService;
    private final ObjectifBienEtreController objectifController;
    private final QuestionEvaluationController questionController;

    public AppContext() {
        properties = new AppProperties();
        dataSource = new SimpleDataSource(
                properties.getString("db.url", ""),
                properties.getString("db.username", ""),
                properties.getString("db.password", "")
        );

        ObjectifBienEtreDao objectifDao = new ObjectifBienEtreDao(dataSource);
        QuestionEvaluationDao questionDao = new QuestionEvaluationDao(dataSource);

        objectifService = new ObjectifBienEtreService(objectifDao, new ObjectifBienEtreValidator());
        questionService = new QuestionEvaluationService(questionDao, new QuestionEvaluationValidator());

        objectifController = new ObjectifBienEtreController(objectifService);
        questionController = new QuestionEvaluationController(questionService);
    }

    public AppProperties getProperties() {
        return properties;
    }

    public ObjectifBienEtreController getObjectifController() {
        return objectifController;
    }

    public QuestionEvaluationController getQuestionController() {
        return questionController;
    }
}
