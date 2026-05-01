package tn.esprit.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tn.esprit.models.Evenement;
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

public class FavorisView {
    private static final double EVENT_CARD_WIDTH = 392;
    private static final double EVENT_CARD_HEIGHT = 410;

    private final VBox mainLayout;

    private final FavoriService favoriService;
    private final ParticipationService participationService;

    private final ObservableList<Evenement> favorisList;
    private final ObservableList<Evenement> populairesList;

    private TilePane favorisGrid;
    private TilePane populairesGrid;
    private VBox emptyState;
    private VBox favorisEmptyState;
    private VBox populairesEmptyState;

    private Map<Integer, Integer> favorisCountMap = new HashMap<>();
    private Map<Integer, Integer> participantsCountMap = new HashMap<>();
    private Set<Integer> mesFavorisIds = new HashSet<>();

    private final int currentUserId = 1;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FavorisView() {
        favoriService = new FavoriService();
        participationService = new ParticipationService();

        favorisList = FXCollections.observableArrayList();
        populairesList = FXCollections.observableArrayList();

        mainLayout = new VBox(14);
        mainLayout.setFillWidth(true);

        VBox content = new VBox(16);
        content.setMaxWidth(1400);

        HBox header = createHeader();
        emptyState = createEmptyState();

        VBox sectionFavoris = createSection("Mes favoris", "Événements que vous avez aimés.", true);
        VBox sectionPopulaires = createSection("Populaires", "Les événements les plus appréciés.", false);

        content.getChildren().addAll(header, emptyState, sectionFavoris, sectionPopulaires);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        HBox center = new HBox(scrollPane);
        center.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        mainLayout.getChildren().add(center);
        VBox.setVgrow(center, Priority.ALWAYS);

        refresh();
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(3);
        Label title = new Label("Favoris");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vos événements préférés et les tendances du moment.");
        subtitle.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().addAll("btn-outline");
        refresh.setOnAction(e -> refresh());

        header.getChildren().addAll(titles, spacer, refresh);
        return header;
    }

    private VBox createSection(String titleText, String subtitleText, boolean isFavorisSection) {
        VBox section = new VBox(10);
        section.getStyleClass().add("panel");

        VBox titles = new VBox(2);
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, subtitle);

