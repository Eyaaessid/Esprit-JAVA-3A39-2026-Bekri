package tn.esprit.user.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;

public class UserDashboardController {

    @FXML private javafx.scene.layout.BorderPane root; // add fx:id="root" to your BorderPane

    // ── Sidebar buttons (onAction → ActionEvent) ──────────────────────────────
    @FXML private void handleObjectifs(ActionEvent e)      { openViewFromAction(e, "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckIn(ActionEvent e)   { openViewFromAction(e, "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlan(ActionEvent e)        { openViewFromAction(e, "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsights(ActionEvent e) { openViewFromAction(e, "/fxml/weekly-insight.fxml"); }
    @FXML private void handleChatBot(ActionEvent e)         { openViewFromAction(e, "/fxml/chat-coach.fxml"); }
    @FXML private void handleTest(ActionEvent e)            { openViewFromAction(e, "/fxml/test.fxml"); }
    @FXML private void handleProfilPsy(ActionEvent e)       { openViewFromAction(e, "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfil(ActionEvent e)          { openViewFromAction(e, "/fxml/profile.fxml"); }
    @FXML private void handleLogout(ActionEvent e)          { openViewFromAction(e, "/fxml/login.fxml"); }

    // ── Quick-access cards (onMouseClicked → MouseEvent) ──────────────────────
    // Different method names to avoid @FXML overload ambiguity
    @FXML private void handleObjectifsClick(MouseEvent e)      { openViewFromMouse(e, "/fxml/objectifs.fxml"); }
    @FXML private void handleDailyCheckInClick(MouseEvent e)   { openViewFromMouse(e, "/fxml/suivi_today.fxml"); }
    @FXML private void handleWeekPlanClick(MouseEvent e)        { openViewFromMouse(e, "/fxml/plan-weekly.fxml"); }
    @FXML private void handleWeeklyInsightsClick(MouseEvent e) { openViewFromMouse(e, "/fxml/weekly-insight.fxml"); }
    @FXML private void handleChatBotClick(MouseEvent e)         { openViewFromMouse(e, "/fxml/chat-coach.fxml"); }
    @FXML private void handleTestClick(MouseEvent e)            { openViewFromMouse(e, "/fxml/test.fxml"); }
    @FXML private void handleProfilPsyClick(MouseEvent e)       { openViewFromMouse(e, "/fxml/profil-psychologique.fxml"); }
    @FXML private void handleProfilClick(MouseEvent e)          { openViewFromMouse(e, "/fxml/profile.fxml"); }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void openViewFromAction(ActionEvent e, String fxml) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        loadView(stage, fxml);
    }

    private void openViewFromMouse(MouseEvent e, String fxml) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        loadView(stage, fxml);
    }

    private void loadView(Stage stage, String fxmlResource) {
        try {
            URL url = getClass().getResource(fxmlResource);
            if (url == null) {
                throw new IllegalArgumentException("FXML not found: " + fxmlResource);
            }
            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load FXML: " + fxmlResource, ex);
        }
    }
}