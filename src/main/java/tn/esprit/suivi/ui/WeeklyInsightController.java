package tn.esprit.suivi.ui;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.suivi.dao.WeeklyInsightDAO;
import tn.esprit.suivi.model.CategorySummary;
import tn.esprit.suivi.model.WeeklyInsightResult;
import tn.esprit.user.entity.Utilisateur;

import java.awt.Color; // <-- FIX: use AWT Color for PDF cells
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.ResourceBundle;

public class WeeklyInsightController implements Initializable {

    @FXML private ScrollPane rootScroll;

    @FXML private Label periodLabel;
    @FXML private Label daysFilledLabel;
    @FXML private Label globalAvgLabel;
    @FXML private Label bestDayLabel;
    @FXML private Label worstDayLabel;
    @FXML private Label bestHighlightLabel;
    @FXML private Label worstHighlightLabel;
    @FXML private Label encouragementLabel;

    @FXML private VBox emptyStateBox;
    @FXML private VBox contentSections;

    @FXML private PieChart globalPieChart;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private LineChart<String, Number> humeurLineChart;

    @FXML private HBox recommendationBox;
    @FXML private Label donutCenterLabel;

    private final WeeklyInsightDAO dao = new WeeklyInsightDAO();

    private WeeklyInsightResult lastResult;
    private LocalDate lastStart;
    private LocalDate lastEnd;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            DialogHelper.showError("Session", "Utilisateur non connecte.");
            goToDashboard();
            return;
        }
        int userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);

        lastStart = start;
        lastEnd = end;

        if (periodLabel != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
            periodLabel.setText("Période : " + start.format(fmt) + " - " + end.format(fmt));
        }

        WeeklyInsightResult result = dao.getWeeklyInsight(userId, start, end);
        lastResult = result;

        boolean empty = (result == null || result.getTotalSubmittedDays() == 0);
        showEmptyState(empty);

        int submittedDays = (result == null) ? 0 : result.getTotalSubmittedDays();
        if (daysFilledLabel != null) daysFilledLabel.setText(submittedDays + " / 7");
        if (donutCenterLabel != null) donutCenterLabel.setText(submittedDays + "/7");

        List<CategorySummary> summaries = (result == null) ? List.of() : result.getCategorySummaries();
        populateCategorySummaries(summaries);
        populateCategoryChart(summaries);

        if (globalAvgLabel != null) {
            globalAvgLabel.setText(buildGlobalAvgText(summaries));
        }

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");
        if (bestDayLabel != null) {
            bestDayLabel.setText(result != null && result.getBestDay() != null ? result.getBestDay().format(dayFmt) : "—");
        }
        if (worstDayLabel != null) {
            worstDayLabel.setText(result != null && result.getWorstDay() != null ? result.getWorstDay().format(dayFmt) : "—");
        }
        if (bestHighlightLabel != null) {
            bestHighlightLabel.setText(result != null && result.getBestScore() != null ? ("Score : " + result.getBestScore() + "%") : "—");
        }
        if (worstHighlightLabel != null) {
            worstHighlightLabel.setText(result != null && result.getWorstScore() != null ? ("Score : " + result.getWorstScore() + "%") : "—");
        }

        if (encouragementLabel != null) {
            encouragementLabel.setText(empty
                    ? "Vous n'avez pas encore rempli de check-in cette semaine. Commencez dès aujourd'hui !"
                    : "Voici un résumé simple de votre semaine (moyennes numériques par catégorie).");
        }
    }

    private void showEmptyState(boolean empty) {
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(empty);
            emptyStateBox.setManaged(empty);
        }
        if (contentSections != null) {
            contentSections.setVisible(!empty);
            contentSections.setManaged(!empty);
        }
    }

    private void populateCategorySummaries(List<CategorySummary> summaries) {
        if (recommendationBox == null) return;

        recommendationBox.getChildren().clear();
        if (summaries == null || summaries.isEmpty()) return;

        for (CategorySummary s : summaries) {
            VBox card = new VBox(6);
            card.getStyleClass().add("rec-card");

            String category = (s.getCategory() == null || s.getCategory().isBlank()) ? "Autre" : s.getCategory();
            Label title = new Label(category);
            title.getStyleClass().add("rec-title");

            String avg = (s.getAvgNumericScore() == null) ? "—" : (s.getAvgNumericScore() + "%");
            Label detail = new Label("Réponses: " + s.getCountAnswers() + " | Moyenne: " + avg);
            detail.getStyleClass().add("rec-description");
            detail.setWrapText(true);

            card.getChildren().addAll(title, detail);
            recommendationBox.getChildren().add(card);
        }
    }

    private void populateCategoryChart(List<CategorySummary> summaries) {
        if (categoryBarChart == null) return;

        categoryBarChart.getData().clear();
        if (summaries == null || summaries.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Moyenne numerique");

        for (CategorySummary s : summaries) {
            if (s.getAvgNumericScore() == null) continue;
            String cat = (s.getCategory() == null || s.getCategory().isBlank()) ? "Autre" : s.getCategory();
            series.getData().add(new XYChart.Data<>(cat, s.getAvgNumericScore()));
        }

        if (!series.getData().isEmpty()) categoryBarChart.getData().add(series);
    }

    private String buildGlobalAvgText(List<CategorySummary> summaries) {
        if (summaries == null || summaries.isEmpty()) return "Moyenne globale : —";

        double sum = 0;
        int count = 0;
        for (CategorySummary s : summaries) {
            if (s.getAvgNumericScore() == null) continue;
            sum += s.getAvgNumericScore();
            count++;
        }
        if (count == 0) return "Moyenne globale : —";

        double avg = Math.round((sum / count) * 10.0) / 10.0;
        return "Moyenne globale : " + avg + "%";
    }

    // --- FXML handlers ---
    @FXML private void handleBack() { goToDashboard(); }

    @FXML
    private void handleCheckIn() {
        try {
            SceneManager.switchTo("suivi-today");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleExportPdf() {
        if (lastResult == null) {
            DialogHelper.showError("Export PDF", "Aucune donnée à exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("bekri-insights-" + LocalDate.now() + ".pdf");

        Stage stage = SceneManager.getPrimaryStage();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            exportToPdf(file, lastResult, lastStart, lastEnd);
            DialogHelper.showSuccess("Export PDF", "PDF enregistré : " + file.getAbsolutePath());
        } catch (Exception e) {
            DialogHelper.showError("Export PDF", "Erreur export PDF: " + e.getMessage());
        }
    }

    private void exportToPdf(File file, WeeklyInsightResult result, LocalDate start, LocalDate end) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font hFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);

        document.add(new Paragraph("Bekri — Insights hebdomadaires", titleFont));
        document.add(new Paragraph("Période : " + start + " → " + end, normal));
        document.add(new Paragraph(" ", normal));

        document.add(new Paragraph("Résumé", hFont));
        document.add(new Paragraph("- Jours complétés : " + result.getTotalSubmittedDays() + "/7", normal));
        document.add(new Paragraph("- " + safe(globalAvgLabel != null ? globalAvgLabel.getText() : "Moyenne globale : —"), normal));
        document.add(new Paragraph("- Meilleur jour : " + safe(bestDayLabel != null ? bestDayLabel.getText() : "—") +
                " | " + safe(bestHighlightLabel != null ? bestHighlightLabel.getText() : "—"), normal));
        document.add(new Paragraph("- Jour le plus difficile : " + safe(worstDayLabel != null ? worstDayLabel.getText() : "—") +
                " | " + safe(worstHighlightLabel != null ? worstHighlightLabel.getText() : "—"), normal));
        document.add(new Paragraph(" ", normal));

        document.add(new Paragraph("Scores par catégorie", hFont));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setWidths(new float[]{3f, 2f, 2f});

        table.addCell(headerCell("Catégorie"));
        table.addCell(headerCell("Réponses"));
        table.addCell(headerCell("Moyenne (%)"));

        List<CategorySummary> summaries = result.getCategorySummaries();
        if (summaries == null || summaries.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Aucune donnée", normal));
            cell.setColspan(3);
            cell.setPadding(8);
            table.addCell(cell);
        } else {
            for (CategorySummary s : summaries) {
                table.addCell(bodyCell(safe(s.getCategory())));
                table.addCell(bodyCell(String.valueOf(s.getCountAnswers())));
                table.addCell(bodyCell(s.getAvgNumericScore() == null ? "—" : String.valueOf(s.getAvgNumericScore())));
            }
        }

        document.add(table);
        document.close();
    }

    private PdfPCell headerCell(String text) {
        Font f = new Font(Font.HELVETICA, 11, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(230, 244, 243)); // <-- FIXED
        return cell;
    }

    private PdfPCell bodyCell(String text) {
        Font f = new Font(Font.HELVETICA, 11, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setPadding(8);
        return cell;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void goToDashboard() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }
}