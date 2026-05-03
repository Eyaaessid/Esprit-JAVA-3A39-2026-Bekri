package tn.esprit.evenement.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.service.FavoriService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneData;
import tn.esprit.user.shell.UserShellNavigator;
import tn.esprit.user.shell.UserShellRoute;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MesFavorisController implements Initializable {
    @FXML private FlowPane cardsContainer;
    private final FavoriService favoriService = new FavoriService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refresh();
    }

    @FXML private void handleEvenements(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_LIST); }
    @FXML private void handleAccueil(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.DASHBOARD); }
    @FXML private void handleLogout(ActionEvent e) { UserShellNavigator.logout(); }
    @FXML private void handleMesParticipations(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_PARTICIPATIONS); }
    @FXML private void handleMesFavoris(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_FAVORIS); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { UserShellNavigator.leaveShellToScene("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { UserShellNavigator.leaveShellToScene("coach-dashboard"); }

    private void refresh() {
        cardsContainer.getChildren().clear();
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        for (Evenement e : favoriService.getMesFavoris(userId)) {
            VBox card = new VBox(10);
            card.getStyleClass().addAll("card", "obj-card");
            card.setPrefWidth(320);
            VBox top = new VBox(8);
            top.getStyleClass().add("obj-card-top");
            Label title = new Label(e.getTitre());
            title.getStyleClass().add("section-title");
            Label type = new Label(e.getType());
            type.getStyleClass().addAll("chip", "chip-type");
            Label date = new Label("📅 " + (e.getDate_debut() == null ? "-" : e.getDate_debut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            date.getStyleClass().add("muted");
            top.getChildren().addAll(title, type, date);

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.getStyleClass().add("obj-card-actions");
            Button detail = new Button("Détails");
            detail.getStyleClass().add("btn-ghost");
            detail.setOnAction(evt -> {
                SceneData.getInstance().set(e);
                UserShellNavigator.navigate(UserShellRoute.EVENTS_DETAIL);
            });
            Button remove = new Button("💔 Retirer");
            remove.getStyleClass().add("btn-danger");
            remove.setOnAction(evt -> {
                favoriService.retirerFavori(userId, e.getId());
                refresh();
            });
            actions.getChildren().addAll(detail, remove);
            card.getChildren().addAll(top, actions);
            cardsContainer.getChildren().add(card);
        }
    }
}
