package com.bekri.controllers;

import com.bekri.services.ApiClient;
import com.bekri.utils.DialogHelper;
import com.bekri.utils.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Gère l'évaluation initiale : 20 questions sur 4 pages (5 par page).
 * Logique identique au InitialAssessmentController Symfony.
 * Score = (somme * 100) / 80  — même formule que le web.
 */
public class TestController implements Initializable {

    // ─── Questions (même ordre que Symfony) ──────────────────────────────────
    private static final String[] MENTAL_QUESTIONS = {
            "À quelle fréquence vous sentez-vous stressé(e) ?",
            "Vous sentez-vous anxieux(se) sans raison apparente ?",
            "Avez-vous des pensées négatives récurrentes ?",
            "Vous sentez-vous dépassé(e) par vos responsabilités ?",
            "Avez-vous des difficultés à vous concentrer ?",
            "Vous sentez-vous émotionnellement stable ?",
            "Vous ressentez-vous triste ou démotivé(e) ?",
            "Avez-vous des difficultés à gérer vos émotions ?",
            "Vous sentez-vous satisfait(e) de votre vie actuelle ?",
            "Vous arrive-t-il de vous sentir mentalement épuisé(e) ?"
    };

    private static final String[] PHYSICAL_QUESTIONS = {
            "Comment évaluez-vous la qualité de votre sommeil ?",
            "Vous sentez-vous fatigué(e) au réveil ?",
            "Faites-vous de l'activité physique régulièrement ?",
            "Avez-vous des douleurs physiques fréquentes ?",
            "Votre alimentation est-elle équilibrée ?",
            "Buvez-vous suffisamment d'eau chaque jour ?",
            "Ressentez-vous des tensions musculaires liées au stress ?",
            "Vous sentez-vous énergique durant la journée ?",
            "Prenez-vous du temps pour vous détendre physiquement ?",
            "Votre état de santé général vous semble-t-il bon ?"
    };

    private static final int TOTAL       = 20;
    private static final int PER_PAGE    = 5;
    private static final int TOTAL_PAGES = 4;
    private static final int MAX_RAW_SCORE = 80;

    // ─── FXML ────────────────────────────────────────────────────────────────
    @FXML private VBox       questionsContainer;
    @FXML private ProgressBar progressBar;
    @FXML private Label      pageLabel;
    @FXML private Label      sectionLabel;
    @FXML private Button     prevBtn;
    @FXML private Button     nextBtn;

