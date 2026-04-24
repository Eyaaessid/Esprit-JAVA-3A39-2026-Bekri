package tn.esprit.profil.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.profil.entity.ProfilPsychologique;
import tn.esprit.profil.service.GroqAiService;
import tn.esprit.profil.service.ProfilPsychologiqueService;
import tn.esprit.profil.service.WellbeingRecommendationService;
import tn.esprit.profil.service.WellbeingRecommendationService.WellbeingRecommendation;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.ObjectifContext;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.utils.AppConfig;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ProfilPsychologiqueController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label scoreLabel;
    @FXML private Label profilTypeLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label dateLabel;
    @FXML private Label badgeLabel;
    @FXML private VBox resultBox;
    @FXML private VBox emptyBox;
    @FXML private VBox recommendationsBox;
    @FXML private Button passerTestBtn;
    @FXML private Button repasserBtn;
    @FXML private Button regenerateInsightBtn;
    @FXML private VBox headerCard;
    @FXML private Pane arcPane;
    @FXML private VBox card1;
    @FXML private VBox card2;
    @FXML private VBox card3;
    @FXML private Label feedbackPart1;
    @FXML private Label feedbackPart2;
    @FXML private Label feedbackPart3;

    private final ProfilPsychologiqueService service = new ProfilPsychologiqueService();
    private ProfilPsychologique currentProfil;

    @FXML
    private void initialize() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            showEmpty();
            return;
        }
        Optional<ProfilPsychologique> opt = service.getProfilForUser(user.getId());
        if (opt.isPresent()) {
            showResult(opt.get());
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        emptyBox.setVisible(true);
        emptyBox.setManaged(true);
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        if (repasserBtn != null) {
            repasserBtn.setVisible(false);
            repasserBtn.setManaged(false);
        }
        if (passerTestBtn != null) {
            passerTestBtn.setVisible(true);
            passerTestBtn.setManaged(true);
        }
    }

    private void showResult(ProfilPsychologique p) {
        currentProfil = p;
        emptyBox.setVisible(false);
        emptyBox.setManaged(false);
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        if (passerTestBtn != null) {
            passerTestBtn.setVisible(false);
            passerTestBtn.setManaged(false);
        }
        if (repasserBtn != null) {
            repasserBtn.setVisible(true);
            repasserBtn.setManaged(true);
        }

        Integer sg = p.getScoreGlobal();
        int score = sg != null ? sg : 0;
        scoreLabel.setText("Score : " + score + " / 100");
        profilTypeLabel.setText(p.getProfilType() != null ? p.getProfilType() : "—");
        feedbackLabel.setText(p.getAiFeedback() != null ? p.getAiFeedback() : "");
        if (p.getDateEvaluation() != null) {
            dateLabel.setText(p.getDateEvaluation().format(DATE_FMT));
        } else {
            dateLabel.setText("—");
        }
        applyBadge(score);

        applyHeaderGradient(p.getProfilType());
        drawScoreArc(p.getScoreGlobal() != null ? p.getScoreGlobal() : 0);
        splitFeedbackIntoCards(p.getAiFeedback());
        animateCards();
        if (p.getDateEvaluation() != null) {
            dateLabel.setText("📅 Évaluation du " +
                    p.getDateEvaluation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")));
        }

        loadRecommendations(p);
        Platform.runLater(() -> {
            if (resultBox.getScene() == null || resultBox.getScene().getWindow() == null) {
                return;
            }
            Stage stage = (Stage) resultBox.getScene().getWindow();
            stage.setMaximized(false);
            stage.setWidth(880);
            stage.setHeight(700);
            stage.centerOnScreen();
        });
    }

    private void loadRecommendations(ProfilPsychologique p) {
        if (recommendationsBox == null || p == null) {
            return;
        }
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        String prenom = user != null ? user.getPrenom() : null;

        Task<List<WellbeingRecommendation>> task = new Task<>() {
            @Override
            protected List<WellbeingRecommendation> call() {
                WellbeingRecommendationService recommendationService = new WellbeingRecommendationService();
                return recommendationService.getRecommendations(
                        p.getScoreGlobal() != null ? p.getScoreGlobal() : 0,
                        p.getProfilType(),
                        prenom
                );
            }
        };

        task.setOnSucceeded(event -> {
            recommendationsBox.getChildren().clear();
            List<WellbeingRecommendation> recommendations = task.getValue();
            if (recommendations == null) {
                return;
            }
            for (WellbeingRecommendation rec : recommendations) {
                String circleColor = switch (rec.getType()) {
                    case "humeur" -> "#fef3c7";
                    case "sommeil" -> "#ede9fe";
                    case "activite" -> "#dcfce7";
                    case "nutrition" -> "#fce7f3";
                    case "hydratation" -> "#e0f2fe";
                    default -> "#f1f5f9";
                };
                StackPane iconCircle = new StackPane();
                iconCircle.setPrefSize(44, 44);
                iconCircle.setMaxSize(44, 44);
                iconCircle.setStyle("-fx-background-color: " + circleColor +
                        "; -fx-background-radius: 22px;");
                Label iconLbl = new Label(rec.getIcon());
                iconLbl.setStyle("-fx-font-size: 20px;");
                iconCircle.getChildren().add(iconLbl);

                Label nameLbl = new Label(rec.getLabel());
                nameLbl.getStyleClass().add("reco-label");
                Label reasonLbl = new Label(rec.getReason());
                reasonLbl.getStyleClass().add("reco-reason");
                reasonLbl.setWrapText(true);
                VBox textBox = new VBox(4, nameLbl, reasonLbl);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                Button addBtn = new Button("→ Ajouter");
                addBtn.getStyleClass().add("add-btn");
                addBtn.setOnAction(e -> {
                    ObjectifContext.setPendingType(rec.getType());
                    ObjectifContext.setPendingLabel(rec.getLabel());
                    try {
                        SceneManager.switchTo("objectif-form");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                HBox card = new HBox(14, iconCircle, textBox, addBtn);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 12px;" +
                                "-fx-border-radius: 12px;" +
                                "-fx-padding: 14px;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);");
                VBox.setMargin(card, new Insets(0, 0, 10, 0));
                recommendationsBox.getChildren().add(card);
            }
        });
        task.setOnFailed(event -> {
        });

        Thread thread = new Thread(task, "wellbeing-recommendations");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRegenerateInsight() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (currentProfil == null || user == null || user.getId() == null) {
            return;
        }

        Button actionButton = repasserBtn != null ? repasserBtn : regenerateInsightBtn;
        if (actionButton == null) {
            return;
        }

        actionButton.setDisable(true);
        actionButton.setText("Génération…");
        if (regenerateInsightBtn != null && regenerateInsightBtn != actionButton) {
            regenerateInsightBtn.setDisable(true);
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                int score = currentProfil.getScoreGlobal() != null ? currentProfil.getScoreGlobal() : 0;
                int inferred = (int) Math.round(score * 12.0 / 100.0);
                String apiKey = AppConfig.get("groq.api.key");
                GroqAiService ai = new GroqAiService(apiKey);
                return ai.generateEmotionalInsight(score, currentProfil.getProfilType(), inferred, inferred, inferred);
            }
        };

        task.setOnSucceeded(event -> {
            String feedback = task.getValue();
            if (feedback == null || feedback.isBlank()) {
                restoreRegenerateButton();
                return;
            }
            try {
                currentProfil = service.updateAiFeedback(user.getId(), feedback);
                String newFeedback = currentProfil.getAiFeedback() != null ? currentProfil.getAiFeedback() : feedback;
                feedbackLabel.setText(newFeedback);
                splitFeedbackIntoCards(newFeedback);
                animateCards();
            } catch (Exception e) {
                DialogHelper.showError("Analyse", e.getMessage());
            } finally {
                restoreRegenerateButton();
            }
        });
        task.setOnFailed(event -> restoreRegenerateButton());

        Thread thread = new Thread(task, "regenerate-psychological-insight");
        thread.setDaemon(true);
        thread.start();
    }

    private void restoreRegenerateButton() {
        if (repasserBtn != null) {
            repasserBtn.setDisable(false);
            repasserBtn.setText("Régénérer l'analyse ✨");
        }
        if (regenerateInsightBtn != null) {
            regenerateInsightBtn.setDisable(false);
        }
    }

    private void applyBadge(int scoreGlobal) {
        String text;
        String bg;
        if (scoreGlobal <= 25) {
            text = "Équilibre très bon";
            bg = "#22c55e";
        } else if (scoreGlobal <= 50) {
            text = "Équilibre modéré";
            bg = "#3b82f6";
        } else if (scoreGlobal <= 75) {
            text = "Vulnérabilité moyenne";
            bg = "#f97316";
        } else {
            text = "Risque élevé";
            bg = "#ef4444";
        }
        badgeLabel.setText(text);
        badgeLabel.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-padding: 8 16; "
                + "-fx-background-radius: 20; -fx-font-weight: bold;");
    }

    private void applyHeaderGradient(String profilType) {
        String gradient = switch (profilType) {
            case "Profil résilient" -> "linear-gradient(to bottom right, #16a34a, #4ade80)";
            case "Profil stable" -> "linear-gradient(to bottom right, #2563eb, #60a5fa)";
            case "Profil à risque élevé" -> "linear-gradient(to bottom right, #dc2626, #f87171)";
            default -> "linear-gradient(to bottom right, #ea580c, #fb923c)";
        };
        headerCard.setStyle("-fx-background-color: " + gradient + "; -fx-padding: 40px 30px 30px 30px;");

        String badgeColor = switch (profilType) {
            case "Profil résilient" -> "#dcfce7; -fx-text-fill: #16a34a;";
            case "Profil stable" -> "#dbeafe; -fx-text-fill: #2563eb;";
            case "Profil à risque élevé" -> "#fee2e2; -fx-text-fill: #dc2626;";
            default -> "#ffedd5; -fx-text-fill: #ea580c;";
        };
        badgeLabel.setStyle("-fx-background-color: " + badgeColor +
                " -fx-background-radius: 20px; -fx-padding: 4px 16px;" +
                " -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void drawScoreArc(int score) {
        arcPane.getChildren().clear();
        double size = 110;
        double cx = size / 2;
        double cy = size / 2;
        double r = 46;

        Circle bg = new Circle(cx, cy, r);
        bg.setFill(null);
        bg.setStroke(Color.web("#e5e7eb"));
        bg.setStrokeWidth(10);

        Arc arc = new Arc(cx, cy, r, r, 90, -(score / 100.0 * 360));
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStrokeWidth(10);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setStroke(Color.WHITE);
        arc.setOpacity(0.9);

        Label pct = new Label(score + "%");
        pct.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        pct.setLayoutX(cx - 22);
        pct.setLayoutY(cy - 14);

        arcPane.getChildren().addAll(bg, arc, pct);
        arcPane.setPrefSize(size, size);
        arcPane.setMaxSize(size, size);
    }

    private void splitFeedbackIntoCards(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            feedbackPart1.setText("");
            feedbackPart2.setText("");
            feedbackPart3.setText("");
            return;
        }

        String p1;
        String p2 = "";
        String p3 = "";

        String upper = feedback.toUpperCase();
        int i1 = findSection(upper, "RÉSUMÉ", "RESUME", "EMOTIONAL", "ÉMOTIONNEL");
        int i2 = findSection(upper, "INTERPRÉTATION", "INTERPRETATION");
        int i3 = findSection(upper, "CONSEILS", "POSTURE");

        if (i1 >= 0 && i2 > i1 && i3 > i2) {
            p1 = cleanSectionTitle(feedback.substring(skipToNewline(feedback, i1), i2).trim());
            p2 = cleanSectionTitle(feedback.substring(skipToNewline(feedback, i2), i3).trim());
            p3 = cleanSectionTitle(feedback.substring(skipToNewline(feedback, i3)).trim());
        } else {
            String[] parts = feedback.split("\\n\\n+", 3);
            p1 = parts.length > 0 ? parts[0].trim() : feedback;
            p2 = parts.length > 1 ? parts[1].trim() : "";
            p3 = parts.length > 2 ? parts[2].trim() : "";
        }

        feedbackPart1.setText(p1);
        feedbackPart2.setText(p2);
        feedbackPart3.setText(p3);
    }

    private int findSection(String upper, String... keywords) {
        for (String kw : keywords) {
            int idx = upper.indexOf(kw);
            if (idx >= 0) return idx;
        }
        return -1;
    }

    private int skipToNewline(String text, int from) {
        int nl = text.indexOf('\n', from);
        return nl >= 0 ? nl + 1 : from;
    }

    private String cleanSectionTitle(String text) {
        return text.replaceFirst(
                "(?i)^(RÉSUMÉ ÉMOTIONNEL|INTERPRÉTATION|CONSEILS DE POSTURE MENTALE)\\s*:?\\s*", ""
        ).trim();
    }

    private void animateCards() {
        int delay = 0;
        for (VBox card : new VBox[]{card1, card2, card3}) {
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(450), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            delay += 200;
        }
    }

    @FXML
    private void handleRetour() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRepasser() {
        goToTest();
    }

    @FXML
    private void handlePasserTest() {
        goToTest();
    }

    private void goToTest() {
        try {
            SceneManager.switchTo("test");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }
}
