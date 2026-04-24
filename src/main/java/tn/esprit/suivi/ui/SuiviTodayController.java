package tn.esprit.suivi.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.suivi.dao.SuiviDAO;
import tn.esprit.suivi.model.QuestionEvaluation;
import tn.esprit.suivi.service.CheckInService;
import tn.esprit.user.entity.Utilisateur;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SuiviTodayController implements Initializable {

    @FXML private Label dateLabel;
    @FXML private VBox questionsContainer;
    @FXML private TextArea commentaireArea;
    @FXML private Label errorLabel;

    private final CheckInService service = new CheckInService();
    private final SuiviDAO suiviDAO = new SuiviDAO();
    private final Map<QuestionEvaluation, ToggleGroup> questionGroups = new LinkedHashMap<>();

    private int userId = -1;
    private int suiviId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (dateLabel != null) {
            dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")));
        }

        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            DialogHelper.showError("Session", "Utilisateur non connecte.");
            goToDashboard();
            return;
        }
        userId = user.getId();

        if (service.isAlreadySubmitted(userId, LocalDate.now())) {
            Platform.runLater(() -> {
                DialogHelper.showError("Check-in deja soumis", "Vous avez deja soumis le check-in du jour.");
                goToWeeklyInsights();
            });
            return;
        }

        List<QuestionEvaluation> questions = service.loadQuestionsForUser(userId);

        LocalDate today = LocalDate.now();
        try {
            suiviId = suiviDAO.findOrCreateToday(userId, today);
        } catch (SQLException e) {
            DialogHelper.showError("Check-in", "Impossible d'initialiser le suivi du jour: " + e.getMessage());
            goToDashboard();
            return;
        }

        Map<Integer, String> existingAnswers = suiviId > 0 ? suiviDAO.getAnswersBySuiviId(suiviId) : Map.of();
        buildQuestionCards(questions, existingAnswers);
    }

    private void buildQuestionCards(List<QuestionEvaluation> questions, Map<Integer, String> existingAnswers) {
        if (questionsContainer == null) return;

        questionsContainer.getChildren().clear();
        questionGroups.clear();

        for (QuestionEvaluation q : questions) {
            VBox card = new VBox(8);
            card.getStyleClass().add("question-card");

            Label category = new Label(safe(q.getCategory()));
            category.getStyleClass().add("category-badge");

            Label text = new Label(safe(q.getTexte()));
            text.setWrapText(true);
            text.getStyleClass().add("question-text");

            ToggleGroup group = new ToggleGroup();
            RadioButton rb1 = buildOption(safe(q.getOption1()), group);
            RadioButton rb2 = buildOption(safe(q.getOption2()), group);
            RadioButton rb3 = buildOption(safe(q.getOption3()), group);

            card.getChildren().addAll(category, text, rb1, rb2, rb3);
            questionsContainer.getChildren().add(card);
            questionGroups.put(q, group);

            String existing = existingAnswers == null ? null : existingAnswers.get(q.getId());
            if (existing != null) {
                if (existing.equals(rb1.getText())) {
                    group.selectToggle(rb1);
                } else if (existing.equals(rb2.getText())) {
                    group.selectToggle(rb2);
                } else if (existing.equals(rb3.getText())) {
                    group.selectToggle(rb3);
                }
            }
        }
    }

    private RadioButton buildOption(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.getStyleClass().add("option-radio");
        return rb;
    }

    @FXML
    private void handleSubmit() {
        if (errorLabel != null) errorLabel.setText("");

        String commentaire = commentaireArea == null || commentaireArea.getText() == null
                ? ""
                : commentaireArea.getText().trim();

        if (commentaire.length() > 1000) {
            if (errorLabel != null) {
                errorLabel.setText("Le commentaire ne doit pas depasser 1000 caracteres.");
            }
            return;
        }

        Map<Integer, String> answers = new HashMap<>();
        for (Map.Entry<QuestionEvaluation, ToggleGroup> entry : questionGroups.entrySet()) {
            Toggle selected = entry.getValue().getSelectedToggle();
            if (!(selected instanceof RadioButton rb)) {
                if (errorLabel != null) {
                    errorLabel.setText("Veuillez selectionner une reponse pour chaque question.");
                }
                return;
            }

            String value = rb.getText();
            QuestionEvaluation q = entry.getKey();

            boolean allowed = value.equals(safe(q.getOption1()))
                    || value.equals(safe(q.getOption2()))
                    || value.equals(safe(q.getOption3()));

            if (!allowed) {
                if (errorLabel != null) {
                    errorLabel.setText("Une reponse invalide a ete detectee.");
                }
                return;
            }

            answers.put(q.getId(), value);
        }

        try {
            service.submitCheckIn(userId, LocalDate.now(), commentaire, answers);
            DialogHelper.showSuccess("Check-in enregistre", "Votre suivi quotidien a ete enregistre avec succes.");
            goToWeeklyInsights();
        } catch (IllegalStateException duplicate) {
            DialogHelper.showError("Check-in deja soumis", "Vous avez deja soumis le check-in du jour.");
            goToWeeklyInsights();
        } catch (IllegalArgumentException e) {
            if (errorLabel != null) {
                errorLabel.setText(e.getMessage());
            } else {
                DialogHelper.showError("Erreur", e.getMessage());
            }
        } catch (Exception e) {
            DialogHelper.showError("Erreur", "Impossible d'enregistrer le check-in: " + e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        goToDashboard();
    }

    private void goToWeeklyInsights() {
        try {
            SceneManager.switchTo("weekly-insight");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void goToDashboard() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}