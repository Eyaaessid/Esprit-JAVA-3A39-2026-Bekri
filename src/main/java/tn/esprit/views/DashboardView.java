package tn.esprit.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.models.DashboardStats;
import tn.esprit.services.DashboardService;

import java.util.ArrayList;
import java.util.List;

public class DashboardView {

    private final BorderPane mainLayout;
    private final DashboardService dashboardService;

    private GridPane statsGrid;

    public DashboardView() {
        dashboardService = new DashboardService();

        mainLayout = new BorderPane();

        VBox page = new VBox(14);
        page.setMaxWidth(1400);

        HBox header = createHeader();

        statsGrid = new GridPane();
        statsGrid.getStyleClass().add("stats-grid");
        statsGrid.setHgap(16);
        statsGrid.setVgap(16);

        page.getChildren().addAll(header, statsGrid);
        page.setPadding(new Insets(2, 2, 16, 2));

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        HBox center = new HBox(scrollPane);
        center.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        mainLayout.setCenter(center);

        refreshStats();
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(3);
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue d'ensemble des indicateurs Bekri.");
        subtitle.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().addAll("btn-outline");
        refresh.setOnAction(e -> refreshStats());

        header.getChildren().addAll(titles, spacer, refresh);
        return header;
    }

    public void refreshStats() {
        DashboardStats stats = dashboardService.getStatistiques();
        rebuildGrid(stats);
    }

    private void rebuildGrid(DashboardStats stats) {
        statsGrid.getChildren().clear();

        List<VBox> cards = new ArrayList<>();
        cards.add(createStatCard("EV", "Total evenements", String.valueOf(stats.getTotalEvenements()), "turquoise"));
        cards.add(createStatCard("OK", "Evenements ouverts", String.valueOf(stats.getEvenementsOuverts()), "success"));
        cards.add(createStatCard("FR", "Evenements fermes", String.valueOf(stats.getEvenementsFermes()), "danger"));
        cards.add(createStatCard("PL", "Evenements planifies", String.valueOf(stats.getEvenementsPlanifies()), "blue"));
        cards.add(createStatCard("PT", "Total participations", String.valueOf(stats.getTotalParticipations()), "warning"));
        cards.add(createStatCard("POP", "Evenement populaire", safe(stats.getEvenementPopulaire()), "info"));
        cards.add(createStatCard("TP", "Type le plus demande", safe(stats.getTypePopulaire()), "turquoise"));
        cards.add(createStatCard("TX", "Taux de remplissage moyen", stats.getTauxRemplissageMoyen() + "%", "primary"));
        cards.add(createStatCard("MX", "Meilleur remplissage", safe(stats.getMeilleurTauxRemplissage()), "warning"));

        int columns = 3;
        for (int i = 0; i < cards.size(); i++) {
            VBox card = cards.get(i);
            int col = i % columns;
            int row = i / columns;
            statsGrid.add(card, col, row);
            GridPane.setHgrow(card, Priority.ALWAYS);
        }

        statsGrid.getChildren().forEach(node -> GridPane.setHgrow(node, Priority.ALWAYS));
    }

    private VBox createStatCard(String iconText, String labelText, String valueText, String variant) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "stat-card");
        if (variant != null && !variant.isBlank()) {
            card.getStyleClass().add("stat-" + variant);
        }
        card.setPadding(new Insets(16));
        card.setMinHeight(132);
        card.setPrefHeight(132);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox top = new HBox(10);
        top.getStyleClass().add("stat-top-row");
        top.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.getStyleClass().addAll("stat-icon", "stat-icon-" + (variant == null || variant.isBlank() ? "muted" : variant));

        Label label = new Label(labelText);
        label.getStyleClass().add("stat-label");
        label.setWrapText(true);

        top.getChildren().addAll(icon, label);

        Label value = new Label(valueText);
        value.getStyleClass().add("stat-value");
        value.setWrapText(true);

        card.getChildren().addAll(top, value);
        return card;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    public BorderPane getView() {
        return mainLayout;
    }
}