        TilePane grid = new TilePane();
        grid.getStyleClass().add("cards-grid");
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(2, 2, 2, 2));
        grid.setPrefTileWidth(EVENT_CARD_WIDTH);
        grid.setTileAlignment(Pos.TOP_LEFT);

        VBox sectionEmpty = createSectionEmptyState(
                isFavorisSection
                        ? "Aucun favori enregistre pour le moment."
                        : "Aucun evenement populaire disponible."
        );

        if (isFavorisSection) {
            favorisGrid = grid;
            favorisEmptyState = sectionEmpty;
        } else {
            populairesGrid = grid;
            populairesEmptyState = sectionEmpty;
        }

        section.getChildren().addAll(titles, sectionEmpty, grid);
        return section;
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(22));
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(200);
        box.setMaxWidth(760);

        Label icon = new Label("\u2665");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Aucun favori pour le moment");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Ajoutez un événement à vos favoris pour le retrouver ici.");
        desc.getStyleClass().add("page-subtitle");

        box.getChildren().addAll(icon, title, desc);
        return box;
    }

    private VBox createSectionEmptyState(String text) {
        VBox box = new VBox(6);
        box.getStyleClass().add("section-empty");
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(text);
        title.getStyleClass().add("page-subtitle");

        box.getChildren().add(title);
        return box;
    }

    public void refresh() {
        loadState();

        favorisList.setAll(favoriService.getMesFavoris(currentUserId));
        populairesList.setAll(favoriService.getEvenementsPopulaires(10));
        mesFavorisIds = favorisList.stream().map(Evenement::getId).collect(Collectors.toSet());

        rebuild();
    }

    private void loadState() {
        favorisCountMap = favoriService.getFavorisCountMap();
        participantsCountMap = participationService.getParticipantsCountMap();
    }

    private void rebuild() {
        boolean hasFavoris = !favorisList.isEmpty();
        emptyState.setVisible(!hasFavoris);
        emptyState.setManaged(!hasFavoris);

        if (favorisEmptyState != null) {
            favorisEmptyState.setVisible(!hasFavoris);
            favorisEmptyState.setManaged(!hasFavoris);
        }

        boolean hasPopulaires = !populairesList.isEmpty();
        if (populairesEmptyState != null) {
            populairesEmptyState.setVisible(!hasPopulaires);
            populairesEmptyState.setManaged(!hasPopulaires);
        }

        favorisGrid.getChildren().clear();
        for (Evenement e : favorisList) {
            favorisGrid.getChildren().add(createEventCard(e, true));
        }

        populairesGrid.getChildren().clear();
        for (Evenement e : populairesList) {
            populairesGrid.getChildren().add(createEventCard(e, false));
        }
    }

    private Node createEventCard(Evenement evenement, boolean isFavorisCard) {
        VBox card = new VBox(0);
        card.getStyleClass().addAll("card", "event-card");
        card.setPrefWidth(EVENT_CARD_WIDTH);
        card.setMinHeight(EVENT_CARD_HEIGHT);
        card.setPrefHeight(EVENT_CARD_HEIGHT);
        card.setMaxHeight(EVENT_CARD_HEIGHT);

        StackPane imageBox = createImageBox(evenement);

        VBox body = new VBox(10);
        body.getStyleClass().add("card-body");
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
        heart.setSelected(mesFavorisIds.contains(evenement.getId()));
        heart.setOnAction(e -> toggleFavori(evenement));

        topRow.getChildren().addAll(type, badge, spacer, heart);

        Label title = new Label(cleanText(evenement.getTitre(), "Sans titre"));
        title.getStyleClass().add("event-title");
        title.setWrapText(true);

        Label desc = new Label(ellipsize(cleanText(evenement.getDescription(), ""), 126));
        desc.getStyleClass().add("event-desc");
        desc.setWrapText(true);
        desc.setMinHeight(34);
        desc.setPrefHeight(34);

        String dateDebut = evenement.getDate_debut() != null ? evenement.getDate_debut().format(dateTimeFormatter) : "--";
        Label meta1 = new Label("Début: " + dateDebut);
        meta1.getStyleClass().add("event-meta-line");

        Label meta2 = new Label("Lieu: " + cleanText(evenement.getLieu(), "--"));
        meta2.getStyleClass().add("event-meta-line");

        int participants = participantsCountMap.getOrDefault(evenement.getId(), 0);
        int capacite = Math.max(evenement.getCapacite_max(), 0);
        int restantes = Math.max(capacite - participants, 0);
        Label stats = new Label("Participants: " + participants + " / " + capacite + "   |   Places restantes: " + restantes);
        stats.getStyleClass().add("event-stats");
        if (restantes == 0) {
            stats.getStyleClass().add("event-stats-full");
        }

        int nbFavoris = favorisCountMap.getOrDefault(evenement.getId(), 0);
        Label fav = new Label("Favoris: " + nbFavoris);
        fav.getStyleClass().add("event-favoris-count");

        Region bodySpacer = new Region();
        VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        body.getChildren().addAll(topRow, title, desc, meta1, meta2, stats, fav, bodySpacer);

        HBox footer = new HBox(10);
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Button btnDetails = new Button("Voir d\u00E9tails");
        btnDetails.getStyleClass().addAll("btn-outline", "event-action-button");
        btnDetails.setOnAction(e -> openVoirEvenement(evenement));

        Button btnParticiper = new Button("Participer");
        btnParticiper.getStyleClass().addAll("btn-primary", "event-action-button");
        btnParticiper.setDisable(!canParticipate(evenement, restantes));
        btnParticiper.setOnAction(e -> openParticiper(evenement));

        HBox actions = new HBox(8, btnDetails, btnParticiper);
        actions.getStyleClass().add("event-action-group");
        footer.getChildren().add(actions);

        card.getChildren().addAll(imageBox, body, footer);
        return card;
    }

    private StackPane createImageBox(Evenement evenement) {
        StackPane box = new StackPane();
        box.getStyleClass().add("event-image");
        box.setMinHeight(110);
        box.setPrefHeight(110);
        box.setMaxHeight(110);

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
        VBox placeholder = new VBox(3);
        placeholder.getStyleClass().add("event-image-placeholder-box");
        placeholder.setAlignment(Pos.CENTER);

        Label icon = new Label("IMG");
        icon.getStyleClass().add("event-image-placeholder-icon");

        String typeText = cleanText(evenement != null ? evenement.getType() : null, "Evenement");
        Label text = new Label(typeText);
        text.getStyleClass().add("event-image-placeholder-text");

        placeholder.getChildren().addAll(icon, text);
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

    private void toggleFavori(Evenement evenement) {
        if (evenement == null) {
            return;
        }
        boolean wasFav = mesFavorisIds.contains(evenement.getId());
        if (wasFav) {
            favoriService.retirerFavori(currentUserId, evenement.getId());
        } else {
            favoriService.ajouterFavori(currentUserId, evenement.getId());
        }
        refresh();
    }

    private void openVoirEvenement(Evenement evenement) {
        EvenementDetailsView detailsView = new EvenementDetailsView(evenement);
        Stage stage = new Stage();
        detailsView.show(stage);
    }

    private void openParticiper(Evenement evenement) {
        ParticipationFormView formView = new ParticipationFormView(null, evenement.getId(), evenement.getTitre());
        Stage stage = new Stage();
        formView.show(stage, this::refresh);
    }

    private boolean canParticipate(Evenement evenement, int restantes) {
        if (restantes <= 0) {
            return false;
        }
        String statut = normalizeStatus(evenement != null ? evenement.getStatut() : "");
        return !statut.equals("ferme") && !statut.equals("annule") && !statut.equals("complet");
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

    private String ellipsize(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() <= maxChars) {
            return v;
        }
        return v.substring(0, Math.max(0, maxChars - 1)).trim() + "...";
    }

    public VBox getView() {
        return mainLayout;
    }

    private String cleanText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String v = value.trim();
        return v.isEmpty() ? fallback : v;
    }
}
