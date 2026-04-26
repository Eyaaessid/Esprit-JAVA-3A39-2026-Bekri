package tn.esprit.suivi.ui;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.suivi.dao.WeeklyInsightDAO;
import tn.esprit.suivi.model.CategorySummary;
import tn.esprit.suivi.model.WeeklyInsightResult;
import tn.esprit.user.entity.Utilisateur;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class WeeklyInsightController implements Initializable {

    private static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY");
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private ScrollPane rootScroll;

    @FXML private Label periodLabel;
    @FXML private Label daysFilledLabel;
    @FXML private Label globalAvgLabel;
    @FXML private Label bestDayLabel;
    @FXML private Label worstDayLabel;
    @FXML private Label bestHighlightLabel;
    @FXML private Label worstHighlightLabel;
    @FXML private Label bestHighlightLabel2;
    @FXML private Label worstHighlightLabel2;
    @FXML private Label encouragementLabel;

    @FXML private VBox  emptyStateBox;
    @FXML private VBox  contentSections;

    @FXML private PieChart                globalPieChart;
    @FXML private BarChart<String,Number> categoryBarChart;
    @FXML private LineChart<String,Number> humeurLineChart;

    @FXML private HBox  recommendationBox;
    @FXML private Label donutCenterLabel;
    @FXML private Label humeurTrendBadge;
    @FXML private Label humeurTrendBadgeAlt;
    @FXML private Label aiStatusLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private final WeeklyInsightDAO dao = new WeeklyInsightDAO();
    private WeeklyInsightResult lastResult;
    private LocalDate lastStart;
    private LocalDate lastEnd;

    // ── Colours matching the app palette ──────────────────────────────────────
    private static final Color TEAL        = new Color(61,  107, 125);   // #3D6B7D
    private static final Color TEAL_LIGHT  = new Color(210, 229, 234);   // light teal bg
    private static final Color OLIVE       = new Color(173, 191, 78);    // #ADBF4E sidebar green
    private static final Color AMBER       = new Color(201, 154, 81);    // #C99A51
    private static final Color RED_SOFT    = new Color(197, 110, 90);    // #C56E5A
    private static final Color TEXT_DARK   = new Color(30,  58,  95);    // #1e3a5f
    private static final Color TEXT_GREY   = new Color(100, 116, 139);   // slate-500
    private static final Color BG_LIGHT    = new Color(246, 248, 243);   // page bg
    private static final Color WHITE       = Color.WHITE;
    private static final Color DIVIDER     = new Color(217, 225, 209);

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            DialogHelper.showError("Session", "Utilisateur non connecté.");
            goToDashboard();
            return;
        }
        int userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        LocalDate end   = today;
        lastStart = start;
        lastEnd   = end;

        if (periodLabel != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            periodLabel.setText(start.format(fmt) + " — " + end.format(fmt));
        }

        WeeklyInsightResult result = dao.getWeeklyInsight(userId, start, end);
        lastResult = result;

        boolean empty = (result == null || result.getTotalSubmittedDays() == 0);
        showEmptyState(empty);

        int submittedDays = (result == null) ? 0 : result.getTotalSubmittedDays();
        if (daysFilledLabel != null) daysFilledLabel.setText(submittedDays + " / 7");

        List<CategorySummary> summaries = (result == null) ? List.of() : result.getCategorySummaries();
        double globalAvg = computeGlobalAvg(summaries);

        if (globalAvgLabel  != null) globalAvgLabel.setText(globalAvg + "%");
        if (donutCenterLabel != null) donutCenterLabel.setText(globalAvg + "%");

        populateDonutChart(globalAvg);
        populateCategoryChart(summaries);
        populateHumeurLineChart(result);

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");

        if (bestDayLabel != null)
            bestDayLabel.setText(result != null && result.getBestDay() != null
                    ? result.getBestDay().format(dayFmt) : "—");
        if (worstDayLabel != null)
            worstDayLabel.setText(result != null && result.getWorstDay() != null
                    ? result.getWorstDay().format(dayFmt) : "—");
        if (bestHighlightLabel != null)
            bestHighlightLabel.setText(result != null && result.getBestScore() != null
                    ? "Score : " + result.getBestScore() + "%" : "—");
        if (worstHighlightLabel != null)
            worstHighlightLabel.setText(result != null && result.getWorstScore() != null
                    ? "Score : " + result.getWorstScore() + "%" : "—");
        if (bestHighlightLabel2 != null)
            bestHighlightLabel2.setText(result != null && result.getBestDay() != null
                    ? "Le " + result.getBestDay().format(dayFmt) + " — votre score moyen était le plus élevé !"
                    : "Pas encore de données suffisantes.");
        if (worstHighlightLabel2 != null)
            worstHighlightLabel2.setText(result != null && result.getWorstDay() != null
                    ? "Le " + result.getWorstDay().format(dayFmt) + " — votre score moyen était le plus bas."
                    : "Pas encore de données suffisantes.");

        String trend = computeHumeurTrend(result);
        updateTrendBadge(trend);

        if (encouragementLabel != null) {
            if (empty) {
                encouragementLabel.setText(
                        "Vous n'avez pas encore rempli de check-in cette semaine. Commencez dès aujourd'hui !");
            } else if (globalAvg >= 66) {
                encouragementLabel.setText("Vous êtes en bonne forme cette semaine — bravo ! 🎉");
            } else if (globalAvg >= 40) {
                encouragementLabel.setText("Vous progressez doucement — chaque jour compte. 💪");
            } else {
                encouragementLabel.setText("C'est une semaine plus difficile, mais vous n'êtes pas seul(e). 🌱");
            }
        }

        if (!empty && GROQ_API_KEY != null && !GROQ_API_KEY.isBlank()) {
            if (aiStatusLabel != null) aiStatusLabel.setText("✨ Chargement des recommandations IA...");
            loadAIRecommendationsAsync(summaries, globalAvg, trend, submittedDays);
        } else {
            populateFallbackRecommendations(summaries);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LINE CHART
    // ═════════════════════════════════════════════════════════════════════════

    private void populateHumeurLineChart(WeeklyInsightResult result) {
        if (humeurLineChart == null) return;
        humeurLineChart.getData().clear();
        if (result == null) return;

        Map<LocalDate, Double> dailyScores = result.getDailyHumeurScores();
        if (dailyScores == null || dailyScores.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Humeur");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        dailyScores.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(e.getKey().format(fmt), e.getValue())));

        if (!series.getData().isEmpty()) humeurLineChart.getData().add(series);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TREND
    // ═════════════════════════════════════════════════════════════════════════

    private String computeHumeurTrend(WeeklyInsightResult result) {
        if (result == null) return "Declining";
        Map<LocalDate, Double> scores = result.getDailyHumeurScores();
        if (scores == null || scores.size() < 2) return "Declining";

        List<Double> vals = new ArrayList<>(scores.values());
        int mid   = vals.size() / 2;
        double first = vals.subList(0, mid).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double last  = vals.subList(mid, vals.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return last >= first ? "Improving" : "Declining";
    }

    private void updateTrendBadge(String trend) {
        for (Label badge : new Label[]{humeurTrendBadge, humeurTrendBadgeAlt}) {
            if (badge == null) continue;
            if ("Improving".equals(trend)) {
                badge.setText("↑ En amélioration");
                badge.getStyleClass().removeAll("sym-trend-down");
                badge.getStyleClass().add("sym-trend-up");
            } else {
                badge.setText("↓ En déclin");
                badge.getStyleClass().removeAll("sym-trend-up");
                badge.getStyleClass().add("sym-trend-down");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  AI RECOMMENDATIONS
    // ═════════════════════════════════════════════════════════════════════════

    private void loadAIRecommendationsAsync(List<CategorySummary> summaries,
                                            double globalAvg, String trend, int totalDays) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return fetchGroqRecommendations(summaries, globalAvg, trend, totalDays);
            }
        };

        task.setOnSucceeded(e -> {
            String json = task.getValue();
            Platform.runLater(() -> {
                if (aiStatusLabel != null) aiStatusLabel.setText("");
                renderAIRecommendations(json, summaries);
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (aiStatusLabel != null) aiStatusLabel.setText("");
            populateFallbackRecommendations(summaries);
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String fetchGroqRecommendations(List<CategorySummary> summaries,
                                            double globalAvg, String trend, int totalDays) throws Exception {
        StringBuilder lines = new StringBuilder();
        lines.append("Données utilisateur sur les 7 derniers jours (scores en %) :\n");
        lines.append("- Jours complétés : ").append(totalDays).append("/7\n");
        lines.append("- Score moyen global : ").append(globalAvg).append("%\n");
        lines.append("- Tendance humeur : ").append(trend).append("\n");
        for (CategorySummary s : summaries) {
            if (s.getAvgNumericScore() != null)
                lines.append("- ").append(s.getCategory())
                        .append(" : ").append(s.getAvgNumericScore()).append("%\n");
        }

        String prompt = lines +
                "\nGénère exactement 3 recommandations d'objectifs personnalisées basées sur ces données, en JSON uniquement.\n" +
                "Format attendu (tableau JSON, rien d'autre, pas de markdown) :\n" +
                "[\n" +
                "  {\n" +
                "    \"title\": \"Titre court de l'objectif\",\n" +
                "    \"description\": \"Description courte et bienveillante, max 2 phrases.\",\n" +
                "    \"icon\": \"💡\",\n" +
                "    \"score\": 52.7,\n" +
                "    \"category\": \"humeur\",\n" +
                "    \"suggestedObjectif\": \"Phrase d'objectif SMART prête à ajouter\"\n" +
                "  }\n" +
                "]\n" +
                "Règles :\n" +
                "- Réponds UNIQUEMENT avec le JSON valide, sans texte avant ou après, sans backticks\n" +
                "- Chaque suggestedObjectif doit être une phrase d'objectif SMART concrète\n" +
                "- Icônes emoji adaptées (🧘 🏃 💧 🥗 🌙 🧠 💪)\n" +
                "- Priorise les catégories avec les scores les plus bas\n" +
                "- Rédige en français";

        JSONObject body = new JSONObject();
        body.put("model",       "llama-3.1-8b-instant");
        body.put("temperature", 0.5);
        body.put("max_tokens",  800);
        body.put("stream",      false);

        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role",    "system");
        system.put("content", "Tu es un coach bien-être expert. Tu réponds UNIQUEMENT en JSON valide.");
        messages.put(system);
        JSONObject userMsg = new JSONObject();
        userMsg.put("role",    "user");
        userMsg.put("content", prompt);
        messages.put(userMsg);
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) return null;

        String content = new JSONObject(response.body())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        content = content.replaceAll("(?i)```json\\s*", "").replaceAll("```\\s*", "").trim();
        return content;
    }

    private void renderAIRecommendations(String json, List<CategorySummary> summaries) {
        if (recommendationBox == null) return;
        recommendationBox.getChildren().clear();

        try {
            JSONArray recs = new JSONArray(json);
            for (int i = 0; i < recs.length(); i++) {
                JSONObject rec     = recs.getJSONObject(i);
                String title       = rec.optString("title",            "Recommandation");
                String description = rec.optString("description",      "");
                String icon        = rec.optString("icon",             "💡");
                String objectif    = rec.optString("suggestedObjectif","");
                double score       = rec.optDouble("score",            -1);

                VBox card = buildAIRecommendationCard(icon, title, description, objectif, score);
                recommendationBox.getChildren().add(card);
            }
        } catch (Exception e) {
            populateFallbackRecommendations(summaries);
        }
    }

    private VBox buildAIRecommendationCard(String icon, String title,
                                           String description, String objectif,
                                           double score) {
        VBox card = new VBox(10);
        card.getStyleClass().add("ai-rec-card");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("ai-rec-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ai-rec-title");
        titleLabel.setWrapText(true);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("ai-rec-desc");
        descLabel.setWrapText(true);

        card.getChildren().addAll(iconLabel, titleLabel, descLabel);

        if (score >= 0) {
            String scoreClass = score >= 66 ? "ai-rec-score-good"
                    : score >= 40 ? "ai-rec-score-warn"
                      :               "ai-rec-score-bad";
            Label scoreLabel = new Label(String.format("Score actuel : %.1f%%", score));
            scoreLabel.getStyleClass().addAll("ai-rec-score", scoreClass);
            card.getChildren().add(scoreLabel);
        }

        if (objectif != null && !objectif.isBlank()) {
            Label objLabel = new Label("Objectif : " + objectif);
            objLabel.getStyleClass().add("ai-rec-objectif");
            objLabel.setWrapText(true);
            card.getChildren().add(objLabel);

            Button addBtn = new Button("+ Ajouter comme objectif");
            addBtn.getStyleClass().add("ai-rec-add-btn");
            final String objectifText = objectif;
            addBtn.setOnAction(e -> {
                ObjectifPreFill.setSuggestedTitle(objectifText);
                Stage stage = (Stage) addBtn.getScene().getWindow();
                loadView(stage, "/fxml/objectifs.fxml");
            });
            card.getChildren().add(addBtn);
        }

        return card;
    }

    private void populateFallbackRecommendations(List<CategorySummary> summaries) {
        if (recommendationBox == null) return;
        recommendationBox.getChildren().clear();
        if (summaries == null || summaries.isEmpty()) return;

        summaries.stream()
                .filter(s -> s.getAvgNumericScore() != null)
                .sorted((a, b) -> Double.compare(a.getAvgNumericScore(), b.getAvgNumericScore()))
                .limit(3)
                .forEach(s -> {
                    String category = (s.getCategory() == null || s.getCategory().isBlank())
                            ? "Autre" : s.getCategory();
                    String icon  = getCategoryIcon(category);
                    double score = s.getAvgNumericScore();
                    String cap   = category.substring(0, 1).toUpperCase() + category.substring(1);
                    String desc  = score < 40
                            ? "Cette catégorie nécessite votre attention. Fixez-vous un objectif concret !"
                            : "Continuez vos efforts dans cette catégorie pour progresser.";
                    String suggested = buildDefaultObjectif(category);

                    VBox card = buildAIRecommendationCard(icon, cap, desc, suggested, score);
                    recommendationBox.getChildren().add(card);
                });
    }

    private String buildDefaultObjectif(String category) {
        return switch (category.toLowerCase()) {
            case "humeur"      -> "Pratiquer 10 min de cohérence cardiaque chaque matin cette semaine";
            case "sommeil"     -> "Dormir 8h par nuit en éteignant les écrans 30 min avant de dormir";
            case "nutrition"   -> "Manger au moins 3 portions de légumes par jour pendant 5 jours";
            case "activite"    -> "Faire 30 min d'activité physique modérée au moins 4 fois cette semaine";
            case "hydratation" -> "Boire 2 litres d'eau par jour pendant toute la semaine";
            default            -> "Améliorer ma routine quotidienne dans cette catégorie";
        };
    }

    private String getCategoryIcon(String category) {
        return switch (category.toLowerCase()) {
            case "humeur"      -> "Humeur";
            case "sommeil"     -> "Sommeil";
            case "activite"    -> "Activite";
            case "nutrition"   -> "Nutrition";
            case "hydratation" -> "Hydratation";
            case "stress"      -> "Stress";
            case "poids"       -> "Poids";
            default            -> "Autre";
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CHART HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private double computeGlobalAvg(List<CategorySummary> summaries) {
        if (summaries == null || summaries.isEmpty()) return 0.0;
        double sum = 0; int count = 0;
        for (CategorySummary s : summaries) {
            if (s.getAvgNumericScore() == null) continue;
            sum += s.getAvgNumericScore(); count++;
        }
        if (count == 0) return 0.0;
        return Math.round((sum / count) * 10.0) / 10.0;
    }

    private void populateDonutChart(double globalAvg) {
        if (globalPieChart == null) return;
        globalPieChart.getData().clear();
        double filled    = Math.max(0, Math.min(100, globalAvg));
        double remaining = 100 - filled;
        PieChart.Data filledSlice    = new PieChart.Data("Score",   filled);
        PieChart.Data remainingSlice = new PieChart.Data("Restant", remaining);
        globalPieChart.getData().addAll(filledSlice, remainingSlice);
        globalPieChart.getData().forEach(d -> {
            if ("Score".equals(d.getName())) {
                String color = (globalAvg >= 66) ? "#3D6B7D"
                        : (globalAvg >= 40) ? "#C99A51"
                          :                     "#C56E5A";
                d.getNode().setStyle("-fx-pie-color: " + color + ";");
            } else {
                d.getNode().setStyle("-fx-pie-color: rgba(213,222,209,0.5);");
            }
        });
    }

    private void populateCategoryChart(List<CategorySummary> summaries) {
        if (categoryBarChart == null) return;
        categoryBarChart.getData().clear();
        if (summaries == null || summaries.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Moyenne");

        for (CategorySummary s : summaries) {
            if (s.getAvgNumericScore() == null) continue;
            String cat = (s.getCategory() == null || s.getCategory().isBlank()) ? "Autre" : s.getCategory();
            cat = cat.substring(0, 1).toUpperCase() + cat.substring(1);
            series.getData().add(new XYChart.Data<>(cat, s.getAvgNumericScore()));
        }

        if (!series.getData().isEmpty()) {
            categoryBarChart.getData().add(series);
            for (XYChart.Data<String, Number> d : series.getData()) {
                double val = d.getYValue().doubleValue();
                String color = val >= 66 ? "#3D6B7D" : val >= 40 ? "#C99A51" : "#C56E5A";
                if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: " + color + ";");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════════════════════════════════════════

    private void goToDashboard() {
        try {
            Stage stage = SceneManager.getPrimaryStage();
            URL fxmlUrl = getClass().getResource("/fxml/user-dashboard.fxml");
            if (fxmlUrl == null) { DialogHelper.showError("Navigation", "Dashboard FXML not found."); return; }
            Parent root = FXMLLoader.load(fxmlUrl);
            stage.setScene(new Scene(root)); stage.show();
        } catch (IOException e) { DialogHelper.showError("Navigation", e.getMessage()); }
    }

    private void showEmptyState(boolean empty) {
        if (emptyStateBox   != null) { emptyStateBox.setVisible(empty);    emptyStateBox.setManaged(empty); }
        if (contentSections != null) { contentSections.setVisible(!empty); contentSections.setManaged(!empty); }
    }

    @FXML private void handleAccueil(ActionEvent e)        { loadView(stageFrom(e), "/fxml/user-dashboard.fxml"); }
    @FXML private void handleObjectifs(ActionEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckIn(ActionEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlan(ActionEvent e)       { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsights(ActionEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleChatBot(ActionEvent e)        { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTest(ActionEvent e)           { loadView(stageFrom(e), "/fxml/test.fxml"); }
    @FXML private void handleProfilPsy(ActionEvent e)      { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfil(ActionEvent e)         { loadView(stageFrom(e), "/fxml/profile.fxml"); }
    @FXML private void handleLogout(ActionEvent e)         { SessionManager.getInstance().logout(); loadView(stageFrom(e), "/fxml/login.fxml"); }
    @FXML private void handleCheckIn(ActionEvent e)        { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }

    private Stage stageFrom(ActionEvent e) { return (Stage) ((Node) e.getSource()).getScene().getWindow(); }

    private void loadView(Stage stage, String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root)); stage.show();
        } catch (IOException e) { DialogHelper.showError("Navigation", e.getMessage()); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PDF EXPORT — beautiful version
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleExportPdf() {
        if (lastResult == null) { DialogHelper.showError("Export PDF", "Aucune donnée à exporter."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("bekri-resume-" + LocalDate.now() + ".pdf");
        File file = chooser.showSaveDialog(SceneManager.getPrimaryStage());
        if (file == null) return;
        try {
            exportToPdf(file, lastResult, lastStart, lastEnd);
            DialogHelper.showSuccess("Export PDF", "PDF enregistré : " + file.getAbsolutePath());
        } catch (Exception e) { DialogHelper.showError("Export PDF", "Erreur export : " + e.getMessage()); }
    }

    private void exportToPdf(File file, WeeklyInsightResult result,
                             LocalDate start, LocalDate end) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();
        PdfContentByte cb = writer.getDirectContent();

        DateTimeFormatter fmt    = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");

        List<CategorySummary> summaries = result.getCategorySummaries();
        double globalAvg = computeGlobalAvg(summaries);

        float pageW = PageSize.A4.getWidth();   // 595
        float pageH = PageSize.A4.getHeight();  // 842
        float margin = 40f;
        float contentW = pageW - margin * 2;    // 515

        // ── 1. HEADER BANNER ─────────────────────────────────────────────────
        float bannerH = 80f;
        float bannerY = pageH - margin - bannerH;

        // Teal background rectangle
        cb.setColorFill(TEAL);
        cb.rectangle(margin, bannerY, contentW, bannerH);
        cb.fill();

        // Accent left strip (olive/green)
        cb.setColorFill(OLIVE);
        cb.rectangle(margin, bannerY, 6, bannerH);
        cb.fill();

        // Title text
        cb.setColorFill(WHITE);
        cb.beginText();
        cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 20f);
        cb.setTextMatrix(margin + 18, bannerY + bannerH - 28);
        cb.showText("Bekri  —  Insights hebdomadaires");
        cb.endText();

        // Period text
        cb.setColorFill(new Color(210, 229, 234));
        cb.beginText();
        cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false), 11f);
        cb.setTextMatrix(margin + 18, bannerY + 14);
        cb.showText("Periode : " + start.format(fmt) + "  ->  " + end.format(fmt));
        cb.endText();

        float cursorY = bannerY - 20;

        // ── 2. STAT CARDS ROW ────────────────────────────────────────────────
        String bestDay   = result.getBestDay()   != null ? result.getBestDay().format(dayFmt)   : "—";
        String worstDay  = result.getWorstDay()  != null ? result.getWorstDay().format(dayFmt)  : "—";
        String bestScore = result.getBestScore() != null ? result.getBestScore() + "%" : "—";
        String worstScore= result.getWorstScore()!= null ? result.getWorstScore()+ "%" : "—";

        float cardH = 68f;
        float cardGap = 10f;
        float cardW = (contentW - cardGap * 3) / 4f;
        float cardY = cursorY - cardH;

        String[][] cards = {
                {"Jours completes", result.getTotalSubmittedDays() + " / 7", "Cette semaine"},
                {"Meilleur jour",   bestDay,  "Score : " + bestScore},
                {"Jour difficile",  worstDay, "Score : " + worstScore},
                {"Score global",    globalAvg + "%", "Toutes categories"}
        };
        Color[] cardColors = {TEAL, new Color(61,139,87), AMBER, TEXT_DARK};

        BaseFont bfBold   = BaseFont.createFont(BaseFont.HELVETICA_BOLD,   BaseFont.CP1252, false);
        BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA,        BaseFont.CP1252, false);

        for (int i = 0; i < 4; i++) {
            float cx = margin + i * (cardW + cardGap);

            // Card background
            cb.setColorFill(new Color(246, 248, 250));
            roundRect(cb, cx, cardY, cardW, cardH, 6);
            cb.fill();

            // Colored top border
            cb.setColorFill(cardColors[i]);
            cb.rectangle(cx, cardY + cardH - 4, cardW, 4);
            cb.fill();

            // Label (small, grey)
            cb.setColorFill(TEXT_GREY);
            cb.beginText();
            cb.setFontAndSize(bfNormal, 8f);
            cb.setTextMatrix(cx + 10, cardY + cardH - 18);
            cb.showText(cards[i][0].toUpperCase());
            cb.endText();

            // Value (big, dark)
            cb.setColorFill(cardColors[i]);
            cb.beginText();
            cb.setFontAndSize(bfBold, 18f);
            cb.setTextMatrix(cx + 10, cardY + cardH - 38);
            cb.showText(cards[i][1]);
            cb.endText();

            // Sub-label
            cb.setColorFill(TEXT_GREY);
            cb.beginText();
            cb.setFontAndSize(bfNormal, 8f);
            cb.setTextMatrix(cx + 10, cardY + 10);
            cb.showText(cards[i][2]);
            cb.endText();
        }

        cursorY = cardY - 24;

        // ── 3. SECTION TITLE — Scores par catégorie ──────────────────────────
        cb.setColorFill(TEXT_DARK);
        cb.beginText();
        cb.setFontAndSize(bfBold, 13f);
        cb.setTextMatrix(margin, cursorY);
        cb.showText("Scores par categorie");
        cb.endText();

        // Underline
        cb.setColorStroke(TEAL);
        cb.setLineWidth(2f);
        cb.moveTo(margin, cursorY - 3);
        cb.lineTo(margin + 160, cursorY - 3);
        cb.stroke();

        cursorY -= 16;

        // ── 4. CATEGORY BARS ─────────────────────────────────────────────────
        if (summaries != null && !summaries.isEmpty()) {
            float barAreaW  = contentW;
            float barH      = 22f;
            float barSpacing = 34f;
            float labelW    = 90f;
            float scoreW    = 45f;
            float trackW    = barAreaW - labelW - scoreW - 16;

            for (CategorySummary s : summaries) {
                if (s.getAvgNumericScore() == null) continue;
                if (cursorY < margin + barSpacing) break;  // safety

                String cat   = s.getCategory() == null ? "Autre" : s.getCategory();
                cat = cat.substring(0, 1).toUpperCase() + cat.substring(1);
                double val   = s.getAvgNumericScore();
                Color barColor = val >= 66 ? TEAL : val >= 40 ? AMBER : RED_SOFT;

                float rowY = cursorY - barH;

                // Category label
                cb.setColorFill(TEXT_DARK);
                cb.beginText();
                cb.setFontAndSize(bfNormal, 10f);
                cb.setTextMatrix(margin, rowY + 6);
                cb.showText(cat);
                cb.endText();

                // Track (grey background)
                float trackX = margin + labelW;
                cb.setColorFill(new Color(226, 232, 240));
                cb.rectangle(trackX, rowY + 4, trackW, 14);
                cb.fill();

                // Filled bar
                float fillW = (float)(val / 100.0 * trackW);
                cb.setColorFill(barColor);
                cb.rectangle(trackX, rowY + 4, fillW, 14);
                cb.fill();

                // Score text
                cb.setColorFill(barColor);
                cb.beginText();
                cb.setFontAndSize(bfBold, 10f);
                cb.setTextMatrix(trackX + trackW + 8, rowY + 6);
                cb.showText(val + "%");
                cb.endText();

                // Responses count
                cb.setColorFill(TEXT_GREY);
                cb.beginText();
                cb.setFontAndSize(bfNormal, 8f);
                cb.setTextMatrix(trackX + trackW + 8, rowY - 2);
                cb.showText(s.getCountAnswers() + " rep.");
                cb.endText();

                cursorY -= barSpacing;
            }
        }

        cursorY -= 12;

        // ── 5. SECTION TITLE — Objectifs recommandés ─────────────────────────
        if (cursorY > margin + 80) {
            cb.setColorFill(TEXT_DARK);
            cb.beginText();
            cb.setFontAndSize(bfBold, 13f);
            cb.setTextMatrix(margin, cursorY);
            cb.showText("Objectifs recommandes");
            cb.endText();

            cb.setColorStroke(OLIVE);
            cb.setLineWidth(2f);
            cb.moveTo(margin, cursorY - 3);
            cb.lineTo(margin + 160, cursorY - 3);
            cb.stroke();

            cursorY -= 18;

            // Show top 3 lowest-scoring categories as recommendation cards
            if (summaries != null) {
                List<CategorySummary> sorted = summaries.stream()
                        .filter(s -> s.getAvgNumericScore() != null)
                        .sorted((a, b) -> Double.compare(a.getAvgNumericScore(), b.getAvgNumericScore()))
                        .limit(3)
                        .toList();

                float recCardH = 52f;
                float recCardGap = 10f;
                float recCardW = (contentW - recCardGap * 2) / 3f;

                for (int i = 0; i < sorted.size(); i++) {
                    if (cursorY - recCardH < margin) break;
                    CategorySummary s = sorted.get(i);
                    float cx = margin + i * (recCardW + recCardGap);
                    float cy = cursorY - recCardH;

                    double val = s.getAvgNumericScore();
                    Color recColor = val >= 66 ? TEAL : val >= 40 ? AMBER : RED_SOFT;
                    String cat = s.getCategory() == null ? "Autre" : s.getCategory();
                    cat = cat.substring(0, 1).toUpperCase() + cat.substring(1);
                    String suggested = buildDefaultObjectif(s.getCategory() == null ? "" : s.getCategory());

                    // Card bg
                    cb.setColorFill(new Color(246, 248, 250));
                    roundRect(cb, cx, cy, recCardW, recCardH, 6);
                    cb.fill();

                    // Left color strip
                    cb.setColorFill(recColor);
                    cb.rectangle(cx, cy, 4, recCardH);
                    cb.fill();

                    // Category name
                    cb.setColorFill(recColor);
                    cb.beginText();
                    cb.setFontAndSize(bfBold, 11f);
                    cb.setTextMatrix(cx + 12, cy + recCardH - 18);
                    cb.showText(cat + "  —  " + val + "%");
                    cb.endText();

                    // Suggested objectif (clipped to card width)
                    String obj = suggested.length() > 52 ? suggested.substring(0, 49) + "..." : suggested;
                    cb.setColorFill(TEXT_GREY);
                    cb.beginText();
                    cb.setFontAndSize(bfNormal, 8f);
                    cb.setTextMatrix(cx + 12, cy + recCardH - 32);
                    cb.showText(obj);
                    cb.endText();

                    // "Continuez vos efforts" or "A ameliorer"
                    String tag = val < 40 ? "A ameliorer" : val < 66 ? "En progression" : "Bonne forme";
                    cb.setColorFill(recColor);
                    cb.beginText();
                    cb.setFontAndSize(bfNormal, 8f);
                    cb.setTextMatrix(cx + 12, cy + 10);
                    cb.showText(tag);
                    cb.endText();
                }
                cursorY -= recCardH + 20;
            }
        }

        // ── 6. FOOTER ────────────────────────────────────────────────────────
        cb.setColorFill(DIVIDER);
        cb.rectangle(margin, margin + 18, contentW, 1);
        cb.fill();

        cb.setColorFill(TEXT_GREY);
        cb.beginText();
        cb.setFontAndSize(bfNormal, 8f);
        cb.setTextMatrix(margin, margin + 6);
        cb.showText("Bekri Wellbeing  —  Rapport genere le " + LocalDate.now().format(fmt)
                + "  —  Toutes les donnees sont privees et confidentielles.");
        cb.endText();

        doc.close();
    }

    /**
     * Draws a rounded rectangle using Bezier curves (OpenPDF doesn't have a native roundRect).
     */
    private void roundRect(PdfContentByte cb, float x, float y, float w, float h, float r) {
        cb.moveTo(x + r, y);
        cb.lineTo(x + w - r, y);
        cb.curveTo(x + w, y, x + w, y, x + w, y + r);
        cb.lineTo(x + w, y + h - r);
        cb.curveTo(x + w, y + h, x + w, y + h, x + w - r, y + h);
        cb.lineTo(x + r, y + h);
        cb.curveTo(x, y + h, x, y + h, x, y + h - r);
        cb.lineTo(x, y + r);
        cb.curveTo(x, y, x, y, x + r, y);
        cb.closePath();
    }

    private String safe(String s) { return s == null ? "" : s; }
}