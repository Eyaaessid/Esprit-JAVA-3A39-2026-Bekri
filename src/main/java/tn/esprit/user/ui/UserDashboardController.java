package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.shared.CommunityNavigation;
import tn.esprit.session.SessionManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.utils.WeatherData;
import tn.esprit.utils.WeatherInsightHelper;
import tn.esprit.utils.WeatherService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class UserDashboardController implements Initializable {

    // ── Welcome section ──────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label roleSubtitleLabel;

    // ── Weather card nodes ───────────────────────────────────────────────────
    @FXML private VBox  weatherCardBox;
    @FXML private Label lblWeatherEmoji;
    @FXML private Label lblWeatherTitle;
    @FXML private Label lblWeatherSummary;
    @FXML private Label lblWeatherBadge;
    @FXML private Label lblTemp;
    @FXML private Label lblHumidity;
    @FXML private Label lblWind;
    @FXML private VBox  weatherTipsContainer;
    @FXML private Label lblWeatherStatus;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Welcome message
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            String prenom = user.getPrenom() != null ? user.getPrenom() : user.getNom();
            if (welcomeLabel      != null) welcomeLabel.setText("Bonjour, " + prenom + " 👋");
            if (roleSubtitleLabel != null) {
                boolean isAdmin = user.getRole() != null
                        && user.getRole().toString().equalsIgnoreCase("ADMIN");
                roleSubtitleLabel.setText(isAdmin ? "Administrateur" : "");
            }
        }

        // Today's date in French
        if (dateLabel != null) {
            LocalDate today   = LocalDate.now();
            String    dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            String    date    = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
            dateLabel.setText(
                    dayName.substring(0, 1).toUpperCase() + dayName.substring(1) + " " + date);
        }

        // Weather — async, never blocks UI
        loadWeatherAsync();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  WEATHER
    // ═════════════════════════════════════════════════════════════════════════

    private void loadWeatherAsync() {
        showWeatherStatus("Chargement de la météo...", false);
        if (weatherCardBox != null) weatherCardBox.getStyleClass().add("weather-loading");

        CompletableFuture
                .supplyAsync(() -> new WeatherService().fetchCurrentWeather())
                .thenAccept(weather -> Platform.runLater(() -> populateWeatherCard(weather)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (weatherCardBox != null) weatherCardBox.getStyleClass().remove("weather-loading");
                        showWeatherStatus("⚠️ Météo indisponible. Vérifiez votre connexion.", true);
                    });
                    return null;
                });
    }

    private void populateWeatherCard(WeatherData weather) {
        // Remove loading dim
        if (weatherCardBox != null) weatherCardBox.getStyleClass().remove("weather-loading");
        hideWeatherStatus();

        if (!weather.isFetchSuccess()) {
            showWeatherStatus(
                    "⚠️ Clé API manquante ou invalide — ajoutez openweather.api.key dans config.properties.",
                    true);
            return;
        }

        // ── Emoji ─────────────────────────────────────────────────────────────
        if (lblWeatherEmoji != null) lblWeatherEmoji.setText(weather.getEmoji());

        // ── Title & summary ───────────────────────────────────────────────────
        if (lblWeatherTitle   != null) lblWeatherTitle.setText("Météo à " + weather.getCity());
        if (lblWeatherSummary != null)
            lblWeatherSummary.setText(WeatherInsightHelper.getWeatherContextLine(weather));

        // ── Condition badge ───────────────────────────────────────────────────
        if (lblWeatherBadge != null) {
            String desc = weather.getDescription();
            // Capitalise first letter
            lblWeatherBadge.setText(desc.isEmpty() ? "" :
                    desc.substring(0, 1).toUpperCase() + desc.substring(1));

            String sc = WeatherInsightHelper.getWeatherStyleClass(weather);
            lblWeatherBadge.getStyleClass().removeIf(c -> c.startsWith("weather-"));
            lblWeatherBadge.getStyleClass().addAll("weather-badge", sc);
        }

        // ── Stat values ───────────────────────────────────────────────────────
        if (lblTemp     != null) lblTemp.setText(String.format("%.0f°C", weather.getTemperatureCelsius()));
        if (lblHumidity != null) lblHumidity.setText(weather.getHumidity() + "%");
        if (lblWind     != null) lblWind.setText(String.format("%.1f m/s", weather.getWindSpeed()));

        // ── Dynamic card background driven by temperature category ────────────
        if (weatherCardBox != null) {
            weatherCardBox.getStyleClass().removeIf(c -> c.startsWith("weather-card-"));
            String condClass = switch (weather.getTemperatureCategory()) {
                case "hot"  -> "weather-card-hot";
                case "warm" -> "weather-card-warm";
                case "cold" -> "weather-card-cold";
                default     -> "weather-card-mild";
            };
            // Also apply rain/storm/snow override based on description
            String desc = weather.getDescription().toLowerCase();
            if (desc.contains("thunder") || desc.contains("storm")) condClass = "weather-card-storm";
            else if (desc.contains("rain") || desc.contains("drizzle"))  condClass = "weather-card-rain";
            else if (desc.contains("snow"))                               condClass = "weather-card-snow";

            weatherCardBox.getStyleClass().add(condClass);
        }

        // ── Tips — each rendered as a styled HBox row ─────────────────────────
        if (weatherTipsContainer != null) {
            weatherTipsContainer.getChildren().clear();
            List<String> tips = WeatherInsightHelper.generateTips(weather, -1, -1, -1);
            for (String tip : tips) {
                weatherTipsContainer.getChildren().add(buildTipRow(tip));
            }
        }
    }

    /**
     * Builds a styled tip row: pill background + bullet icon + wrapped text.
     */
    private HBox buildTipRow(String tip) {
        HBox row = new HBox(10);
        row.getStyleClass().add("weather-tip-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label bullet = new Label("•");
        bullet.getStyleClass().add("weather-tip-icon");
        bullet.setStyle("-fx-text-fill: -bekri-primary; -fx-font-weight: bold; -fx-font-size: 18px;");
        bullet.setMinWidth(20);

        Label text = new Label(tip);
        text.getStyleClass().add("weather-tip-label");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(bullet, text);
        return row;
    }

    // ── Status helpers ────────────────────────────────────────────────────────

    private void showWeatherStatus(String message, boolean isError) {
        if (lblWeatherStatus == null) return;
        lblWeatherStatus.setText(message);
        lblWeatherStatus.setVisible(true);
        lblWeatherStatus.setManaged(true);
        lblWeatherStatus.getStyleClass().removeAll("weather-status-error");
        if (isError) lblWeatherStatus.getStyleClass().add("weather-status-error");
    }

    private void hideWeatherStatus() {
        if (lblWeatherStatus == null) return;
        lblWeatherStatus.setVisible(false);
        lblWeatherStatus.setManaged(false);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SIDEBAR NAVIGATION
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private void handleAccueil(ActionEvent e)        { loadView(stageFrom(e), "/fxml/user-dashboard.fxml"); }
    @FXML private void handleObjectifs(ActionEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckIn(ActionEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlan(ActionEvent e)       { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsights(ActionEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleCommunity(ActionEvent e)      { CommunityNavigation.openPosts(stageFrom(e)); }
    @FXML private void handleChatBot(ActionEvent e)        { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTest(ActionEvent e)           { loadView(stageFrom(e), "/fxml/test.fxml"); }
    @FXML private void handleProfilPsy(ActionEvent e)      { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfil(ActionEvent e)         { loadView(stageFrom(e), "/fxml/profile.fxml"); }
    @FXML private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        loadView(stageFrom(e), "/fxml/login.fxml");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  QUICK-ACCESS CARDS (MouseEvent)
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private void handleObjectifsClick(MouseEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckInClick(MouseEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlanClick(MouseEvent e)       { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsightsClick(MouseEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleCommunityClick(MouseEvent e)      { CommunityNavigation.openPosts(stageFrom(e)); }
    @FXML private void handleChatBotClick(MouseEvent e)        { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTestClick(MouseEvent e)           { loadView(stageFrom(e), "/fxml/test.fxml"); }
    @FXML private void handleProfilPsyClick(MouseEvent e)      { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfilClick(MouseEvent e)         { loadView(stageFrom(e), "/fxml/profile.fxml"); }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private Stage stageFrom(ActionEvent e) { return (Stage) ((Node) e.getSource()).getScene().getWindow(); }
    private Stage stageFrom(MouseEvent  e) { return (Stage) ((Node) e.getSource()).getScene().getWindow(); }

    private void loadView(Stage stage, String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, ex);
        }
    }
}
