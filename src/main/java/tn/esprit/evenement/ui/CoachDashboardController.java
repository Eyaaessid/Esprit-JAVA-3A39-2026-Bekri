package tn.esprit.evenement.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.evenement.entity.DashboardStats;
import tn.esprit.evenement.service.DashboardService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class CoachDashboardController implements Initializable {
    @FXML private Label lblWelcome;
    @FXML private FlowPane globalStatsContainer;
    @FXML private FlowPane coachStatsContainer;

    private final DashboardService dashboardService = new DashboardService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String prenom = SessionManager.getInstance().getCurrentUser().getPrenom();
        lblWelcome.setText("Bonjour Coach " + (prenom == null ? "" : prenom) + " 👋");
        loadStats();
    }

    @FXML private void handleRefresh(ActionEvent e) { loadStats(); }
    @FXML private void handleAccueil(ActionEvent e) throws Exception { SceneManager.switchTo("user-dashboard"); }
    @FXML private void handleLogout(ActionEvent e) throws Exception { SessionManager.getInstance().logout(); SceneManager.switchTo("login"); }
    @FXML private void handleEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("evenements-list"); }
    @FXML private void handleMesParticipations(ActionEvent e) throws Exception { SceneManager.switchTo("mes-participations"); }
    @FXML private void handleMesFavoris(ActionEvent e) throws Exception { SceneManager.switchTo("mes-favoris"); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { SceneManager.switchTo("coach-dashboard"); }

    private void loadStats() {
        globalStatsContainer.getChildren().clear();
        coachStatsContainer.getChildren().clear();
        DashboardStats s = dashboardService.getStatistiques();
        int coachId = SessionManager.getInstance().getCurrentUser().getId();
        DashboardStats cs = dashboardService.getStatistiquesCoach(coachId);

        globalStatsContainer.getChildren().addAll(
                card("📅", "Total événements", s.getTotalEvenements(), false),
                card("🟢", "Ouverts", s.getTotalOuverts(), false),
                card("🔒", "Fermés", s.getTotalFermes(), false),
                card("🗓️", "Planifiés", s.getTotalPlanifies(), false),
                card("🎫", "Participations", s.getTotalParticipations(), false)
        );
        coachStatsContainer.getChildren().addAll(
                card("⭐", "Mes événements", cs.getTotalMesEvenements(), true),
                card("👥", "Participants (mes events)", cs.getTotalParticipantsMesEvenements(), true)
        );
    }

    private VBox card(String icon, String label, int value, boolean coach) {
        VBox box = new VBox(8);
        box.getStyleClass().addAll("stat-card", "card");
        if (coach) box.getStyleClass().add("stat-card-coach");
        box.setPrefWidth(220);
        box.setPrefHeight(120);
        Label i = new Label(icon); i.setStyle("-fx-font-size: 28px;");
        Label l = new Label(label); l.getStyleClass().add("card-title");
        Label v = new Label(String.valueOf(value)); v.getStyleClass().add("section-title");
        box.getChildren().addAll(i, l, v);
        return box;
    }
}
