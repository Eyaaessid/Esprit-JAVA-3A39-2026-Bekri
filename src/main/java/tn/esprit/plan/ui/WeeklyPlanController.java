package tn.esprit.plan.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Control;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.plan.model.WeeklyPlan;
import tn.esprit.plan.service.WeeklyPlanPdfExporter;
import tn.esprit.plan.service.WeeklyPlanService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.CommunityNavigation;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.PsychologicalProfileNavigation;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class WeeklyPlanController implements Initializable {

    @FXML private ScrollPane rootScroll;
    @FXML private StackPane loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private VBox contentVBox;

    // Form
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private ComboBox<String> objectifCombo;
    @FXML private ComboBox<String> exerciceCombo;
    @FXML private TextArea restrictionsArea;
    @FXML private Button generateBtn;
    @FXML private Button exportPdfBtn;

    // Results visibility
    @FXML private VBox resultsBox;

    // Stats
    @FXML private Label imcValueLabel;
    @FXML private Label caloriesValueLabel;
    @FXML private Label hydratationValueLabel;
    @FXML private Label sommeilValueLabel;

    // Sections
    @FXML private Label resumeLabel;
    @FXML private VBox conseilsBox;
    @FXML private TableView<MealRow> mealTable;
    @FXML private TableColumn<MealRow, String> jourCol;
    @FXML private TableColumn<MealRow, String> petitDejCol;
    @FXML private TableColumn<MealRow, String> dejeunerCol;
    @FXML private TableColumn<MealRow, String> dinerCol;
    @FXML private TableColumn<MealRow, String> collationCol;

    @FXML private HBox exerciseCardsBox;
    @FXML private VBox hydratationTipsBox;
    @FXML private VBox sommeilTipsBox;
    @FXML private Label hydratationGoalLabel;
    @FXML private Label sleepGoalLabel;
    @FXML private VBox hydratationPanel;
    @FXML private VBox sleepPanel;

    private final WeeklyPlanService service = new WeeklyPlanService();
    private final WeeklyPlanPdfExporter pdfExporter = new WeeklyPlanPdfExporter();

    private WeeklyPlan currentPlan;
    private WeeklyPlanService.FormData lastForm;
    private LocalDateTime generatedAt;

    public static final class MealRow {
        private final String jour;
        private final String petitDejeuner;
        private final String dejeuner;
        private final String diner;
        private final String collation;

        public MealRow(String jour, String petitDejeuner, String dejeuner, String diner, String collation) {
            this.jour = jour;
            this.petitDejeuner = petitDejeuner;
            this.dejeuner = dejeuner;
            this.diner = diner;
            this.collation = collation;
        }

        public String getJour() {
            return jour;
        }

        public String getPetitDejeuner() {
            return petitDejeuner;
        }

        public String getDejeuner() {
            return dejeuner;
        }

        public String getDiner() {
            return diner;
        }

        public String getCollation() {
            return collation;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (sexeCombo != null) {
            sexeCombo.getItems().setAll("Homme", "Femme");
            sexeCombo.getSelectionModel().selectFirst();
        }
        if (objectifCombo != null) {
            objectifCombo.getItems().setAll("Perte de poids", "Maintien / santé", "Prise de masse", "Performance");
            objectifCombo.getSelectionModel().select(1);
        }
        if (exerciceCombo != null) {
            exerciceCombo.getItems().setAll("Faible", "Modéré", "Intense");
            exerciceCombo.getSelectionModel().select(1);
        }
        showResults(false);
        setLoading(false, null);

        setupMealTable();

        Platform.runLater(() -> {
            if (rootScroll != null) {
                Scene scene = rootScroll.getScene();
                if (scene != null) {
                    rootScroll.prefWidthProperty().bind(scene.widthProperty());
                    rootScroll.prefHeightProperty().bind(scene.heightProperty());
                }
            }
            if (contentVBox != null && rootScroll != null) {
                contentVBox.prefWidthProperty().bind(rootScroll.widthProperty().subtract(20));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  SIDEBAR NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    @FXML private void handleAccueil(ActionEvent e)        { loadView(stageFrom(e), "/fxml/user-dashboard.fxml"); }
    @FXML private void handleObjectifs(ActionEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckIn(ActionEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlan(ActionEvent e)       { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsights(ActionEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleCommunity(ActionEvent e)      { CommunityNavigation.openPosts(stageFrom(e)); }
    @FXML private void handleChatBot(ActionEvent e)        { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTest(ActionEvent e)           { openPsychologicalTest(); }
    @FXML private void handleProfilPsy(ActionEvent e)      { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfil(ActionEvent e)         { loadView(stageFrom(e), "/fxml/profile.fxml"); }
    @FXML private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        loadView(stageFrom(e), "/fxml/login.fxml");
    }

    private Stage stageFrom(ActionEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }

    private void loadView(Stage stage, String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            DialogHelper.showError("Navigation", ex.getMessage());
        }
    }

    private void openPsychologicalTest() {
        try {
            PsychologicalProfileNavigation.openTestIfAllowedOrDashboard();
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleGenerate(ActionEvent e) {
        WeeklyPlanService.FormData form;
        try {
            form = readAndValidateForm();
        } catch (IllegalArgumentException ex) {
            DialogHelper.showError("Validation", ex.getMessage());
            return;
        }

        lastForm = form;
        setLoading(true, "L’IA génère votre plan…");

        Task<WeeklyPlan> task = new Task<>() {
            @Override
            protected WeeklyPlan call() throws Exception {
                return service.generateWeeklyPlan(form);
            }
        };

        task.setOnSucceeded(ev -> {
            WeeklyPlan plan = task.getValue();
            generatedAt = LocalDateTime.now();
            currentPlan = plan;
            render(plan);
            setLoading(false, null);
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            setLoading(false, null);
            DialogHelper.showError("Génération IA", ex == null ? "Erreur inconnue." : ex.getMessage());
        });

        Thread worker = new Thread(task, "weekly-plan-ai");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleExportPdf(ActionEvent e) {
        if (currentPlan == null || rootScroll == null || rootScroll.getScene() == null) {
            DialogHelper.showError("Export PDF", "Aucun plan à exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le plan PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("bekri-plan-semaine-" + LocalDate.now() + ".pdf");

        File file = chooser.showSaveDialog(rootScroll.getScene().getWindow());
        if (file == null) return;

        String userName = getUserName();
        WeeklyPlanPdfExporter.ExportContext ctx = new WeeklyPlanPdfExporter.ExportContext(userName, lastForm, generatedAt);

        try {
            pdfExporter.export(file, currentPlan, ctx);
            DialogHelper.showSuccess("Export PDF", "Fichier généré : " + file.getAbsolutePath());
        } catch (Exception ex) {
            DialogHelper.showError("Export PDF", "Échec de génération PDF : " + ex.getMessage());
        }
    }

    private WeeklyPlanService.FormData readAndValidateForm() {
        double poids = parseDouble(poidsField, "Poids (kg)");
        double taille = parseDouble(tailleField, "Taille (cm)");
        int age = parseInt(ageField, "Âge");
        if (age < 5 || age > 120) {
            throw new IllegalArgumentException("Âge invalide.");
        }

        String sexe = valueOf(sexeCombo);
        String objectif = valueOf(objectifCombo);
        String exercice = valueOf(exerciceCombo);
        String restrictions = restrictionsArea == null ? "" : safe(restrictionsArea.getText());

        if (poids <= 0 || poids > 500) throw new IllegalArgumentException("Poids invalide.");
        if (taille <= 30 || taille > 260) throw new IllegalArgumentException("Taille invalide.");
        if (sexe.isBlank()) throw new IllegalArgumentException("Veuillez choisir le sexe.");
        if (objectif.isBlank()) throw new IllegalArgumentException("Veuillez choisir l'objectif.");
        if (exercice.isBlank()) throw new IllegalArgumentException("Veuillez choisir l'exercice.");

        return new WeeklyPlanService.FormData(poids, taille, age, sexe, objectif, exercice, restrictions);
    }

    private void render(WeeklyPlan plan) {
        if (plan == null) return;

        showResults(true);
        if (exportPdfBtn != null) {
            exportPdfBtn.setVisible(true);
            exportPdfBtn.setManaged(true);
        }

        // Stats
        if (imcValueLabel != null) imcValueLabel.setText(fmt(plan.getImc(), 1));
        if (caloriesValueLabel != null) caloriesValueLabel.setText(plan.getCaloriesJournalieres() == null ? "—" : plan.getCaloriesJournalieres() + " kcal");
        if (hydratationValueLabel != null) {
            Double v = plan.getHydratation() == null ? null : plan.getHydratation().getLitresParJour();
            hydratationValueLabel.setText(v == null ? "—" : fmt(v, 1) + " L/j");
        }
        if (sommeilValueLabel != null) {
            String h = plan.getSommeil() == null ? null : plan.getSommeil().getHeuresRecommandees();
            sommeilValueLabel.setText(h == null || h.isBlank() ? "—" : h);
        }

        // Resume
        if (resumeLabel != null) {
            resumeLabel.setText(safe(plan.getResume()));
        }

        // Conseils
        fillConseils(conseilsBox, plan.getConseilsGeneraux());

        // Repas table
        renderMealTable(plan.getRepas());

        // Exercices cards row
        renderExerciseCards(exerciseCardsBox, plan.getExercices());

        // Tips
        if (hydratationGoalLabel != null) {
            Double v = plan.getHydratation() == null ? null : plan.getHydratation().getLitresParJour();
            hydratationGoalLabel.setText("Objectif : " + (v == null ? "—" : fmt(v, 1) + "L") + " par jour");
        }
        if (sleepGoalLabel != null) {
            String h = plan.getSommeil() == null ? null : plan.getSommeil().getHeuresRecommandees();
            sleepGoalLabel.setText("Objectif : " + ((h == null || h.isBlank()) ? "—" : h) + " par nuit");
        }

        fillTips(hydratationTipsBox, "💧", plan.getHydratation() == null ? null : plan.getHydratation().getConseils());
        fillTips(sommeilTipsBox, "🌙", plan.getSommeil() == null ? null : plan.getSommeil().getConseils());

        // Equal grow for exercise cards & panels (in case children were rebuilt)
        if (exerciseCardsBox != null) {
            for (Node card : exerciseCardsBox.getChildren()) {
                if (card instanceof Region r) {
                    HBox.setHgrow(r, Priority.ALWAYS);
                    r.setMaxWidth(Double.MAX_VALUE);
                }
            }
        }
        if (hydratationPanel != null) {
            HBox.setHgrow(hydratationPanel, Priority.ALWAYS);
            hydratationPanel.setMaxWidth(Double.MAX_VALUE);
        }
        if (sleepPanel != null) {
            HBox.setHgrow(sleepPanel, Priority.ALWAYS);
            sleepPanel.setMaxWidth(Double.MAX_VALUE);
        }

        if (rootScroll != null) {
            rootScroll.setVvalue(0);
        }
    }

    private void setupMealTable() {
        if (mealTable == null) return;

        mealTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mealTable.setFixedCellSize(-1);

        if (jourCol != null) jourCol.setCellValueFactory(new PropertyValueFactory<>("jour"));
        if (petitDejCol != null) petitDejCol.setCellValueFactory(new PropertyValueFactory<>("petitDejeuner"));
        if (dejeunerCol != null) dejeunerCol.setCellValueFactory(new PropertyValueFactory<>("dejeuner"));
        if (dinerCol != null) dinerCol.setCellValueFactory(new PropertyValueFactory<>("diner"));
        if (collationCol != null) collationCol.setCellValueFactory(new PropertyValueFactory<>("collation"));

        // Proportional widths (10% + 22.5% *4)
        Platform.runLater(() -> {
            if (mealTable == null) return;
            if (jourCol != null) jourCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.10));
            if (petitDejCol != null) petitDejCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
            if (dejeunerCol != null) dejeunerCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
            if (dinerCol != null) dinerCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
            if (collationCol != null) collationCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
        });

        // Wrapped cells
        setWrappedCellFactory(jourCol, true);
        setWrappedCellFactory(petitDejCol, false);
        setWrappedCellFactory(dejeunerCol, false);
        setWrappedCellFactory(dinerCol, false);
        setWrappedCellFactory(collationCol, false);
    }

    private void renderMealTable(Map<String, WeeklyPlan.RepasDay> repas) {
        if (mealTable == null) return;
        mealTable.getItems().clear();

        for (String d : days()) {
            WeeklyPlan.RepasDay r = repas == null ? null : repas.get(d);
            mealTable.getItems().add(new MealRow(
                    cap(d),
                    r == null ? "—" : safe(r.getPetitDejeuner()),
                    r == null ? "—" : safe(r.getDejeuner()),
                    r == null ? "—" : safe(r.getDiner()),
                    r == null ? "—" : safe(r.getCollation())
            ));
        }
    }

    private void setWrappedCellFactory(TableColumn<MealRow, String> col, boolean dayCol) {
        if (col == null) return;
        col.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.getStyleClass().add(dayCol ? "plan-table-day" : "plan-table-cell");
                setGraphic(label);
                setPrefHeight(Control.USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    label.setText("");
                    setText(null);
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });
    }

    private void renderExerciseCards(HBox box, Map<String, WeeklyPlan.ExerciceDay> exercices) {
        if (box == null) return;
        box.getChildren().clear();
        for (String d : days()) {
            WeeklyPlan.ExerciceDay ex = exercices == null ? null : exercices.get(d);
            box.getChildren().add(buildExerciseCard(d, ex));
        }
    }

    private VBox buildExerciseCard(String day, WeeklyPlan.ExerciceDay ex) {
        boolean rest = "mardi".equalsIgnoreCase(day) || "dimanche".equalsIgnoreCase(day)
                || (ex != null && "repos".equalsIgnoreCase(safe(ex.getType())));

        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMinWidth(100);
        card.getStyleClass().addAll("plan-ex-card", "exercise-card");
        card.getStyleClass().add(rest ? "plan-ex-rest" : "plan-ex-active");

        Label dayLbl = new Label(cap(day));
        dayLbl.getStyleClass().add("plan-ex-day");

        card.getChildren().add(dayLbl);

        if (rest) {
            Label restLbl = new Label("😴 Repos");
            restLbl.getStyleClass().add("plan-ex-rest-title");
            Label restSub = new Label("Journée de récupération");
            restSub.setWrapText(true);
            restSub.getStyleClass().add("plan-ex-rest-sub");
            card.getChildren().addAll(restLbl, restSub);
            return card;
        }

        String type = ex == null ? "—" : safe(ex.getType());
        String duree = ex == null ? "—" : safe(ex.getDuree());
        String intensite = ex == null ? "—" : safe(ex.getIntensite());
        String desc = ex == null ? "—" : safe(ex.getDescription());

        Label typeLbl = new Label(type);
        typeLbl.setWrapText(true);
        typeLbl.getStyleClass().add("plan-ex-type");

        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER);
        Label clock = new Label("⏱");
        clock.getStyleClass().add("plan-ex-meta-ico");
        Label time = new Label(duree);
        time.getStyleClass().add("plan-ex-meta");
        timeRow.getChildren().addAll(clock, time);

        Label intLbl = new Label(intensite);
        intLbl.getStyleClass().add("plan-ex-meta");

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);
        descLbl.getStyleClass().add("plan-ex-desc");
        descLbl.maxWidthProperty().bind(card.widthProperty().subtract(16));

        card.getChildren().addAll(typeLbl, timeRow, intLbl, descLbl);
        return card;
    }

    private void addTableHeader(GridPane grid, int row, String... titles) {
        for (int col = 0; col < titles.length; col++) {
            Label lbl = new Label(titles[col]);
            lbl.getStyleClass().add("plan-table-header");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setPadding(new Insets(10, 10, 10, 10));
            GridPane.setHalignment(lbl, HPos.LEFT);
            grid.add(lbl, col, row);
        }
    }

    private void addCell(GridPane grid, int col, int row, String text, boolean dayCol) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setAlignment(Pos.TOP_LEFT);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setPadding(new Insets(10, 10, 10, 10));
        lbl.getStyleClass().add(dayCol ? "plan-table-day" : "plan-table-cell");
        grid.add(lbl, col, row);
    }

    private void fillConseils(VBox box, List<String> items) {
        if (box == null) return;
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label l = new Label("—");
            l.getStyleClass().add("plan-muted");
            box.getChildren().add(l);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            String s = items.get(i);
            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("plan-conseil-row");

            Label check = new Label("✓");
            check.getStyleClass().add("plan-check");
            check.setMinWidth(24);

            Label txt = new Label(safe(s));
            txt.setWrapText(true);
            txt.getStyleClass().add("plan-conseil-text");

            row.getChildren().addAll(check, txt);
            box.getChildren().add(row);

            if (i < items.size() - 1) {
                Separator sep = new Separator();
                sep.getStyleClass().add("plan-row-sep");
                box.getChildren().add(sep);
            }
        }
    }

    private void fillTips(VBox box, String icon, List<String> items) {
        if (box == null) return;
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label l = new Label("—");
            l.getStyleClass().add("plan-muted");
            box.getChildren().add(l);
            return;
        }
        for (String s : items) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("plan-tip-row");
            Label ic = new Label(icon);
            ic.getStyleClass().add("plan-tip-ico");
            Label txt = new Label(safe(s));
            txt.setWrapText(true);
            txt.getStyleClass().add("plan-tip-text");
            row.getChildren().addAll(ic, txt);
            box.getChildren().add(row);
        }
    }

    private void showResults(boolean visible) {
        if (resultsBox != null) {
            resultsBox.setVisible(visible);
            resultsBox.setManaged(visible);
        }
        if (exportPdfBtn != null) {
            exportPdfBtn.setVisible(visible);
            exportPdfBtn.setManaged(visible);
        }
    }

    private void setLoading(boolean loading, String message) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(loading);
            loadingOverlay.setManaged(loading);
        }
        if (loadingLabel != null && message != null) {
            loadingLabel.setText(message);
        }
        if (generateBtn != null) generateBtn.setDisable(loading);
        if (exportPdfBtn != null) exportPdfBtn.setDisable(loading);
        disableForm(loading);
    }

    private void disableForm(boolean disabled) {
        for (Node n : new Node[]{poidsField, tailleField, ageField, sexeCombo, objectifCombo, exerciceCombo, restrictionsArea}) {
            if (n != null) n.setDisable(disabled);
        }
    }

    private String getUserName() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return "Utilisateur";
        String full = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
        return full.isBlank() ? "Utilisateur" : full;
    }

    private static List<String> days() {
        return List.of("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche");
    }

    private static String cap(String s) {
        if (s == null || s.isBlank()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String valueOf(ComboBox<String> cb) {
        if (cb == null) return "";
        String v = cb.getValue();
        return v == null ? "" : v.trim();
    }

    private static double parseDouble(TextField field, String label) {
        if (field == null) throw new IllegalArgumentException(label + " manquant.");
        String raw = safe(field.getText()).replace(",", ".");
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private static int parseInt(TextField field, String label) {
        if (field == null) throw new IllegalArgumentException(label + " manquant.");
        String raw = safe(field.getText());
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private static String fmt(Double d, int decimals) {
        if (d == null) return "—";
        double m = Math.pow(10, decimals);
        return String.valueOf(Math.round(d * m) / m);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
