package tn.esprit.objectif.ui;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.CommunityNavigation;
import tn.esprit.user.shell.UserShellNavigator;
import tn.esprit.user.shell.UserShellRoute;
import tn.esprit.objectif.entity.ObjectifBienEtre;
import tn.esprit.objectif.model.ObjectifBienEtreDto;
import tn.esprit.objectif.service.ObjectifBienEtreService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ObjectifsController {

    @FXML private FlowPane          cardsPane;
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  filterStatut;
    @FXML private HBox              paginationBar;
    @FXML private Label             pageLabel;
    @FXML private Button            btnPrev;
    @FXML private Button            btnNext;

    private static final int PAGE_SIZE = 6;
    private int currentPage = 0;
    private List<ObjectifBienEtreDto> filteredList = List.of();

    private final ObjectifBienEtreService objectifService = new ObjectifBienEtreService();
    private List<ObjectifBienEtreDto> allObjectifs = List.of();

    @FXML
    public void initialize() {
        filterStatut.getItems().addAll("en_cours", "atteint", "abandonne");
        filterStatut.setConverter(new StringConverter<>() {
            @Override public String toString(String v) {
                if (v == null) return "Tous les statuts";
                return switch (v) {
                    case "en_cours"  -> "En cours";
                    case "atteint"   -> "Atteint";
                    case "abandonne" -> "Abandonné";
                    default          -> v;
                };
            }
            @Override public String fromString(String s) { return s; }
        });

        cardsPane.widthProperty().addListener((obs, oldW, newW) -> relayout(newW.doubleValue()));

        loadCards();
    }

    private void loadCards() {
        try {
            var session = SessionManager.getInstance().getCurrentUser();
            if (session == null || session.getId() == null) {
                allObjectifs = List.of();
            } else {
                allObjectifs = objectifService.findByUtilisateurId(session.getId()).stream()
                        .map(ObjectifEntityMapper::toDto)
                        .collect(Collectors.toList());
            }
            applyFilter();
        } catch (Exception e) {
            showError("Impossible de charger les objectifs : " + e.getMessage());
        }
    }

    private void applyFilter() {
        String query  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statut = filterStatut.getValue();
        filteredList = allObjectifs.stream()
                .filter(o -> query.isEmpty()
                        || o.getTitre().toLowerCase().contains(query)
                        || (o.getDescription() != null && o.getDescription().toLowerCase().contains(query)))
                .filter(o -> statut == null || statut.isEmpty() || statut.equals(o.getStatut()))
                .collect(Collectors.toList());
        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        cardsPane.getChildren().clear();

        int total      = filteredList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.min(currentPage, totalPages - 1);

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        double paneW = cardsPane.getWidth();
        double cardW = computeCardWidth(paneW);

        for (ObjectifBienEtreDto o : filteredList.subList(from, to)) {
            VBox card = buildCard(o);
            card.setPrefWidth(cardW);
            card.setMinWidth(cardW);
            card.setMaxWidth(cardW);
            cardsPane.getChildren().add(card);
        }

        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
        paginationBar.setVisible(totalPages > 1);
        paginationBar.setManaged(totalPages > 1);
    }

    private void relayout(double paneW) {
        if (paneW <= 0) return;
        double cardW = computeCardWidth(paneW);
        cardsPane.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                card.setPrefWidth(cardW);
                card.setMinWidth(cardW);
                card.setMaxWidth(cardW);
            }
        });
    }

    private double computeCardWidth(double paneW) {
        if (paneW <= 0) return 320;
        return Math.floor((paneW - 2 * 20) / 3);
    }

    @FXML private void handleSearch() { applyFilter(); }
    @FXML private void handleNew()    { openForm(null); }

    @FXML private void handleAccueil(ActionEvent e)         { UserShellNavigator.navigate(UserShellRoute.DASHBOARD); }
    @FXML private void handleObjectifs(ActionEvent e)       { UserShellNavigator.navigate(UserShellRoute.OBJECTIFS); }
    @FXML private void handleDailyCheckIn(ActionEvent e)    { UserShellNavigator.navigate(UserShellRoute.SUIVI_TODAY); }
    @FXML private void handleWeekPlan(ActionEvent e)        { UserShellNavigator.navigate(UserShellRoute.PLAN_WEEKLY); }
    @FXML private void handleWeeklyInsights(ActionEvent e)  { UserShellNavigator.navigate(UserShellRoute.WEEKLY_INSIGHT); }
    @FXML private void handleCommunity(ActionEvent e)       { CommunityNavigation.openPosts(stageFrom(e)); }
    @FXML private void handleChatBot(ActionEvent e)         { UserShellNavigator.navigate(UserShellRoute.CHAT_COACH); }
    @FXML private void handleTest(ActionEvent e)            { UserShellNavigator.navigate(UserShellRoute.TEST_PSY); }
    @FXML private void handleProfilPsy(ActionEvent e)       { UserShellNavigator.navigate(UserShellRoute.PROFIL_PSY); }
    @FXML private void handleProfil(ActionEvent e)          { UserShellNavigator.navigate(UserShellRoute.PROFILE); }
    @FXML private void handleLogout(ActionEvent e) {
        UserShellNavigator.logout();
    }

    private Stage stageFrom(ActionEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }

    @FXML
    private void handlePrev() {
        if (currentPage > 0) { currentPage--; renderPage(); }
    }

    @FXML
    private void handleNext() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) { currentPage++; renderPage(); }
    }

    private VBox buildCard(ObjectifBienEtreDto o) {
        VBox card = new VBox(0);
        card.getStyleClass().add("obj-card");

        VBox top = new VBox(8);
        top.getStyleClass().add("obj-card-top");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(o.getTitre());
        titleLabel.getStyleClass().add("obj-title");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label statutChip = new Label(formatStatut(o.getStatut()));
        statutChip.getStyleClass().addAll("chip", chipClass(o.getStatut()));

        titleRow.getChildren().addAll(titleLabel, statutChip);

        Label descLabel = new Label(o.getDescription() != null ? o.getDescription() : "");
        descLabel.getStyleClass().add("obj-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(42);

        top.getChildren().addAll(titleRow, descLabel);

        VBox progressSection = new VBox(6);
        progressSection.getStyleClass().add("progress-section");

        int pct = computePct(o);

        HBox progressLabelRow = new HBox();
        progressLabelRow.setAlignment(Pos.CENTER_LEFT);
        Label progText = new Label("Progression");
        progText.getStyleClass().add("progress-label-text");
        HBox.setHgrow(progText, Priority.ALWAYS);
        Label progPct = new Label(pct + "%");
        progPct.getStyleClass().add("progress-pct");
        progressLabelRow.getChildren().addAll(progText, progPct);

        StackPane track = new StackPane();
        track.getStyleClass().add("progress-track");
        track.setPrefHeight(6); track.setMaxHeight(6); track.setMinHeight(6);

        Region fill = new Region();
        fill.getStyleClass().add("progress-bar-fill");
        fill.setPrefHeight(6); fill.setMaxHeight(6); fill.setMinHeight(6);

        double fraction = Math.min(pct / 100.0, 1.0);
        track.widthProperty().addListener((obs, oldW, newW) ->
                fill.setPrefWidth(newW.doubleValue() * fraction));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        progressSection.getChildren().addAll(progressLabelRow, track);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("obj-card-meta");
        meta.getChildren().addAll(
                metaItem("🏷  " + capitalize(o.getType())),
                metaItem("🎯  Cible : " + o.getValeurCible()
                        + "   |   Actuelle : " + (o.getValeurActuelle() != null ? o.getValeurActuelle() : "N/A")),
                metaItem("📅  " + fmt(o.getDateDebut()) + " → " + fmt(o.getDateFin()))
        );

        HBox actions = new HBox(10);
        actions.getStyleClass().add("obj-card-actions");

        Button editBtn = new Button("✏  Modifier");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setOnAction(e -> openForm(o));  // ← was lambda$buildCard$1 in the stack trace

        Button delBtn = new Button("🗑  Supprimer");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(delBtn, Priority.ALWAYS);
        delBtn.setOnAction(e -> handleDelete(o));

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().addAll(top, progressSection, meta, actions);
        return card;
    }

    private Label metaItem(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("meta-item");
        l.setWrapText(true);
        return l;
    }

    private int computePct(ObjectifBienEtreDto o) {
        if (o.getValeurCible() == null || o.getValeurCible() <= 0) return 0;
        if (o.getValeurActuelle() == null) return 0;
        return Math.min((int) Math.round((o.getValeurActuelle() / o.getValeurCible()) * 100), 100);
    }

    private String chipClass(String statut) {
        if (statut == null) return "chip-en_cours";
        return switch (statut) {
            case "atteint"   -> "chip-atteint";
            case "abandonne" -> "chip-abandonne";
            default          -> "chip-en_cours";
        };
    }

    private String formatStatut(String s) {
        if (s == null) return "";
        return switch (s) {
            case "en_cours"  -> "En cours";
            case "atteint"   -> "Atteint";
            case "abandonne" -> "Abandonné";
            default          -> s;
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String fmt(Object date) { return date != null ? date.toString() : "—"; }

    // ── THE FIX ──────────────────────────────────────────────────────────────
    // Before: VBox root = loader.load();
    //         → CRASH because objectif-form.fxml root is <BorderPane>, not <VBox>
    // After:  Parent root = loader.load();
    //         → Works for ANY root element type (BorderPane, VBox, AnchorPane, etc.)
    // ─────────────────────────────────────────────────────────────────────────
    private void openForm(ObjectifBienEtreDto existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/objectif-form.fxml"));

            Parent root = loader.load(); // ← FIXED: was "VBox root" — caused ClassCastException

            ObjectifFormController ctrl = loader.getController();
            if (existing != null) ctrl.setObjectif(existing);

            Scene scene = new Scene(root);
            var css = getClass().getResource("/css/bekri.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "Nouvel objectif" : "Modifier l'objectif");
            stage.setScene(scene);
            stage.showAndWait();

            ObjectifBienEtreDto dto = ctrl.getResult();
            if (dto != null) {
                try {
                    var session = SessionManager.getInstance().getCurrentUser();
                    if (session == null || session.getId() == null) {
                        throw new IllegalStateException("Utilisateur non connecté");
                    }
                    ObjectifBienEtre entity = ObjectifEntityMapper.toEntity(dto);
                    entity.setUtilisateurId(session.getId());
                    if (existing == null) {
                        objectifService.save(entity);
                    } else {
                        entity.setId(existing.getId());
                        objectifService.save(entity);
                    }
                    loadCards();
                } catch (Exception ex) {
                    showError("Erreur lors de la sauvegarde : " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void handleDelete(ObjectifBienEtreDto o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer cet objectif ?");
        confirm.setContentText(o.getTitre());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                objectifService.deleteById(o.getId());
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
