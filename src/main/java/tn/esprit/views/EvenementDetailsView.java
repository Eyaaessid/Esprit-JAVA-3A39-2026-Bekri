package tn.esprit.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.models.Evenement;
import tn.esprit.services.MeteoService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class EvenementDetailsView {

    private final Evenement evenement;
    private final MeteoService meteoService;

    public EvenementDetailsView(Evenement evenement) {
        this.evenement = evenement;
        this.meteoService = new MeteoService();
    }

    public void show(Stage stage) {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Détails de l'événement");

        StackPane root = new StackPane();
        root.setPadding(new Insets(22));

        VBox card = new VBox(0);
        card.getStyleClass().addAll("card", "details-card");
        card.setMaxWidth(760);

        VBox body = new VBox(14);
        body.getStyleClass().add("card-body");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(3);
        Label title = new Label(cleanText(evenement != null ? evenement.getTitre() : null, "Evenement"));
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Détails et météo du lieu");
        subtitle.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titles, spacer);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("details-grid");
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        int row = 0;
        addDetailRow(grid, row++, "ID", String.valueOf(evenement.getId()));
        addDetailRow(grid, row++, "Titre", evenement.getTitre());
        addDetailRow(grid, row++, "Type", evenement.getType());
        addDetailRow(grid, row++, "Statut", evenement.getStatut());
        addDetailRow(grid, row++, "Début", evenement.getDate_debut() != null ? evenement.getDate_debut().format(formatter) : "--");
        addDetailRow(grid, row++, "Fin", evenement.getDate_fin() != null ? evenement.getDate_fin().format(formatter) : "--");
        addDetailRow(grid, row++, "Lieu", evenement.getLieu());
        addDetailRow(grid, row++, "Capacité max", String.valueOf(evenement.getCapacite_max()));
        addDetailRow(grid, row++, "Description", evenement.getDescription());

        VBox meteoBox = createMeteoSection();

        body.getChildren().addAll(header, grid, new Separator(), meteoBox);

        HBox footer = new HBox(10);
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnGoogleCal = new Button("Google Calendar");
        btnGoogleCal.getStyleClass().addAll("btn-primary");
        btnGoogleCal.setOnAction(e -> ouvrirGoogleCalendar());

        Button btnFermer = new Button("Fermer");
        btnFermer.getStyleClass().addAll("btn-muted");
        btnFermer.setOnAction(e -> stage.close());

        footer.getChildren().addAll(btnFermer, btnGoogleCal);

        card.getChildren().addAll(body, footer);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 820, 720);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private VBox createMeteoSection() {
        VBox meteoBox = new VBox(10);
        meteoBox.getStyleClass().addAll("panel", "weather-card");

        Label meteoTitle = new Label("Météo");
        meteoTitle.getStyleClass().add("section-title");

        Label villeLabel = new Label("Lieu: " + cleanText(evenement.getLieu(), "Lieu inconnu"));
        villeLabel.getStyleClass().add("event-meta-line");

        Label tempLabel = new Label("Température: --");
        tempLabel.getStyleClass().add("event-meta-line");

        Label descLabel = new Label("Chargement météo...");
        descLabel.getStyleClass().add("event-meta-line");

        Label conseilLabel = new Label("Récupération des informations météo.");
        conseilLabel.getStyleClass().add("weather-tip");
        conseilLabel.setWrapText(true);

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(26, 26);

        meteoBox.getChildren().addAll(meteoTitle, villeLabel, loading, tempLabel, descLabel, conseilLabel);

        meteoService.getMeteoAsync(evenement.getLieu()).thenAccept(meteo ->
                Platform.runLater(() -> {
                    meteoBox.getChildren().remove(loading);
                    if (meteo == null || !meteo.isDisponible()) {
                        tempLabel.setText("Température: --");
                        descLabel.setText("Météo indisponible");
                        conseilLabel.setText(meteo != null ? meteo.getConseil() : "Erreur lors de la récupération météo.");
                        return;
                    }
                    villeLabel.setText("Lieu: " + meteo.getVille());
                    tempLabel.setText("Température: " + String.format("%.1f", meteo.getTemperature()) + " C");
                    descLabel.setText("Etat: " + meteo.getDescription());
                    conseilLabel.setText(meteo.getConseil());
                })
        ).exceptionally(ex -> {
            Platform.runLater(() -> {
                meteoBox.getChildren().remove(loading);
                tempLabel.setText("Température: --");
                descLabel.setText("Météo indisponible");
                conseilLabel.setText("Impossible de joindre le service météo.");
            });
            return null;
        });

        return meteoBox;
    }

    private void ouvrirGoogleCalendar() {
        try {
            String lien = genererLienGoogleCalendar();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(lien));
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Lien Google Calendar",
                        "Copiez ce lien dans votre navigateur:\n" + lien);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de l'ouverture de Google Calendar.");
        }
    }

    private String genererLienGoogleCalendar() {
        String titre = encode(evenement.getTitre());
        String description = encode(evenement.getDescription());
        String lieu = encode(evenement.getLieu());

        DateTimeFormatter googleFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String dateDebut = evenement.getDate_debut() != null ? evenement.getDate_debut().format(googleFormatter) : "";
        String dateFin = evenement.getDate_fin() != null ? evenement.getDate_fin().format(googleFormatter) : dateDebut;

        return "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                "&text=" + titre +
                "&dates=" + dateDebut + "/" + dateFin +
                "&details=" + description +
                "&location=" + lieu;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("detail-label");

        Label lblValue = new Label(value != null ? value : "");
        lblValue.getStyleClass().add("detail-value");
        lblValue.setWrapText(true);
        lblValue.setMaxWidth(520);

        grid.add(lblLabel, 0, row);
        grid.add(lblValue, 1, row);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String cleanText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String v = value.trim();
        return v.isEmpty() ? fallback : v;
    }
}
