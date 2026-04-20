package tn.esprit.profil.ui;

import tn.esprit.profil.service.ProfilPsychologiqueService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test psychologique multi-pages (échelle de Likert). Les tableaux de questions et la logique
 * de score sont regroupés ici de façon explicite.
 */
public class TestController {

    private static final String[] SECTION_TITLES = {
            "Humeur et régulation émotionnelle",
            "Charge mentale, stress et concentration",
            "Sommeil, lien social et ressources personnelles"
    };

    /**
     * Formulations où un accord plus fort indique davantage de symptômes / détresse (1 = pas du tout … 5 = tout le temps).
     */
    private static final String[][] QUESTIONS = {
            {
                    "Je ressens souvent nervosité ou tension interne.",
                    "J'ai du mal à me détendre même en dehors des obligations.",
                    "Mes pensées sont envahissantes ou en boucle (rumination).",
                    "Je me sens triste ou découragé(e) plusieurs fois par semaine."
            },
            {
                    "Je me sens dépassé(e) par mes responsabilités.",
                    "J'ai des difficultés de concentration prolongées.",
                    "L'anxiété perturbe mes activités quotidiennes.",
                    "Je ressens une fatigue mentale importante en fin de journée."
            },
            {
                    "Mon sommeil est fragmenté ou insuffisant.",
                    "Je me sens isolé(e) ou peu compris(e) par mon entourage.",
                    "J'ai du mal à accorder du temps à mes besoins personnels.",
                    "Je me sens peu soutenu(e) dans les moments difficiles."
            }
    };

    private static final String[] LIKERT_LABELS = {
            "Pas du tout", "Un peu", "Modérément", "Beaucoup", "Tout le temps"
    };

    @FXML private Label sectionLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label pageLabel;
    @FXML private VBox questionsContainer;
    @FXML private javafx.scene.control.Button prevBtn;
    @FXML private javafx.scene.control.Button nextBtn;

    private int currentPage;
    private final List<int[]> answersPerPage = new ArrayList<>();

    @FXML
    private void initialize() {
        for (int p = 0; p < QUESTIONS.length; p++) {
            answersPerPage.add(new int[QUESTIONS[p].length]);
            for (int q = 0; q < QUESTIONS[p].length; q++) {
                answersPerPage.get(p)[q] = -1;
            }
        }
        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        sectionLabel.setText(SECTION_TITLES[currentPage]);
        progressBar.setProgress((double) (currentPage + 1) / QUESTIONS.length);
        pageLabel.setText("Section " + (currentPage + 1) + " / " + QUESTIONS.length);

        questionsContainer.getChildren().clear();

        String[] pageQuestions = QUESTIONS[currentPage];
        int[] pageAnswers = answersPerPage.get(currentPage);

        for (int i = 0; i < pageQuestions.length; i++) {
            int qIndex = i;
            int globalIndex = currentPage * 4 + i + 1;

            // ── Question card ──────────────────────────────────
            VBox card = new VBox(14);
            card.getStyleClass().add("question-card");
            card.setPadding(new Insets(20, 24, 20, 24));

            // Question number badge + text row
            HBox headerRow = new HBox(12);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            Label numBadge = new Label("Q" + globalIndex);
            numBadge.getStyleClass().add("question-number");

            Label qLabel = new Label(pageQuestions[i]);
            qLabel.getStyleClass().add("question-text");
            qLabel.setWrapText(true);
            qLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(qLabel, javafx.scene.layout.Priority.ALWAYS);

            headerRow.getChildren().addAll(numBadge, qLabel);

            // ── Likert scale ──────────────────────────────────
            ToggleGroup group = new ToggleGroup();

            HBox scaleRow = new HBox(0);
            scaleRow.setAlignment(Pos.CENTER);
            scaleRow.setPadding(new Insets(8, 0, 0, 0));

            for (int v = 1; v <= 5; v++) {
                int value = v;

                VBox option = new VBox(6);
                option.setAlignment(Pos.CENTER);
                option.setPrefWidth(120);

                RadioButton rb = new RadioButton();
                rb.setToggleGroup(group);
                rb.setText("");
                rb.setStyle(
                        "-fx-padding: 0;" +
                                "-fx-cursor: hand;"
                );
                if (pageAnswers[qIndex] == value) {
                    rb.setSelected(true);
                }
                rb.selectedProperty().addListener((obs, o, n) -> {
                    if (n) pageAnswers[qIndex] = value;
                });

                Label optLabel = new Label(LIKERT_LABELS[v - 1]);
                optLabel.setStyle(
                        "-fx-font-size: 11px;" +
                                "-fx-text-fill: #8FB3E2;" +
                                "-fx-text-alignment: center;" +
                                "-fx-wrap-text: true;" +
                                "-fx-max-width: 90px;"
                );
                optLabel.setAlignment(Pos.CENTER);
                optLabel.setWrapText(true);

                option.getChildren().addAll(rb, optLabel);
                scaleRow.getChildren().add(option);
            }

            card.getChildren().addAll(headerRow, scaleRow);
            questionsContainer.getChildren().add(card);
        }

        prevBtn.setDisable(currentPage == 0);
        boolean last = currentPage == QUESTIONS.length - 1;
        nextBtn.setText(last ? "Terminer ✓" : "Suivant →");
    }

