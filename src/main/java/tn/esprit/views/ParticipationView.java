package tn.esprit.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.models.ParticipationDisplay;
import tn.esprit.models.ParticipationEvenement;
import tn.esprit.services.ParticipationService;

import java.time.format.DateTimeFormatter;

public class ParticipationView {

    private final VBox mainLayout;

    private final ParticipationService participationService;
    private final ObservableList<ParticipationDisplay> participationList;

    private TilePane cardsGrid;
    private VBox emptyState;
    private Runnable onVoirEvenements;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ParticipationView() {
        participationService = new ParticipationService();
        participationList = FXCollections.observableArrayList();

        mainLayout = new VBox(14);
        mainLayout.setFillWidth(true);

        HBox header = createHeader();
        cardsGrid = createCardsGrid();
        emptyState = createEmptyState();

        VBox content = new VBox(14, header, emptyState, cardsGrid);

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
        Label title = new Label("Mes participations");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Consultez et gérez vos inscriptions.");
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

    private TilePane createCardsGrid() {
        TilePane grid = new TilePane();
        grid.getStyleClass().add("cards-grid");
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(2, 2, 16, 2));
        grid.setPrefTileWidth(420);
        grid.setTileAlignment(Pos.TOP_LEFT);
        return grid;
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(22));
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(220);
        box.setMaxWidth(760);

        Label icon = new Label("CAL");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Aucune participation pour le moment");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Rejoignez un événement pour le retrouver ici.");
        desc.getStyleClass().add("page-subtitle");

        Button btnVoirEvents = new Button("Voir les événements disponibles");
        btnVoirEvents.getStyleClass().addAll("btn-primary");
        btnVoirEvents.setOnAction(e -> {
            if (onVoirEvenements != null) {
                onVoirEvenements.run();
            }
        });

        box.getChildren().addAll(icon, title, desc, btnVoirEvents);
        return box;
    }

    public void refresh() {
        participationList.setAll(participationService.afficherAllEnriched());
        rebuildCards();
    }

    private void rebuildCards() {
        cardsGrid.getChildren().clear();

        boolean hasData = !participationList.isEmpty();
        emptyState.setVisible(!hasData);
        emptyState.setManaged(!hasData);
        cardsGrid.setVisible(hasData);
        cardsGrid.setManaged(hasData);

        if (!hasData) {
            return;
        }

        for (ParticipationDisplay display : participationList) {
            cardsGrid.getChildren().add(createParticipationCard(display));
        }
    }

    private Node createParticipationCard(ParticipationDisplay display) {
        VBox card = new VBox(0);
        card.getStyleClass().addAll("card", "participation-card");
        card.setPrefWidth(420);

        VBox body = new VBox(10);
        body.getStyleClass().add("card-body");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label type = new Label(cleanText(display.getEvenementType(), "Type"));
        type.getStyleClass().addAll("pill", "pill-type");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statut = new Label(cleanText(display.getParticipationStatut(), "statut").toUpperCase());
        statut.getStyleClass().addAll("badge");

        topRow.getChildren().addAll(type, spacer, statut);

        Label title = new Label(cleanText(display.getEvenementTitre(), "Événement"));
        title.getStyleClass().add("event-title");
        title.setWrapText(true);

        String date = display.getEvenementDateDebut() != null ? display.getEvenementDateDebut().format(dateTimeFormatter) : "--";
        Label meta1 = new Label("Date: " + date);
        meta1.getStyleClass().add("event-meta-line");

        Label meta2 = new Label("Lieu: " + cleanText(display.getEvenementLieu(), "--"));
        meta2.getStyleClass().add("event-meta-line");

        Label meta3 = new Label("Capacité: " + display.getEvenementCapaciteMax());
        meta3.getStyleClass().add("event-meta-line");

        body.getChildren().addAll(topRow, title, meta1, meta2, meta3);

        HBox footer = new HBox(10);
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Button btnVoir = new Button("Voir");
        btnVoir.getStyleClass().addAll("btn-outline");
        btnVoir.setOnAction(e -> openVoirParticipation(display.getParticipationOriginal()));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().addAll("btn-outline");
        btnModifier.setOnAction(e -> openModifierParticipation(display.getParticipationOriginal()));

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.getStyleClass().addAll("btn-danger");
        btnAnnuler.setOnAction(e -> openAnnulerParticipation(display.getParticipationOriginal()));

        footer.getChildren().addAll(btnVoir, btnModifier, btnAnnuler);

        card.getChildren().addAll(body, footer);
        return card;
    }

    private void openVoirParticipation(ParticipationEvenement participation) {
        if (participation == null) {
            return;
        }
        ParticipationDetailsView detailsView = new ParticipationDetailsView(participation);
        Stage stage = new Stage();
        detailsView.show(stage);
    }

    private void openModifierParticipation(ParticipationEvenement participation) {
        if (participation == null) {
            return;
        }
        ParticipationFormView formView = new ParticipationFormView(participation);
        Stage stage = new Stage();
        formView.show(stage, this::refresh);
    }

    private void openAnnulerParticipation(ParticipationEvenement participation) {
        if (participation == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Annuler cette participation ?");
        confirmation.setContentText("Participation ID : " + participation.getId());

        ButtonType btnOui = new ButtonType("Oui, annuler");
        ButtonType btnNon = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnOui, btnNon);

        confirmation.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                participationService.supprimer(participation.getId());
                refresh();
            }
        });
    }

    public void setOnVoirEvenements(Runnable onVoirEvenements) {
        this.onVoirEvenements = onVoirEvenements;
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
