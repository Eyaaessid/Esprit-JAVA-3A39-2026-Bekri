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
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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
import tn.esprit.shared.DialogHelper;
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
    @FXML private StackPane  loadingOverlay;
    @FXML private Label      loadingLabel;
    @FXML private VBox       contentVBox;

    // Form fields
    @FXML private TextField        poidsField;
    @FXML private TextField        tailleField;
    @FXML private TextField        ageField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private ComboBox<String> objectifCombo;
    @FXML private ComboBox<String> exerciceCombo;
    @FXML private TextArea         restrictionsArea;
    @FXML private Button           generateBtn;
    @FXML private Button           exportPdfBtn;

    // Inline error labels
    @FXML private Label poidsError;
    @FXML private Label tailleError;
    @FXML private Label ageError;
    @FXML private Label sexeError;
    @FXML private Label objectifError;
    @FXML private Label exerciceError;

    // Results
    @FXML private VBox  resultsBox;
    @FXML private Label imcValueLabel;
    @FXML private Label caloriesValueLabel;
    @FXML private Label hydratationValueLabel;
    @FXML private Label sommeilValueLabel;
    @FXML private Label resumeLabel;
    @FXML private VBox  conseilsBox;

    @FXML private TableView<MealRow>          mealTable;
    @FXML private TableColumn<MealRow,String> jourCol;
    @FXML private TableColumn<MealRow,String> petitDejCol;
    @FXML private TableColumn<MealRow,String> dejeunerCol;
    @FXML private TableColumn<MealRow,String> dinerCol;
    @FXML private TableColumn<MealRow,String> collationCol;

    @FXML private HBox  exerciseCardsBox;
    @FXML private VBox  hydratationTipsBox;
    @FXML private VBox  sommeilTipsBox;
    @FXML private Label hydratationGoalLabel;
    @FXML private Label sleepGoalLabel;
    @FXML private VBox  hydratationPanel;
    @FXML private VBox  sleepPanel;

    private final WeeklyPlanService      service      = new WeeklyPlanService();
    private final WeeklyPlanPdfExporter  pdfExporter  = new WeeklyPlanPdfExporter();

    private WeeklyPlan                   currentPlan;
    private WeeklyPlanService.FormData   lastForm;
    private LocalDateTime                generatedAt;

    // CSS classes matching bekri.css input states
    private static final String INVALID = "input-invalid";
    private static final String VALID   = "input-valid";   // maps to .field-valid in CSS

    // ─────────────────────────────────────────────────────────────────
    //  MealRow
    // ─────────────────────────────────────────────────────────────────

    public static final class MealRow {
        private final String jour, petitDejeuner, dejeuner, diner, collation;
        public MealRow(String jour, String petitDejeuner, String dejeuner, String diner, String collation) {
            this.jour = jour; this.petitDejeuner = petitDejeuner;
            this.dejeuner = dejeuner; this.diner = diner; this.collation = collation;
        }
        public String getJour()          { return jour; }
        public String getPetitDejeuner() { return petitDejeuner; }
        public String getDejeuner()      { return dejeuner; }
        public String getDiner()         { return diner; }
        public String getCollation()     { return collation; }
    }

    // ─────────────────────────────────────────────────────────────────
    //  initialize
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (sexeCombo     != null) sexeCombo.getItems().setAll("Homme", "Femme");
        if (objectifCombo != null) objectifCombo.getItems().setAll("Perte de poids","Maintien / santé","Prise de masse","Performance");
        if (exerciceCombo != null) exerciceCombo.getItems().setAll("Faible","Modéré","Intense");

        // Pre-select defaults (won't trigger validation errors)
        if (sexeCombo     != null) sexeCombo.getSelectionModel().selectFirst();
        if (objectifCombo != null) objectifCombo.getSelectionModel().select(1);
        if (exerciceCombo != null) exerciceCombo.getSelectionModel().select(1);

        // Live validation listeners
        if (poidsField    != null) poidsField.textProperty().addListener((o,a,b)    -> livePoids());
        if (tailleField   != null) tailleField.textProperty().addListener((o,a,b)   -> liveTaille());
        if (ageField      != null) ageField.textProperty().addListener((o,a,b)      -> liveAge());
        if (sexeCombo     != null) sexeCombo.valueProperty().addListener((o,a,b)    -> { if (b!=null && !b.isBlank()) clearError(sexeCombo,sexeError);     });
        if (objectifCombo != null) objectifCombo.valueProperty().addListener((o,a,b)-> { if (b!=null && !b.isBlank()) clearError(objectifCombo,objectifError); });
        if (exerciceCombo != null) exerciceCombo.valueProperty().addListener((o,a,b)-> { if (b!=null && !b.isBlank()) clearError(exerciceCombo,exerciceError); });

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
            if (contentVBox != null && rootScroll != null)
                contentVBox.prefWidthProperty().bind(rootScroll.widthProperty().subtract(20));
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Live validators
    // ─────────────────────────────────────────────────────────────────

    private void livePoids() {
        String s = str(poidsField.getText());
        if (s.isEmpty()) { clearError(poidsField, poidsError); return; }
        try {
            double d = Double.parseDouble(s.replace(",","."));
            if (d <= 0 || d > 500) showError(poidsField, poidsError, "Poids invalide (1 – 500 kg).");
            else                   clearError(poidsField, poidsError);
        } catch (NumberFormatException e) { showError(poidsField, poidsError, "Nombre valide requis."); }
    }

    private void liveTaille() {
        String s = str(tailleField.getText());
        if (s.isEmpty()) { clearError(tailleField, tailleError); return; }
        try {
            double d = Double.parseDouble(s.replace(",","."));
            if (d <= 30 || d > 260) showError(tailleField, tailleError, "Taille invalide (31 – 260 cm).");
            else                    clearError(tailleField, tailleError);
        } catch (NumberFormatException e) { showError(tailleField, tailleError, "Nombre valide requis."); }
    }

    private void liveAge() {
        String s = str(ageField.getText());
        if (s.isEmpty()) { clearError(ageField, ageError); return; }
        try {
            int i = Integer.parseInt(s);
            if (i < 5 || i > 120) showError(ageField, ageError, "Âge invalide (5 – 120 ans).");
            else                   clearError(ageField, ageError);
        } catch (NumberFormatException e) { showError(ageField, ageError, "Entier valide requis."); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Full validation on submit — returns FormData or null
    // ─────────────────────────────────────────────────────────────────

    private WeeklyPlanService.FormData validateAndBuildForm() {
        boolean ok = true;

        // Poids
        String sp = str(poidsField != null ? poidsField.getText() : "");
        double poids = 0;
        if (sp.isEmpty()) { showError(poidsField, poidsError, "Le poids est obligatoire."); ok = false; }
        else { try {
            poids = Double.parseDouble(sp.replace(",","."));
            if (poids <= 0 || poids > 500) { showError(poidsField, poidsError, "Poids invalide (1 – 500 kg)."); ok = false; }
            else clearError(poidsField, poidsError);
        } catch (NumberFormatException e) { showError(poidsField, poidsError, "Nombre valide requis."); ok = false; } }

        // Taille
        String st = str(tailleField != null ? tailleField.getText() : "");
        double taille = 0;
        if (st.isEmpty()) { showError(tailleField, tailleError, "La taille est obligatoire."); ok = false; }
        else { try {
            taille = Double.parseDouble(st.replace(",","."));
            if (taille <= 30 || taille > 260) { showError(tailleField, tailleError, "Taille invalide (31 – 260 cm)."); ok = false; }
            else clearError(tailleField, tailleError);
        } catch (NumberFormatException e) { showError(tailleField, tailleError, "Nombre valide requis."); ok = false; } }

        // Âge
        String sa = str(ageField != null ? ageField.getText() : "");
        int age = 0;
        if (sa.isEmpty()) { showError(ageField, ageError, "L'âge est obligatoire."); ok = false; }
        else { try {
            age = Integer.parseInt(sa);
            if (age < 5 || age > 120) { showError(ageField, ageError, "Âge invalide (5 – 120 ans)."); ok = false; }
            else clearError(ageField, ageError);
        } catch (NumberFormatException e) { showError(ageField, ageError, "Entier valide requis."); ok = false; } }

        // Sexe
        String sexe = valueOf(sexeCombo);
        if (sexe.isBlank()) { showError(sexeCombo, sexeError, "Veuillez choisir le sexe."); ok = false; }
        else clearError(sexeCombo, sexeError);

        // Objectif
        String objectif = valueOf(objectifCombo);
        if (objectif.isBlank()) { showError(objectifCombo, objectifError, "Veuillez choisir l'objectif."); ok = false; }
        else clearError(objectifCombo, objectifError);

        // Exercice
        String exercice = valueOf(exerciceCombo);
        if (exercice.isBlank()) { showError(exerciceCombo, exerciceError, "Veuillez choisir le niveau d'exercice."); ok = false; }
        else clearError(exerciceCombo, exerciceError);

        if (!ok) return null;

        String restrictions = restrictionsArea == null ? "" : safe(restrictionsArea.getText());
        return new WeeklyPlanService.FormData(poids, taille, age, sexe, objectif, exercice, restrictions);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Error helpers — show/hide the error-label pill + border the field
    // ─────────────────────────────────────────────────────────────────

    /** Show red pill label beneath the field and add invalid border to the control. */
    private void showError(Control field, Label errorLabel, String message) {
        if (field != null) {
            field.getStyleClass().removeAll(VALID, INVALID);
            field.getStyleClass().add(INVALID);
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /** Remove error state — hide the pill label and restore normal border. */
    private void clearError(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().removeAll(VALID, INVALID);
            field.getStyleClass().add(VALID);
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Sidebar navigation
    // ─────────────────────────────────────────────────────────────────

    @FXML private void handleAccueil(ActionEvent e)        { loadView(stageFrom(e), "/fxml/user-dashboard.fxml"); }
    @FXML private void handleObjectifs(ActionEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckIn(ActionEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlan(ActionEvent e)       { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsights(ActionEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleChatBot(ActionEvent e)        { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTest(ActionEvent e)           { loadView(stageFrom(e), "/fxml/test.fxml"); }
    @FXML private void handleProfilPsy(ActionEvent e)      { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfil(ActionEvent e)         { loadView(stageFrom(e), "/fxml/profile.fxml"); }
    @FXML private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        loadView(stageFrom(e), "/fxml/login.fxml");
    }

    private Stage stageFrom(ActionEvent e) { return (Stage)((Node)e.getSource()).getScene().getWindow(); }

    private void loadView(Stage stage, String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) { DialogHelper.showError("Navigation", ex.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Generate
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleGenerate(ActionEvent e) {
        WeeklyPlanService.FormData form = validateAndBuildForm();
        if (form == null) return;

        lastForm = form;
        setLoading(true, "L'IA génère votre plan…");

        Task<WeeklyPlan> task = new Task<>() {
            @Override protected WeeklyPlan call() throws Exception { return service.generateWeeklyPlan(form); }
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

    // ─────────────────────────────────────────────────────────────────
    //  Export PDF
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleExportPdf(ActionEvent e) {
        if (currentPlan == null || rootScroll == null || rootScroll.getScene() == null) {
            DialogHelper.showError("Export PDF", "Aucun plan à exporter."); return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le plan PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("bekri-plan-semaine-" + LocalDate.now() + ".pdf");
        File file = chooser.showSaveDialog(rootScroll.getScene().getWindow());
        if (file == null) return;
        try {
            pdfExporter.export(file, currentPlan, new WeeklyPlanPdfExporter.ExportContext(getUserName(), lastForm, generatedAt));
            DialogHelper.showSuccess("Export PDF", "Fichier généré : " + file.getAbsolutePath());
        } catch (Exception ex) { DialogHelper.showError("Export PDF", "Échec : " + ex.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Render results
    // ─────────────────────────────────────────────────────────────────

    private void render(WeeklyPlan plan) {
        if (plan == null) return;
        showResults(true);
        if (exportPdfBtn != null) { exportPdfBtn.setVisible(true); exportPdfBtn.setManaged(true); }

        if (imcValueLabel       != null) imcValueLabel.setText(fmt(plan.getImc(), 1));
        if (caloriesValueLabel  != null) caloriesValueLabel.setText(plan.getCaloriesJournalieres()==null?"—":plan.getCaloriesJournalieres()+" kcal");
        if (hydratationValueLabel != null) {
            Double v = plan.getHydratation()==null?null:plan.getHydratation().getLitresParJour();
            hydratationValueLabel.setText(v==null?"—":fmt(v,1)+" L/j");
        }
        if (sommeilValueLabel != null) {
            String h = plan.getSommeil()==null?null:plan.getSommeil().getHeuresRecommandees();
            sommeilValueLabel.setText(h==null||h.isBlank()?"—":h);
        }
        if (resumeLabel != null) resumeLabel.setText(safe(plan.getResume()));

        fillConseils(conseilsBox, plan.getConseilsGeneraux());
        renderMealTable(plan.getRepas());
        renderExerciseCards(exerciseCardsBox, plan.getExercices());

        if (hydratationGoalLabel != null) {
            Double v = plan.getHydratation()==null?null:plan.getHydratation().getLitresParJour();
            hydratationGoalLabel.setText("Objectif : "+(v==null?"—":fmt(v,1)+"L")+" par jour");
        }
        if (sleepGoalLabel != null) {
            String h = plan.getSommeil()==null?null:plan.getSommeil().getHeuresRecommandees();
            sleepGoalLabel.setText("Objectif : "+((h==null||h.isBlank())?"—":h)+" par nuit");
        }
        fillTips(hydratationTipsBox, "💧", plan.getHydratation()==null?null:plan.getHydratation().getConseils());
        fillTips(sommeilTipsBox,     "🌙", plan.getSommeil()==null?null:plan.getSommeil().getConseils());

        if (exerciseCardsBox != null)
            for (Node card : exerciseCardsBox.getChildren())
                if (card instanceof Region r) { HBox.setHgrow(r, Priority.ALWAYS); r.setMaxWidth(Double.MAX_VALUE); }
        if (hydratationPanel != null) { HBox.setHgrow(hydratationPanel, Priority.ALWAYS); hydratationPanel.setMaxWidth(Double.MAX_VALUE); }
        if (sleepPanel       != null) { HBox.setHgrow(sleepPanel,       Priority.ALWAYS); sleepPanel.setMaxWidth(Double.MAX_VALUE); }
        if (rootScroll       != null) rootScroll.setVvalue(0);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Meal table
    // ─────────────────────────────────────────────────────────────────

    private void setupMealTable() {
        if (mealTable == null) return;
        mealTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mealTable.setFixedCellSize(-1);
        if (jourCol      != null) jourCol.setCellValueFactory(new PropertyValueFactory<>("jour"));
        if (petitDejCol  != null) petitDejCol.setCellValueFactory(new PropertyValueFactory<>("petitDejeuner"));
        if (dejeunerCol  != null) dejeunerCol.setCellValueFactory(new PropertyValueFactory<>("dejeuner"));
        if (dinerCol     != null) dinerCol.setCellValueFactory(new PropertyValueFactory<>("diner"));
        if (collationCol != null) collationCol.setCellValueFactory(new PropertyValueFactory<>("collation"));
        Platform.runLater(() -> {
            if (mealTable == null) return;
            if (jourCol      != null) jourCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.10));
            if (petitDejCol  != null) petitDejCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
            if (dejeunerCol  != null) dejeunerCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
            if (dinerCol     != null) dinerCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
            if (collationCol != null) collationCol.prefWidthProperty().bind(mealTable.widthProperty().multiply(0.225));
        });
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
            mealTable.getItems().add(new MealRow(cap(d),
                    r==null?"—":safe(r.getPetitDejeuner()), r==null?"—":safe(r.getDejeuner()),
                    r==null?"—":safe(r.getDiner()),         r==null?"—":safe(r.getCollation())));
        }
    }

    private void setWrappedCellFactory(TableColumn<MealRow,String> col, boolean dayCol) {
        if (col == null) return;
        col.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
            private final Label label = new Label();
            { label.setWrapText(true); label.setMaxWidth(Double.MAX_VALUE);
                label.getStyleClass().add(dayCol?"plan-table-day":"plan-table-cell");
                setGraphic(label); setPrefHeight(Control.USE_COMPUTED_SIZE); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty||item==null) { label.setText(""); setText(null); setGraphic(null); }
                else { label.setText(item); setGraphic(label); }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Exercise cards
    // ─────────────────────────────────────────────────────────────────

    private void renderExerciseCards(HBox box, Map<String,WeeklyPlan.ExerciceDay> exercices) {
        if (box == null) return;
        box.getChildren().clear();
        for (String d : days()) box.getChildren().add(buildExerciseCard(d, exercices==null?null:exercices.get(d)));
    }

    private VBox buildExerciseCard(String day, WeeklyPlan.ExerciceDay ex) {
        boolean rest = "mardi".equalsIgnoreCase(day) || "dimanche".equalsIgnoreCase(day)
                || (ex != null && "repos".equalsIgnoreCase(safe(ex.getType())));
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER); card.setMinWidth(100);
        card.getStyleClass().addAll("plan-ex-card","exercise-card");
        card.getStyleClass().add(rest?"plan-ex-rest":"plan-ex-active");
        Label dayLbl = new Label(cap(day)); dayLbl.getStyleClass().add("plan-ex-day");
        card.getChildren().add(dayLbl);
        if (rest) {
            Label r1 = new Label("😴 Repos"); r1.getStyleClass().add("plan-ex-rest-title");
            Label r2 = new Label("Journée de récupération"); r2.setWrapText(true); r2.getStyleClass().add("plan-ex-rest-sub");
            card.getChildren().addAll(r1, r2); return card;
        }
        String type=ex==null?"—":safe(ex.getType()), duree=ex==null?"—":safe(ex.getDuree()),
                intensite=ex==null?"—":safe(ex.getIntensite()), desc=ex==null?"—":safe(ex.getDescription());
        Label typeLbl=new Label(type); typeLbl.setWrapText(true); typeLbl.getStyleClass().add("plan-ex-type");
        HBox timeRow=new HBox(6); timeRow.setAlignment(Pos.CENTER);
        Label clock=new Label("⏱"); clock.getStyleClass().add("plan-ex-meta-ico");
        Label time=new Label(duree); time.getStyleClass().add("plan-ex-meta");
        timeRow.getChildren().addAll(clock,time);
        Label intLbl=new Label(intensite); intLbl.getStyleClass().add("plan-ex-meta");
        Label descLbl=new Label(desc); descLbl.setWrapText(true); descLbl.setAlignment(Pos.CENTER);
        descLbl.getStyleClass().add("plan-ex-desc"); descLbl.maxWidthProperty().bind(card.widthProperty().subtract(16));
        card.getChildren().addAll(typeLbl,timeRow,intLbl,descLbl);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Tips / conseils
    // ─────────────────────────────────────────────────────────────────

    private void fillConseils(VBox box, List<String> items) {
        if (box==null) return; box.getChildren().clear();
        if (items==null||items.isEmpty()) { Label l=new Label("—"); l.getStyleClass().add("plan-muted"); box.getChildren().add(l); return; }
        for (int i=0;i<items.size();i++) {
            HBox row=new HBox(10); row.setAlignment(Pos.TOP_LEFT); row.getStyleClass().add("plan-conseil-row");
            Label check=new Label("✓"); check.getStyleClass().add("plan-check"); check.setMinWidth(24);
            Label txt=new Label(safe(items.get(i))); txt.setWrapText(true); txt.getStyleClass().add("plan-conseil-text");
            row.getChildren().addAll(check,txt); box.getChildren().add(row);
            if (i<items.size()-1) { Separator sep=new Separator(); sep.getStyleClass().add("plan-row-sep"); box.getChildren().add(sep); }
        }
    }

    private void fillTips(VBox box, String icon, List<String> items) {
        if (box==null) return; box.getChildren().clear();
        if (items==null||items.isEmpty()) { Label l=new Label("—"); l.getStyleClass().add("plan-muted"); box.getChildren().add(l); return; }
        for (String s : items) {
            HBox row=new HBox(8); row.setAlignment(Pos.TOP_LEFT); row.getStyleClass().add("plan-tip-row");
            Label ic=new Label(icon); ic.getStyleClass().add("plan-tip-ico");
            Label txt=new Label(safe(s)); txt.setWrapText(true); txt.getStyleClass().add("plan-tip-text");
            row.getChildren().addAll(ic,txt); box.getChildren().add(row);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  UI state
    // ─────────────────────────────────────────────────────────────────

    private void showResults(boolean visible) {
        if (resultsBox  != null) { resultsBox.setVisible(visible);  resultsBox.setManaged(visible); }
        if (exportPdfBtn!= null) { exportPdfBtn.setVisible(visible);exportPdfBtn.setManaged(visible); }
    }

    private void setLoading(boolean loading, String message) {
        if (loadingOverlay != null) { loadingOverlay.setVisible(loading); loadingOverlay.setManaged(loading); }
        if (loadingLabel   != null && message!=null) loadingLabel.setText(message);
        if (generateBtn    != null) generateBtn.setDisable(loading);
        if (exportPdfBtn   != null) exportPdfBtn.setDisable(loading);
        disableForm(loading);
    }

    private void disableForm(boolean disabled) {
        for (Node n : new Node[]{poidsField,tailleField,ageField,sexeCombo,objectifCombo,exerciceCombo,restrictionsArea})
            if (n!=null) n.setDisable(disabled);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Misc helpers
    // ─────────────────────────────────────────────────────────────────

    private String getUserName() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user==null) return "Utilisateur";
        String full=(safe(user.getPrenom())+" "+safe(user.getNom())).trim();
        return full.isBlank()?"Utilisateur":full;
    }
    private static List<String> days() { return List.of("lundi","mardi","mercredi","jeudi","vendredi","samedi","dimanche"); }
    private static String cap(String s) { if(s==null||s.isBlank())return""; return Character.toUpperCase(s.charAt(0))+s.substring(1); }
    private static String valueOf(ComboBox<String> cb) { if(cb==null)return""; String v=cb.getValue(); return v==null?"":v.trim(); }
    private static String fmt(Double d, int decimals) { if(d==null)return"—"; double m=Math.pow(10,decimals); return String.valueOf(Math.round(d*m)/m); }
    private static String safe(String s) { return s==null?"":s.trim(); }
    private static String str(String s)  { return s==null?"":s.trim(); }
}