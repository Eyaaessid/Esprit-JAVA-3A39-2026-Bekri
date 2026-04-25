package tn.esprit.suivi.ui;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class WeeklyInsightController implements Initializable {

    private static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY"); // set env var
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

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

    @FXML private VBox emptyStateBox;
    @FXML private VBox contentSections;

    @FXML private PieChart globalPieChart;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private LineChart<String, Number> humeurLineChart;

    @FXML private HBox recommendationBox;
    @FXML private Label donutCenterLabel;
    @FXML private Label humeurTrendBadge;
    @FXML private Label aiStatusLabel; // optional: shows "Chargement IA..." while fetching

    private final WeeklyInsightDAO dao = new WeeklyInsightDAO();

    private WeeklyInsightResult lastResult;
    private LocalDate lastStart;
    private LocalDate lastEnd;

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

        if (globalAvgLabel != null) globalAvgLabel.setText(globalAvg + "%");
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

        String trend = computeHumeurTrend(summaries);
        updateTrendBadge(trend);

        if (encouragementLabel != null) {
            if (empty) {
                encouragementLabel.setText("Vous n'avez pas encore rempli de check-in cette semaine. Commencez dès aujourd'hui !");
            } else if (globalAvg >= 66) {
                encouragementLabel.setText("Vous êtes en bonne forme cette semaine — bravo ! 🎉");
            } else if (globalAvg >= 40) {
                encouragementLabel.setText("Vous progressez doucement — chaque jour compte. 💪");
            } else {
                encouragementLabel.setText("C'est une semaine plus difficile, mais vous n'êtes pas seul(e). 🌱");
            }
        }

        // ── AI Recommendations (async so UI doesn't freeze) ─────────────
        if (!empty && GROQ_API_KEY != null && !GROQ_API_KEY.isBlank()) {
            if (aiStatusLabel != null) aiStatusLabel.setText("✨ Chargement des recommandations IA...");
            loadAIRecommendationsAsync(summaries, globalAvg, trend, submittedDays);
        } else {
            populateFallbackRecommendations(summaries);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  AI RECOMMENDATIONS via Groq (async)
    // ═══════════════════════════════════════════════════════════════

    private void loadAIRecommendationsAsync(List<CategorySummary> summaries, double globalAvg, String trend, int totalDays) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
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

    private String fetchGroqRecommendations(List<CategorySummary> summaries, double globalAvg, String trend, int totalDays) throws Exception {
        // Build data summary matching Symfony's prompt
        StringBuilder lines = new StringBuilder();
        lines.append("Données utilisateur sur les 7 derniers jours (scores en %) :\n");
        lines.append("- Jours complétés : ").append(totalDays).append("/7\n");
        lines.append("- Score moyen global : ").append(globalAvg).append("%\n");
        lines.append("- Tendance humeur : ").append(trend).append("\n");

        for (CategorySummary s : summaries) {
            if (s.getAvgNumericScore() != null) {
                lines.append("- ").append(s.getCategory()).append(" : ").append(s.getAvgNumericScore()).append("%\n");
            }
        }

        String dataSummary = lines.toString();

        String prompt = dataSummary + "\n" +
                "Génère exactement 3 recommandations personnalisées basées sur ces données, en JSON uniquement.\n" +
                "Format attendu (tableau JSON, rien d'autre, pas de markdown) :\n" +
                "[\n" +
                "  {\n" +
                "    \"title\": \"...\",\n" +
                "    \"description\": \"...\",\n" +
                "    \"icon\": \"💡\",\n" +
                "    \"score\": 52.7,\n" +
                "    \"category\": \"...\",\n" +
                "    \"suggestedObjectif\": \"...\"\n" +
                "  }\n" +
                "]\n" +
                "Règles :\n" +
                "- Réponds UNIQUEMENT avec le JSON valide, sans texte avant ou après, sans backticks\n" +
                "- Chaque description : concrète, bienveillante, max 2 phrases\n" +
                "- Icônes emoji adaptées (ex: 🧘, 🏃, 💧, 🥗, 🌙, 🧠, 💪)\n" +
                "- Priorise les catégories avec les scores les plus bas\n" +
                "- Rédige en français";

        JSONObject body = new JSONObject();
        body.put("model", "llama-3.1-8b-instant");
        body.put("temperature", 0.5);
        body.put("max_tokens", 700);
        body.put("stream", false);

        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", "Tu es un coach bien-être expert. Tu analyses des données de santé et génères des recommandations personnalisées. Tu réponds UNIQUEMENT en JSON valide, jamais en texte libre, jamais en markdown.");
        messages.put(system);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.put(userMsg);

        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) return null;

        JSONObject resp = new JSONObject(response.body());
        String content = resp.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // Strip markdown fences if any
        content = content.replaceAll("(?i)```json\\s*", "").replaceAll("```\\s*", "").trim();
        return content;
    }

    private void renderAIRecommendations(String json, List<CategorySummary> summaries) {
        if (recommendationBox == null) return;
        recommendationBox.getChildren().clear();

        try {
            JSONArray recs = new JSONArray(json);
            for (int i = 0; i < recs.length(); i++) {
                JSONObject rec = recs.getJSONObject(i);
                String title = rec.optString("title", "Recommandation");
                String description = rec.optString("description", "");
                String icon = rec.optString("icon", "💡");
                String objectif = rec.optString("suggestedObjectif", "");
                double score = rec.optDouble("score", -1);

                VBox card = buildAIRecommendationCard(icon, title, description, objectif, score);
                recommendationBox.getChildren().add(card);
            }
        } catch (Exception e) {
            populateFallbackRecommendations(summaries);
        }
    }

    private VBox buildAIRecommendationCard(String icon, String title, String description, String objectif, double score) {
        VBox card = new VBox(10);
        card.getStyleClass().add("ai-rec-card");
        card.setStyle(
                "-fx-background-color: linear-gradient(135deg, #1e293b, #0f172a);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(20,184,166,0.3);" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4);"
        );

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #e2e8f0;");
        titleLabel.setWrapText(true);

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-line-spacing: 2;");
        descLabel.setWrapText(true);

        if (score >= 0) {
            String scoreColor = score >= 66 ? "#14b8a6" : score >= 40 ? "#f59e0b" : "#ef4444";
            Label scoreLabel = new Label(String.format("Score actuel : %.1f%%", score));
            scoreLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + scoreColor + "; -fx-font-weight: 600;");
            card.getChildren().addAll(iconLabel, titleLabel, descLabel, scoreLabel);
        } else {
            card.getChildren().addAll(iconLabel, titleLabel, descLabel);
        }

        if (!objectif.isBlank()) {
            Label objLabel = new Label("🎯 " + objectif);
            objLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #14b8a6; -fx-font-weight: 700; -fx-cursor: hand;");
            objLabel.setWrapText(true);
            card.getChildren().add(objLabel);
        }

        return card;
    }

    private void populateFallbackRecommendations(List<CategorySummary> summaries) {
        if (recommendationBox == null) return;
        recommendationBox.getChildren().clear();
        if (summaries == null || summaries.isEmpty()) return;

        for (CategorySummary s : summaries) {
            String category = (s.getCategory() == null || s.getCategory().isBlank()) ? "Autre" : s.getCategory();
            String icon = getCategoryIcon(category);
            String avg = s.getAvgNumericScore() == null ? "—" : s.getAvgNumericScore() + "%";
            double score = s.getAvgNumericScore() == null ? -1 : s.getAvgNumericScore();
            String desc = "Réponses : " + s.getCountAnswers() + "  |  Moyenne : " + avg;
            String cap = category.substring(0, 1).toUpperCase() + category.substring(1);

            VBox card = buildAIRecommendationCard(icon, cap, desc, "Ajouter comme objectif", score);
            recommendationBox.getChildren().add(card);
        }
    }

    private String getCategoryIcon(String category) {
        return switch (category.toLowerCase()) {
            case "humeur" -> "😊";
            case "sommeil" -> "🌙";
            case "activite" -> "🏃";
            case "nutrition" -> "🥗";
            case "hydratation" -> "💧";
            case "stress" -> "🧠";
            case "poids" -> "⚖️";
            default -> "💡";
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  CHART HELPERS
    // ═══════════════════════════════════════════════════════════════

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
        double filled = Math.max(0, Math.min(100, globalAvg));
        double remaining = 100 - filled;
        PieChart.Data filledSlice = new PieChart.Data("Score", filled);
        PieChart.Data remainingSlice = new PieChart.Data("Restant", remaining);
        globalPieChart.getData().addAll(filledSlice, remainingSlice);
        globalPieChart.getData().forEach(d -> {
            if (d.getName().equals("Score")) {
                String color = (globalAvg >= 66) ? "#14b8a6" : (globalAvg >= 40) ? "#f59e0b" : "#ef4444";
                d.getNode().setStyle("-fx-pie-color: " + color + ";");
            } else {
                d.getNode().setStyle("-fx-pie-color: rgba(30,41,59,0.6);");
            }
        });
    }

    private String computeHumeurTrend(List<CategorySummary> summaries) {
        if (summaries == null) return "Declining";
        for (CategorySummary s : summaries) {
            if ("humeur".equalsIgnoreCase(s.getCategory()) && s.getAvgNumericScore() != null) {
                return s.getAvgNumericScore() >= 66 ? "Improving" : "Declining";
            }
        }
        return "Declining";
    }

    private void updateTrendBadge(String trend) {
        if (humeurTrendBadge == null) return;
        if ("Improving".equals(trend)) {
            humeurTrendBadge.setText("↑ En amélioration");
            humeurTrendBadge.getStyleClass().removeAll("sym-trend-down");
            humeurTrendBadge.getStyleClass().add("sym-trend-up");
        } else {
            humeurTrendBadge.setText("↓ En déclin");
            humeurTrendBadge.getStyleClass().removeAll("sym-trend-up");
            humeurTrendBadge.getStyleClass().add("sym-trend-down");
        }
    }

    /**
     * Populates humeur line chart from getDailyHumeurScores() — requires DAO to fill it.
     */
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
                .forEach(entry -> series.getData().add(
                        new XYChart.Data<>(entry.getKey().format(fmt), entry.getValue())
                ));

        if (!series.getData().isEmpty()) humeurLineChart.getData().add(series);
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
                String color = val >= 66 ? "#14b8a6" : val >= 40 ? "#f59e0b" : "#ef4444";
                if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: " + color + ";");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════════

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
        if (emptyStateBox != null) { emptyStateBox.setVisible(empty); emptyStateBox.setManaged(empty); }
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
    @FXML private void handleLogout(ActionEvent e) { SessionManager.getInstance().logout(); loadView(stageFrom(e), "/fxml/login.fxml"); }
    @FXML private void handleCheckIn(ActionEvent e) { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }

    private Stage stageFrom(ActionEvent e) { return (Stage) ((Node) e.getSource()).getScene().getWindow(); }

    private void loadView(Stage stage, String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root)); stage.show();
        } catch (IOException e) { DialogHelper.showError("Navigation", e.getMessage()); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PDF EXPORT
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void handleExportPdf() {
        if (lastResult == null) { DialogHelper.showError("Export PDF", "Aucune donnée à exporter."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("bekri-resume-" + LocalDate.now() + ".pdf");
        Stage stage = SceneManager.getPrimaryStage();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            exportToPdf(file, lastResult, lastStart, lastEnd);
            DialogHelper.showSuccess("Export PDF", "PDF enregistré : " + file.getAbsolutePath());
        } catch (Exception e) { DialogHelper.showError("Export PDF", "Erreur export : " + e.getMessage()); }
    }

    private void exportToPdf(File file, WeeklyInsightResult result, LocalDate start, LocalDate end) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font hFont = new Font(Font.HELVETICA, 13, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        document.add(new Paragraph("Bekri — Insights hebdomadaires", titleFont));
        document.add(new Paragraph("Période : " + start.format(fmt) + " → " + end.format(fmt), normal));
        document.add(new Paragraph(" ", normal));
        double globalAvg = computeGlobalAvg(result.getCategorySummaries());
        document.add(new Paragraph("Résumé", hFont));
        document.add(new Paragraph("- Jours complétés : " + result.getTotalSubmittedDays() + "/7", normal));
        document.add(new Paragraph("- Score moyen global : " + globalAvg + "%", normal));
        document.add(new Paragraph("- Meilleur jour : " + safe(bestDayLabel != null ? bestDayLabel.getText() : "—")
                + "   (" + safe(bestHighlightLabel != null ? bestHighlightLabel.getText() : "—") + ")", normal));
        document.add(new Paragraph("- Jour difficile : " + safe(worstDayLabel != null ? worstDayLabel.getText() : "—")
                + "   (" + safe(worstHighlightLabel != null ? worstHighlightLabel.getText() : "—") + ")", normal));
        document.add(new Paragraph(" ", normal));
        document.add(new Paragraph("Scores par catégorie", hFont));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100); table.setSpacingBefore(8); table.setWidths(new float[]{3f, 2f, 2f});
        table.addCell(headerCell("Catégorie")); table.addCell(headerCell("Réponses")); table.addCell(headerCell("Moyenne (%)"));
        List<CategorySummary> summaries = result.getCategorySummaries();
        if (summaries == null || summaries.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Aucune donnée", normal)); cell.setColspan(3); cell.setPadding(8); table.addCell(cell);
        } else {
            for (CategorySummary s : summaries) {
                table.addCell(bodyCell(safe(s.getCategory())));
                table.addCell(bodyCell(String.valueOf(s.getCountAnswers())));
                table.addCell(bodyCell(s.getAvgNumericScore() == null ? "—" : s.getAvgNumericScore() + "%"));
            }
        }
        document.add(table); document.close();
    }

    private PdfPCell headerCell(String text) {
        Font f = new Font(Font.HELVETICA, 11, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, f)); cell.setPadding(8); cell.setBackgroundColor(new Color(20, 184, 166)); return cell;
    }
    private PdfPCell bodyCell(String text) {
        Font f = new Font(Font.HELVETICA, 11, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, f)); cell.setPadding(8); return cell;
    }
    private String safe(String s) { return s == null ? "" : s; }
}