    @FXML
    private void handlePrev() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNext() {
        if (!validateCurrentPage()) {
            DialogHelper.showError("Réponses manquantes", "Veuillez répondre à toutes les questions de cette section.");
            return;
        }
        if (currentPage < QUESTIONS.length - 1) {
            currentPage++;
            renderPage();
        } else {
            submitTest();
        }
    }

    private boolean validateCurrentPage() {
        int[] pageAnswers = answersPerPage.get(currentPage);
        for (int v : pageAnswers) {
            if (v < 1 || v > 5) {
                return false;
            }
        }
        return true;
    }

    private void submitTest() {
        int sum = 0;
        int count = 0;
        for (int p = 0; p < QUESTIONS.length; p++) {
            int[] pa = answersPerPage.get(p);
            for (int v : pa) {
                sum += v;
                count++;
            }
        }
        int scoreGlobal = computeScoreGlobal(sum, count);
        String profilType = getProfilType(scoreGlobal);
        String aiFeedback = generateFeedback(scoreGlobal);

        try {
            ProfilPsychologiqueService service = new ProfilPsychologiqueService();
            Integer userId = SessionManager.getInstance().getCurrentUser().getId();
            if (userId == null) {
                DialogHelper.showError("Session", "Utilisateur non identifié.");
                return;
            }
            service.submitProfil(userId, scoreGlobal, profilType, aiFeedback);
            SceneManager.switchTo("profil-psychologique");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        } catch (RuntimeException e) {
            DialogHelper.showError("Enregistrement", e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
        }
    }

    /**
     * Somme des réponses sur l'échelle 1–5 ; normalisation sur 0–100 (plus le score est élevé, plus la détresse est forte).
     */
    private static int computeScoreGlobal(int sum, int count) {
        if (count == 0) {
            return 0;
        }
        int minSum = count;
        int maxSum = 5 * count;
        double ratio = (sum - minSum) / (double) (maxSum - minSum);
        int score = (int) Math.round(ratio * 100.0);
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Libellé court du profil (cohérent avec les seuils d'affichage du résultat).
     */
    private static String getProfilType(int scoreGlobal) {
        if (scoreGlobal <= 25) {
            return "Profil résilient";
        }
        if (scoreGlobal <= 50) {
            return "Profil stable";
        }
        if (scoreGlobal <= 75) {
            return "Profil sensible";
        }
        return "Profil à risque élevé";
    }

    private static String generateFeedback(int scoreGlobal) {
        if (scoreGlobal <= 25) {
            return "Vos réponses suggèrent un bon équilibre psychologique global. Vous semblez disposer "
                    + "de ressources pour gérer le stress et conserver une vision positive. Continuez à entretenir "
                    + "des habitudes de sommeil, d'activité et de lien social qui vous font du bien.";
        }
        if (scoreGlobal <= 50) {
            return "Vos réponses indiquent un équilibre modéré avec quelques tensions ou fatigues ponctuelles. "
                    + "Il peut être utile de planifier des pauses régulières, de structurer vos journées et de "
                    + "parler à une personne de confiance si certaines difficultés persistent.";
        }
        if (scoreGlobal <= 75) {
            return "Les résultats traduisent une charge émotionnelle ou cognitive plus marquée. Il serait pertinent "
                    + "de renforcer les stratégies de récupération (sommeil, activité physique légère, délégation) "
                    + "et d'envisager un accompagnement professionnel si vous vous sentez dépassé(e).";
        }
        return "Vos réponses signalent un niveau de détresse élevé. Ce type de bilan ne remplace pas un diagnostic "
                + "médical : nous vous encourageons vivement à consulter un professionnel de santé ou un psychologue "
                + "afin d'obtenir une évaluation personnalisée et un soutien adapté.";
    }
    @FXML
    private void handleRetour() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