    // ─── State ───────────────────────────────────────────────────────────────
    private final Integer[] answers = new Integer[TOTAL];
    private int currentPage = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        renderPage(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void renderPage(int page) {
        questionsContainer.getChildren().clear();
        int offset = page * PER_PAGE;

        // Section label
        sectionLabel.setText(offset < 10 ? "🧠 Section mentale" : "💪 Section physique");

        // Progress bar & page counter
        progressBar.setProgress((double) page / TOTAL_PAGES);
        pageLabel.setText("Page " + (page + 1) + " / " + TOTAL_PAGES);
        prevBtn.setDisable(page == 0);
        nextBtn.setText(page == TOTAL_PAGES - 1 ? "Terminer ✓" : "Suivant →");

        // ── Render questions ──────────────────────────────────────────────────
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = offset + i;
            if (idx >= TOTAL) break;

            String questionText = idx < 10
                    ? MENTAL_QUESTIONS[idx]
                    : PHYSICAL_QUESTIONS[idx - 10];

            // ── Question container (flat row with bottom divider) ─────────────
            VBox questionBox = new VBox(0);
            questionBox.getStyleClass().add("question-box");

            // Question label
            Label qLabel = new Label((idx + 1) + ". " + questionText);
            qLabel.getStyleClass().add("question-text");
            qLabel.setWrapText(true);
            qLabel.setMaxWidth(Double.MAX_VALUE);

            // ── Likert row: [Jamais] ○ ○ ○ ○ ○ [Très souvent] ───────────────
            HBox likertRow = new HBox();
            likertRow.getStyleClass().add("likert-row");
            likertRow.setAlignment(Pos.CENTER_LEFT);

            // Left anchor label
            Label leftLabel = new Label("Jamais");
            leftLabel.getStyleClass().add("likert-end-label");
            leftLabel.setMinWidth(60);

            // Right anchor label (added last, pushed right by spacer)
            Label rightLabel = new Label("Très souvent");
            rightLabel.getStyleClass().add("likert-end-label");
            rightLabel.setMinWidth(80);

            likertRow.getChildren().add(leftLabel);

            // Five radio buttons — plain circles, no text, evenly spaced
            ToggleGroup group = new ToggleGroup();
            for (int v = 0; v <= 4; v++) {
                final int value = v;

                RadioButton radio = new RadioButton();
                radio.setToggleGroup(group);
                radio.getStyleClass().add("likert-radio");
                radio.setText("");          // hide built-in text
                radio.setFocusTraversable(true);

                // Restore previously chosen answer
                if (answers[idx] != null && answers[idx] == value) {
                    radio.setSelected(true);
                }
                radio.setOnAction(e -> answers[idx] = value);

                // Spacer around each radio so they spread evenly
                Region spacerL = new Region();
                Region spacerR = new Region();
                spacerL.setMinWidth(18);
                spacerR.setMinWidth(18);
                HBox.setHgrow(spacerL, Priority.SOMETIMES);
                HBox.setHgrow(spacerR, Priority.SOMETIMES);

                likertRow.getChildren().addAll(spacerL, radio, spacerR);
            }

            likertRow.getChildren().add(rightLabel);

            questionBox.getChildren().addAll(qLabel, likertRow);
            questionsContainer.getChildren().add(questionBox);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void handleNext() {
        int offset = currentPage * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = offset + i;
            if (idx >= TOTAL) break;
            if (answers[idx] == null) {
                DialogHelper.showError("Attention",
                        "Veuillez répondre à toutes les questions avant de continuer.");
                return;
            }
        }

        if (currentPage < TOTAL_PAGES - 1) {
            currentPage++;
            renderPage(currentPage);
        } else {
            submitTest();
        }
    }

    @FXML
    private void handlePrev() {
        if (currentPage > 0) {
            currentPage--;
            renderPage(currentPage);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void submitTest() {
        nextBtn.setDisable(true);
        nextBtn.setText("Enregistrement...");

        int totalBrut = 0;
        for (int a : answers) totalBrut += a;
        int scoreGlobal = Math.max(0,
                Math.min(100, (int) Math.round((totalBrut * 100.0) / MAX_RAW_SCORE)));

        String profilType  = getProfilType(scoreGlobal);
        String aiFeedback  = generateFeedback(scoreGlobal, profilType);

        new Thread(() -> {
            try {
                ApiClient.submitProfil(scoreGlobal, profilType, aiFeedback);
                Platform.runLater(() -> {
                    try {
                        SceneManager.getInstance().switchTo("user-dashboard");
                    } catch (Exception e) {
                        DialogHelper.showError("Navigation", "Erreur de navigation.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    DialogHelper.showError("Erreur",
                            "Erreur lors de l'enregistrement : " + e.getMessage());
                    nextBtn.setDisable(false);
                    nextBtn.setText("Terminer ✓");
                });
            }
        }).start();
    }

    private String getProfilType(int score) {
        if (score <= 25) return "Équilibre très bon";
        if (score <= 50) return "Équilibre modéré";
        if (score <= 75) return "Vulnérabilité moyenne";
        return "Risque élevé";
    }

    private String generateFeedback(int score, String profil) {
        return switch (profil) {
            case "Équilibre très bon" ->
                    "Votre bilan révèle un excellent équilibre mental et physique. " +
                            "Continuez vos bonnes habitudes et maintenez ce niveau de bien-être. Score : " + score + "/100.";
            case "Équilibre modéré" ->
                    "Votre bilan montre un équilibre global satisfaisant avec quelques axes d'amélioration. " +
                            "Portez attention aux domaines où vous avez scoré plus haut. Score : " + score + "/100.";
            case "Vulnérabilité moyenne" ->
                    "Votre bilan indique quelques zones de vulnérabilité. " +
                            "Il serait bénéfique d'adopter des pratiques de gestion du stress et d'améliorer votre hygiène de vie. Score : " + score + "/100.";
            default ->
                    "Votre bilan révèle un niveau de stress et de vulnérabilité élevé. " +
                            "Nous vous recommandons vivement de consulter un professionnel de santé. Score : " + score + "/100.";
        };
    }
}