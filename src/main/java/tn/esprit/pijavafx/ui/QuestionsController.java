package tn.esprit.pijavafx.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.pijavafx.model.QuestionEvaluationDto;
import tn.esprit.pijavafx.service.IQuestionService;
import tn.esprit.pijavafx.service.QuestionServiceHttp;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionsController {

    @FXML private FlowPane          cardsPane;
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  filterCategory;
    @FXML private HBox              paginationBar;
    @FXML private Label             pageLabel;
    @FXML private Button            btnPrev;
    @FXML private Button            btnNext;
    @FXML private Label             countLabel;

    private static final int PAGE_SIZE = 8;
    private int currentPage = 0;
    private List<QuestionEvaluationDto> filteredList = List.of();

    private final IQuestionService service = new QuestionServiceHttp();
    private List<QuestionEvaluationDto> allQuestions = List.of();

    @FXML
    public void initialize() {
        filterCategory.getItems().addAll(
                "humeur", "sommeil", "poids", "nutrition", "activite", "hydratation"
        );
        filterCategory.setConverter(new StringConverter<>() {
            @Override public String toString(String v) {
                if (v == null) return "Toutes les catégories";
                return switch (v) {
                    case "humeur"      -> "Humeur";
                    case "sommeil"     -> "Sommeil";
                    case "poids"       -> "Poids";
                    case "nutrition"   -> "Nutrition";
                    case "activite"    -> "Activité physique";
                    case "hydratation" -> "Hydratation";
                    default            -> v;
                };
            }
            @Override public String fromString(String s) { return s; }
        });

        // Re-layout cards on every width change (window resize)
        cardsPane.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w > 0) relayout(w);
        });

        loadCards();
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    private void loadCards() {
        try {
            allQuestions = service.getAll();
            applyFilter();
        } catch (Exception e) {
            showError("Impossible de charger les questions : " + e.getMessage());
        }
    }

    private void applyFilter() {
        String query    = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = filterCategory.getValue();

        filteredList = allQuestions.stream()
                .filter(q -> query.isEmpty()
                        || q.getTexte().toLowerCase().contains(query)
                        || (q.getCategory() != null && q.getCategory().toLowerCase().contains(query)))
                .filter(q -> category == null || category.isEmpty()
                        || category.equals(q.getCategory()))
                .collect(Collectors.toList());

        currentPage = 0;
        renderPage();
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    private void renderPage() {
        cardsPane.getChildren().clear();

        int total      = filteredList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.min(currentPage, totalPages - 1);

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        double paneW = cardsPane.getWidth();
        double cardW = computeCardWidth(paneW);

        for (QuestionEvaluationDto q : filteredList.subList(from, to)) {
            VBox card = buildCard(q);
            card.setPrefWidth(cardW);
            card.setMinWidth(cardW);
            card.setMaxWidth(cardW);
            cardsPane.getChildren().add(card);
        }

        if (countLabel != null) {
            countLabel.setText(total + " question(s)");
        }

        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    private void relayout(double paneW) {
        double cardW = computeCardWidth(paneW);
        cardsPane.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                card.setPrefWidth(cardW);
                card.setMinWidth(cardW);
                card.setMaxWidth(cardW);
            }
        });
    }

    /**
     * 4 columns, 3 gaps of 20px each.
     * -2 is a safe rounding buffer so cards never overflow to next row.
     */
    private double computeCardWidth(double paneW) {
        if (paneW <= 0) return 220;
        return Math.floor((paneW - 3 * 20) / 4) - 2;
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────
    @FXML private void handleSearch()      { applyFilter(); }
    @FXML private void handleNewQuestion() { openForm(null); }

    @FXML
    private void handlePrev() {
        if (currentPage > 0) { currentPage--; renderPage(); }
    }

    @FXML
    private void handleNext() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) { currentPage++; renderPage(); }
    }

    // ── Card builder ──────────────────────────────────────────────────────────
    private VBox buildCard(QuestionEvaluationDto q) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        // Category chip
        Label categoryChip = new Label(q.getCategory() != null ? q.getCategory().toUpperCase() : "—");
        categoryChip.getStyleClass().addAll("chip", "chip-type");

        // Question text
        Label texteLabel = new Label(q.getTexte());
        texteLabel.getStyleClass().add("card-title");
        texteLabel.setWrapText(true);
        VBox.setVgrow(texteLabel, Priority.ALWAYS);

        // Options as indigo pills
        FlowPane optionsBox = new FlowPane(6, 6);
        for (String opt : new String[]{ q.getOption1(), q.getOption2(), q.getOption3() }) {
            if (opt != null && !opt.isBlank()) {
                Label badge = new Label(opt);
                badge.getStyleClass().add("badge-option");
                badge.setWrapText(false);
                optionsBox.getChildren().add(badge);
            }
        }

        // Action buttons — stretch to full width
        Button editBtn = new Button("✏  Modifier");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setOnAction(e -> openForm(q));

        Button deleteBtn = new Button("🗑  Supprimer");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setOnAction(e -> handleDelete(q));

        HBox btnBox = new HBox(8, editBtn, deleteBtn);
        btnBox.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(categoryChip, texteLabel, optionsBox, btnBox);
        return card;
    }

    // ── Form ──────────────────────────────────────────────────────────────────
    private void openForm(QuestionEvaluationDto existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/question_form.fxml"));
            VBox root = loader.load();

            QuestionFormController ctrl = loader.getController();
            ctrl.setQuestion(existing);
            ctrl.setOnSave(dto -> {
                try {
                    if (existing == null) service.create(dto);
                    else service.update(existing.getId(), dto);
                    loadCards();
                } catch (Exception ex) {
                    showError("Erreur lors de la sauvegarde : " + ex.getMessage());
                }
            });

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/ui/app.css").toExternalForm()
            );

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "Nouvelle question" : "Modifier la question");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    private void handleDelete(QuestionEvaluationDto q) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer cette question ?");
        confirm.setContentText(q.getTexte());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.delete(q.getId());
                loadCards();
            } catch (Exception e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}