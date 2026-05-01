package tn.esprit.views;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import tn.esprit.models.Evenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.FavoriService;
import tn.esprit.services.ParticipationService;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EvenementView {
    private static final double EVENT_CARD_MIN_WIDTH = 360;
    private static final double EVENT_CARD_MAX_WIDTH = 370;
    private static final double EVENT_CARD_HEIGHT = 392;
    private static final double EVENT_IMAGE_HEIGHT = 80;
    private static final double EVENT_GRID_GAP = 24;

    private final BorderPane mainLayout;
    private final ScrollPane scrollPane;

    private final EvenementService evenementService;
    private final FavoriService favoriService;
    private final ParticipationService participationService;

    private final ObservableList<Evenement> evenementList;
    private final FilteredList<Evenement> filteredData;

    private TextField searchField;
    private ComboBox<String> typeFilter;
    private ComboBox<String> statutFilter;
    private TilePane cardsGrid;

    private Map<Integer, Integer> favorisCountMap = new HashMap<>();
    private Map<Integer, Integer> participantsCountMap = new HashMap<>();
    private Set<Integer> mesFavorisIds = new HashSet<>();

    private final int currentUserId = 1;
    private Runnable onEventsChanged;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EvenementView() {
        evenementService = new EvenementService();
        favoriService = new FavoriService();
        participationService = new ParticipationService();

        evenementList = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(evenementList, p -> true);

        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("page-root");
        mainLayout.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox header = createHeader();
        HBox filters = createFilters();
        cardsGrid = createCardsGrid();

        VBox content = new VBox(14, header, filters, cardsGrid);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                updateCardsGridLayout(newBounds.getWidth()));
        Platform.runLater(() -> updateCardsGridLayout(scrollPane.getViewportBounds().getWidth()));
        mainLayout.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                scrollToTop();
            }
        });
        mainLayout.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                scrollToTop();
            }
        });
        mainLayout.setCenter(scrollPane);
        BorderPane.setAlignment(scrollPane, Pos.TOP_LEFT);

        filteredData.addListener((ListChangeListener<? super Evenement>) c -> refreshCards());

        loadData();
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(3);
        Label title = new Label("Événements");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Découvrez les événements disponibles et gérez vos favoris.");
        subtitle.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("Actualiser");
        btnRefresh.getStyleClass().addAll("btn-outline");
        btnRefresh.setGraphic(makeIconLabel("⟳"));
        btnRefresh.setOnAction(e -> loadData());

        Button btnCreer = new Button("Créer");
        btnCreer.getStyleClass().addAll("btn-primary");
        btnCreer.setGraphic(makeIconLabel("+"));
        btnCreer.setOnAction(e -> openCreerEvenement());

        header.getChildren().addAll(titles, spacer, btnRefresh, btnCreer);
        return header;
    }

    private HBox createFilters() {
        HBox filters = new HBox(12);
        filters.getStyleClass().add("search-bar-container");
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setFillHeight(true);

        searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("Rechercher par titre, description, type, statut ou lieu…");
        searchField.setMinWidth(320);
        searchField.setPrefWidth(420);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        typeFilter = new ComboBox<>();
        typeFilter.getStyleClass().add("filter-combo");
        typeFilter.setPrefWidth(198);
        typeFilter.getItems().add("Tous les types");
        typeFilter.setValue("Tous les types");

        statutFilter = new ComboBox<>();
        statutFilter.getStyleClass().add("filter-combo");
        statutFilter.setPrefWidth(198);
        statutFilter.getItems().addAll("Tous les statuts", "Ouvert", "Fermé", "Planifié", "Annulé", "Complet");
        statutFilter.setValue("Tous les statuts");

        Button clear = new Button("Effacer");
        clear.getStyleClass().addAll("btn-muted");
        clear.setMinWidth(94);
        clear.setOnAction(e -> {
            searchField.clear();
            typeFilter.setValue("Tous les types");
            statutFilter.setValue("Tous les statuts");
            applyFilters();
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        typeFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        statutFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        filters.getChildren().addAll(searchField, typeFilter, statutFilter, clear);
        return filters;
    }

    private TilePane createCardsGrid() {
        TilePane grid = new TilePane();
        grid.getStyleClass().addAll("cards-grid", "events-grid");
        grid.setHgap(EVENT_GRID_GAP);
        grid.setVgap(EVENT_GRID_GAP);
        // Extra bottom padding so the last row isn't hidden by OS overlays (e.g. watermark).
        grid.setPadding(new Insets(2, 2, 56, 2));
        grid.setPrefColumns(3);
        grid.setPrefTileWidth(EVENT_CARD_MAX_WIDTH);
        grid.setPrefTileHeight(EVENT_CARD_HEIGHT);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setTileAlignment(Pos.TOP_LEFT);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private void scrollToTop() {
        Platform.runLater(() -> {
            scrollPane.setHvalue(0);
            scrollPane.setVvalue(0);
            if (scrollPane.getScene() != null) {
                scrollPane.applyCss();
                scrollPane.layout();
            }
            scrollPane.setHvalue(0);
            scrollPane.setVvalue(0);
            if (searchField != null) {
                searchField.requestFocus();
            }
            Platform.runLater(() -> {
                scrollPane.setHvalue(0);
                scrollPane.setVvalue(0);
            });
        });
    }

    private void updateCardsGridLayout(double viewportWidth) {
        if (cardsGrid == null || viewportWidth <= 0) {
            return;
        }

        Insets padding = cardsGrid.getPadding() == null ? Insets.EMPTY : cardsGrid.getPadding();
        double usableWidth = viewportWidth - padding.getLeft() - padding.getRight();
        double gap = cardsGrid.getHgap();

        int columns;
        if (usableWidth >= (EVENT_CARD_MIN_WIDTH * 3) + (gap * 2)) {
            columns = 3;
        } else if (usableWidth >= (EVENT_CARD_MIN_WIDTH * 2) + gap) {
            columns = 2;
        } else {
            columns = 1;
        }

        double tileWidth = (usableWidth - (gap * (columns - 1))) / columns;
        tileWidth = Math.min(tileWidth, EVENT_CARD_MAX_WIDTH);
        tileWidth = Math.max(tileWidth, 1);

        cardsGrid.setPrefColumns(columns);
        cardsGrid.setPrefTileWidth(tileWidth);
    }

    private void applyFilters() {
        String q = searchField != null ? searchField.getText() : "";
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        String type = typeFilter != null ? typeFilter.getValue() : null;
        String statut = statutFilter != null ? statutFilter.getValue() : null;

        String selectedType = type == null ? "" : type.trim();
        String selectedStatut = statut == null ? "" : statut.trim();

        filteredData.setPredicate(evenement -> {
            if (evenement == null) {
                return false;
            }

            boolean matchesQuery = query.isEmpty()
                    || safeLower(evenement.getTitre()).contains(query)
                    || safeLower(evenement.getDescription()).contains(query)
                    || safeLower(evenement.getType()).contains(query)
                    || safeLower(evenement.getStatut()).contains(query)
                    || safeLower(evenement.getLieu()).contains(query);

            boolean matchesType = selectedType.isEmpty()
                    || "Tous les types".equalsIgnoreCase(selectedType)
                    || safeLower(evenement.getType()).equals(selectedType.toLowerCase(Locale.ROOT));

            boolean matchesStatut = selectedStatut.isEmpty()
                    || "Tous les statuts".equalsIgnoreCase(selectedStatut)
                    || normalizeStatus(evenement.getStatut()).equals(normalizeStatus(selectedStatut));

            return matchesQuery && matchesType && matchesStatut;
        });

        refreshCards();
    }

    private void refreshCards() {
        if (cardsGrid == null) {
            return;
        }

        cardsGrid.getChildren().clear();

        if (filteredData.isEmpty()) {
            cardsGrid.getChildren().add(createEmptyState());
            return;
        }

        for (Evenement e : filteredData) {
            cardsGrid.getChildren().add(createEventCard(e));
        }
    }

    private Node createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(18));
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(760);
        box.setMinHeight(180);

        Label icon = new Label("○");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Aucun événement trouvé");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Essayez de modifier votre recherche ou vos filtres.");
        desc.getStyleClass().add("page-subtitle");

        box.getChildren().addAll(icon, title, desc);
        return box;
    }

    private Node createEventCard(Evenement evenement) {
        VBox card = new VBox(0);
        card.getStyleClass().addAll("card", "event-card", "events-page-card");
        card.prefWidthProperty().bind(cardsGrid.prefTileWidthProperty());
        card.minWidthProperty().bind(cardsGrid.prefTileWidthProperty());
        card.maxWidthProperty().bind(cardsGrid.prefTileWidthProperty());
        card.setMinHeight(EVENT_CARD_HEIGHT);
        card.setPrefHeight(EVENT_CARD_HEIGHT);
        card.setMaxHeight(EVENT_CARD_HEIGHT);

        StackPane imageBox = createImageBox(evenement);

        VBox body = new VBox(8);
        body.getStyleClass().add("card-body");
        body.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(body, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label type = new Label(cleanText(evenement.getType(), "Type"));
        type.getStyleClass().addAll("pill", "pill-type");

        Label badge = createStatusBadge(evenement.getStatut());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleButton heart = new ToggleButton("\u2665");
        heart.getStyleClass().addAll("icon-button", "icon-button-danger");
        boolean isFav = mesFavorisIds.contains(evenement.getId());
        heart.setSelected(isFav);
        heart.setOnAction(e -> toggleFavori(evenement));

        topRow.getChildren().addAll(type, badge, spacer, heart);

        Label title = new Label(cleanText(evenement.getTitre(), "Sans titre"));
        title.getStyleClass().add("event-title");
        title.setWrapText(true);

        Label desc = new Label(ellipsize(cleanText(evenement.getDescription(), ""), 140));
        desc.getStyleClass().add("event-desc");
        desc.setWrapText(true);
        desc.setMinHeight(32);
        desc.setPrefHeight(32);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("event-meta");

        String dateDebut = evenement.getDate_debut() != null ? evenement.getDate_debut().format(dateTimeFormatter) : "--";
        Label date = new Label("Début: " + dateDebut);
        date.getStyleClass().add("event-meta-line");

        Label lieu = new Label("Lieu: " + cleanText(evenement.getLieu(), "--"));
        lieu.getStyleClass().add("event-meta-line");

        int participants = participantsCountMap.getOrDefault(evenement.getId(), 0);
        int capacite = Math.max(evenement.getCapacite_max(), 0);
        int restantes = Math.max(capacite - participants, 0);

        Label stats = new Label("Participants: " + participants + " / " + capacite + "   |   Places restantes: " + restantes);
        stats.getStyleClass().add("event-stats");
        if (restantes == 0) {
            stats.getStyleClass().add("event-stats-full");
        }

        int nbFavoris = favorisCountMap.getOrDefault(evenement.getId(), 0);
        Label favoris = new Label("Favoris: " + nbFavoris);
        favoris.getStyleClass().add("event-favoris-count");

        meta.getChildren().addAll(date, lieu, stats, favoris);

        Region bodySpacer = new Region();
        VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        body.getChildren().addAll(topRow, title, desc, meta, bodySpacer);

        VBox footer = new VBox(8);
        footer.getStyleClass().addAll("card-footer", "event-card-footer");

        Button btnDetails = new Button("Voir d\u00E9tails");
        btnDetails.getStyleClass().addAll("btn-outline", "event-action-button");
        btnDetails.setOnAction(e -> openVoirEvenement(evenement));

        Button btnParticiper = new Button("Participer");
        btnParticiper.getStyleClass().addAll("btn-primary", "event-action-button");
        btnParticiper.setDisable(!canParticipate(evenement, restantes));
        btnParticiper.setOnAction(e -> openParticiper(evenement));

        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().addAll("btn-outline", "event-action-button");
        btnEdit.setOnAction(e -> openModifierEvenement(evenement));

        Button btnDelete = new Button("Supprimer");
        btnDelete.getStyleClass().addAll("btn-danger", "event-action-button");
        btnDelete.setOnAction(e -> openSupprimerEvenement(evenement));

        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnParticiper.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setMaxWidth(Double.MAX_VALUE);

        HBox primaryActions = new HBox(8, btnDetails, btnParticiper);
        primaryActions.getStyleClass().addAll("event-action-group", "event-action-row");
        primaryActions.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetails, Priority.ALWAYS);
        HBox.setHgrow(btnParticiper, Priority.ALWAYS);

        HBox adminActions = new HBox(12, btnEdit, btnDelete);
        adminActions.getStyleClass().addAll("event-action-group", "event-action-row");
        adminActions.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit, Priority.ALWAYS);
        HBox.setHgrow(btnDelete, Priority.ALWAYS);

        footer.getChildren().addAll(primaryActions, adminActions);

        card.getChildren().addAll(imageBox, body, footer);
        return card;
    }

    private StackPane createImageBox(Evenement evenement) {
        StackPane box = new StackPane();
        box.getStyleClass().add("event-image");
        box.setMinHeight(EVENT_IMAGE_HEIGHT);
        box.setPrefHeight(EVENT_IMAGE_HEIGHT);
        box.setMaxHeight(EVENT_IMAGE_HEIGHT);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(box.widthProperty());
        clip.heightProperty().bind(box.heightProperty());
        box.setClip(clip);

        String imageValue = evenement != null ? evenement.getImage() : null;
        String imageUrl = resolveImageUrl(imageValue);
        if (imageUrl == null) {
            box.getChildren().add(createImagePlaceholder(evenement));
            return box;
        }

        try {
            Image image = new Image(imageUrl, true);
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(false);
            imageView.fitWidthProperty().bind(box.widthProperty());
            imageView.fitHeightProperty().bind(box.heightProperty());
            box.getChildren().add(imageView);
        } catch (Exception ex) {
            box.getChildren().add(createImagePlaceholder(evenement));
        }

        return box;
    }

    private Node createImagePlaceholder(Evenement evenement) {
        VBox placeholder = new VBox(6);
        placeholder.getStyleClass().add("event-image-placeholder-box");
        placeholder.setAlignment(Pos.CENTER);

        SVGPath icon = new SVGPath();
        icon.getStyleClass().add("event-image-placeholder-icon");
        icon.setContent("M5 4H19C20.1 4 21 4.9 21 6V18C21 19.1 20.1 20 19 20H5C3.9 20 3 19.1 3 18V6C3 4.9 3.9 4 5 4ZM5 6V18H19V6H5ZM7 16L10 13L12 15L15 12L17 14V16H7Z");
        icon.setPickOnBounds(false);

        StackPane iconWrap = new StackPane(icon);
        iconWrap.getStyleClass().add("event-image-placeholder-icon-wrap");
        iconWrap.setMinSize(30, 30);
        iconWrap.setPrefSize(30, 30);

        String typeText = cleanText(evenement != null ? evenement.getType() : null, "Evenement");
        Label text = new Label(typeText);
        text.getStyleClass().add("event-image-placeholder-text");

        placeholder.getChildren().addAll(iconWrap, text);
        return placeholder;
    }

    private String resolveImageUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        try {
            File file = new File(value);
            if (file.exists()) {
                return file.toURI().toString();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private Label createStatusBadge(String statut) {
        String normalized = normalizeStatus(statut);
        String display = cleanedStatusLabel(normalized);

        Label badge = new Label(display);
        badge.getStyleClass().add("badge");

        switch (normalized) {
            case "ouvert":
                badge.getStyleClass().add("badge-ouvert");
                break;
            case "ferme":
                badge.getStyleClass().add("badge-ferme");
                break;
            case "planifie":
                badge.getStyleClass().add("badge-planifie");
                break;
            case "annule":
                badge.getStyleClass().add("badge-annule");
                break;
            case "complet":
                badge.getStyleClass().add("badge-complet");
                break;
            default:
                // keep default badge styling
        }
        return badge;
    }

    private String cleanedStatusLabel(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "INCONNU";
        }
        switch (normalized) {
            case "ouvert":
                return "OUVERT";
            case "ferme":
                return "FERME";
            case "planifie":
                return "PLANIFIE";
            case "annule":
                return "ANNULE";
            case "complet":
                return "COMPLET";
            default:
                return normalized.toUpperCase(Locale.ROOT);
        }
    }

    private boolean canParticipate(Evenement evenement, int restantes) {
        if (restantes <= 0) {
            return false;
        }
        String statut = normalizeStatus(evenement != null ? evenement.getStatut() : "");
        return !statut.equals("ferme") && !statut.equals("annule") && !statut.equals("complet");
    }

    private void toggleFavori(Evenement evenement) {
        if (evenement == null) {
            return;
        }
        boolean wasFavorite = mesFavorisIds.contains(evenement.getId());
        if (wasFavorite) {
            favoriService.retirerFavori(currentUserId, evenement.getId());
        } else {
            favoriService.ajouterFavori(currentUserId, evenement.getId());
        }
        loadFavorisState();
        refreshCards();
        notifyEventsChanged();
    }

    private void openParticiper(Evenement evenement) {
        if (evenement == null) {
            return;
        }
        ParticipationFormView formView = new ParticipationFormView(null, evenement.getId(), evenement.getTitre());
        Stage stage = new Stage();
        formView.show(stage, () -> {
            loadParticipantsState();
            refreshCards();
            notifyEventsChanged();
        });
    }

    private void openCreerEvenement() {
        EvenementFormView formView = new EvenementFormView(null);
        Stage stage = new Stage();
        formView.show(stage, this::loadData);
    }

    private void openVoirEvenement(Evenement evenement) {
        EvenementDetailsView detailsView = new EvenementDetailsView(evenement);
        Stage stage = new Stage();
        detailsView.show(stage);
    }

    private void openModifierEvenement(Evenement evenement) {
        EvenementFormView formView = new EvenementFormView(evenement);
        Stage stage = new Stage();
        formView.show(stage, this::loadData);
    }

    private void openSupprimerEvenement(Evenement evenement) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cet événement ?");
        confirmation.setContentText(evenement != null ? cleanText(evenement.getTitre(), "") : "");

        ButtonType btnOui = new ButtonType("Supprimer");
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnOui, btnAnnuler);

        confirmation.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                try {
                    evenementService.supprimer(evenement.getId());
                    loadData();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur : " + ex.getMessage());
                }
            }
        });
    }

    private void loadData() {
        loadFavorisState();
        loadParticipantsState();

        evenementList.setAll(evenementService.afficherAll());
        refreshTypeChoices();

        applyFilters();
        notifyEventsChanged();
    }

    private void loadFavorisState() {
        favorisCountMap = favoriService.getFavorisCountMap();
        List<Evenement> mesFavoris = favoriService.getMesFavoris(currentUserId);
        mesFavorisIds = mesFavoris.stream().map(Evenement::getId).collect(Collectors.toSet());
    }

    private void loadParticipantsState() {
        participantsCountMap = participationService.getParticipantsCountMap();
    }

    private void refreshTypeChoices() {
        if (typeFilter == null) {
            return;
        }
        String current = typeFilter.getValue();
        Set<String> types = evenementList.stream()
                .map(Evenement::getType)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());

        typeFilter.getItems().setAll("Tous les types");
        typeFilter.getItems().addAll(types.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList()));

        if (current != null && typeFilter.getItems().contains(current)) {
            typeFilter.setValue(current);
        } else {
            typeFilter.setValue("Tous les types");
        }
    }

    private void notifyEventsChanged() {
        if (onEventsChanged != null) {
            onEventsChanged.run();
        }
    }

    public void setOnEventsChanged(Runnable onEventsChanged) {
        this.onEventsChanged = onEventsChanged;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getView() {
        // When navigating back to this page, force the ScrollPane to start at the top
        // so the header + filters are visible (Symfony-like page layout).
        scrollToTop();
        return mainLayout;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String cleanText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String v = value.trim();
        return v.isEmpty() ? fallback : v;
    }

    private String ellipsize(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() <= maxChars) {
            return v;
        }
        return v.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("fermé") || v.equals("ferme")) {
            return "ferme";
        }
        if (v.equals("planifié") || v.equals("planifie")) {
            return "planifie";
        }
        if (v.equals("annulé") || v.equals("annule")) {
            return "annule";
        }
        return v;
    }

    private Label makeIconLabel(String text) {
        Label icon = new Label(text);
        icon.getStyleClass().add("btn-icon");
        return icon;
    }
}
