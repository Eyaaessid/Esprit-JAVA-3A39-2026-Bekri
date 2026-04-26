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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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

    // ── Welcome section ──────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label roleSubtitleLabel;

    // ── Weather card nodes ───────────────────────────────────────
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

    // ── Initialize ───────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set welcome message from session
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            String prenom = user.getPrenom() != null ? user.getPrenom() : user.getNom();
            if (welcomeLabel     != null) welcomeLabel.setText("Bonjour, " + prenom + " 👋");
            if (roleSubtitleLabel != null) {
                boolean isAdmin = user.getRole() != null
                        && user.getRole().toString().equalsIgnoreCase("ADMIN");
                roleSubtitleLabel.setText(isAdmin ? "Administrateur" : "");
            }
        }

        // Set today's date in French
        if (dateLabel != null) {
            LocalDate today = LocalDate.now();
            String dayName = today.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.FRENCH);
            String formatted = today.format(
                    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
            dateLabel.setText(dayName.substring(0, 1).toUpperCase()
                    + dayName.substring(1) + " " + formatted);
        }

        // Load weather asynchronously — never blocks the UI
        loadWeatherAsync();
    }

    // ═══════════════════════════════════════════════════════════════
    //  WEATHER
    // ═══════════════════════════════════════════════════════════════

    private void loadWeatherAsync() {
        showWeatherStatus("Chargement de la météo...", false);
        CompletableFuture
                .supplyAsync(() -> new WeatherService().fetchCurrentWeather())
                .thenAccept(weather -> Platform.runLater(() -> populateWeatherCard(weather)))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            showWeatherStatus("⚠️ Météo indisponible. Vérifiez votre connexion.", true));
                    return null;
                });
    }

    private void populateWeatherCard(WeatherData weather) {
        hideWeatherStatus();
        if (!weather.isFetchSuccess()) {
            showWeatherStatus(
                    "⚠️ Clé API manquante ou invalide — ajoutez openweather.api.key dans config.properties.", true);
            return;
        }
        if (lblWeatherEmoji   != null) lblWeatherEmoji.setText(weather.getEmoji());
        if (lblWeatherTitle   != null) lblWeatherTitle.setText("Météo à " + weather.getCity());
        if (lblWeatherSummary != null)
            lblWeatherSummary.setText(WeatherInsightHelper.getWeatherContextLine(weather));
        if (lblWeatherBadge != null) {
            lblWeatherBadge.setText(weather.getDescription());
            String sc = WeatherInsightHelper.getWeatherStyleClass(weather);
            lblWeatherBadge.getStyleClass().removeIf(c -> c.startsWith("weather-"));
            lblWeatherBadge.getStyleClass().add(sc);
        }
        if (lblTemp     != null) lblTemp.setText(String.format("%.0f°C", weather.getTemperatureCelsius()));
        if (lblHumidity != null) lblHumidity.setText(weather.getHumidity() + "%");
        if (lblWind     != null) lblWind.setText(String.format("%.1f m/s", weather.getWindSpeed()));
        if (weatherTipsContainer != null) {
            weatherTipsContainer.getChildren().clear();
            // -1 = no score context on dashboard (scores only available in weekly insight)
            List<String> tips = WeatherInsightHelper.generateTips(weather, -1, -1, -1);
            for (String tip : tips) {
                Label tipLabel = new Label(tip);
                tipLabel.setWrapText(true);
                tipLabel.getStyleClass().add("weather-tip-label");
                weatherTipsContainer.getChildren().add(tipLabel);
            }
        }
    }

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

    // ═══════════════════════════════════════════════════════════════
    //  SIDEBAR BUTTONS  (onAction → ActionEvent)
    // ═══════════════════════════════════════════════════════════════

    @FXML private void handleAccueil(ActionEvent e) {
        // Already on dashboard — reload it to reset the weather card
        Stage stage = stageFrom(e);
        loadView(stage, "/fxml/user-dashboard.fxml");
    }

    @FXML private void handleObjectifs(ActionEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckIn(ActionEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlan(ActionEvent e)        { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsights(ActionEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleChatBot(ActionEvent e)         { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTest(ActionEvent e)            { loadView(stageFrom(e), "/fxml/test.fxml"); }
    @FXML private void handleProfilPsy(ActionEvent e)       { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfil(ActionEvent e)          { loadView(stageFrom(e), "/fxml/profile.fxml"); }
    @FXML private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        loadView(stageFrom(e), "/fxml/login.fxml");
    }

    // ═══════════════════════════════════════════════════════════════
    //  QUICK-ACCESS CARDS  (onMouseClicked → MouseEvent)
    // ═══════════════════════════════════════════════════════════════

    @FXML private void handleObjectifsClick(MouseEvent e)      { loadView(stageFrom(e), "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckInClick(MouseEvent e)   { loadView(stageFrom(e), "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlanClick(MouseEvent e)        { loadView(stageFrom(e), "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsightsClick(MouseEvent e) { loadView(stageFrom(e), "/fxml/weekly-insight.fxml"); }
    @FXML private void handleChatBotClick(MouseEvent e)         { loadView(stageFrom(e), "/fxml/chat-coach.fxml"); }
    @FXML private void handleTestClick(MouseEvent e)            { loadView(stageFrom(e), "/fxml/test.fxml"); }
    @FXML private void handleProfilPsyClick(MouseEvent e)       { loadView(stageFrom(e), "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfilClick(MouseEvent e)          { loadView(stageFrom(e), "/fxml/profile.fxml"); }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Stage stageFrom(ActionEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }

    private Stage stageFrom(MouseEvent e) {
        return (Stage) ((Node) e.getSource()).getScene().getWindow();
    }

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