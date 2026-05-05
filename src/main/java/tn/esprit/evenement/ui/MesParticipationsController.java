package tn.esprit.evenement.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.evenement.entity.ParticipationDisplay;
import tn.esprit.evenement.service.ParticipationService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MesParticipationsController implements Initializable {
    @FXML private FlowPane cardsContainer;
    private final ParticipationService participationService = new ParticipationService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refresh();
    }

    @FXML private void handleEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("evenements-list"); }
    @FXML private void handleAccueil(ActionEvent e) throws Exception { SceneManager.switchTo("user-dashboard"); }
    @FXML private void handleLogout(ActionEvent e) throws Exception { SessionManager.getInstance().logout(); SceneManager.switchTo("login"); }
    @FXML private void handleMesParticipations(ActionEvent e) throws Exception { SceneManager.switchTo("mes-participations"); }
    @FXML private void handleMesFavoris(ActionEvent e) throws Exception { SceneManager.switchTo("mes-favoris"); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { SceneManager.switchTo("coach-dashboard"); }

    private void refresh() {
        cardsContainer.getChildren().clear();
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        for (ParticipationDisplay d : participationService.afficherAllEnriched(userId)) {
            VBox card = new VBox(10);
            card.getStyleClass().addAll("card", "obj-card");
            card.setPrefWidth(320);
            VBox top = new VBox(8);
            top.getStyleClass().add("obj-card-top");
            Label title = new Label(d.getEvenement().getTitre());
            title.getStyleClass().add("section-title");
            Label type = new Label(d.getEvenement().getType());
            type.getStyleClass().addAll("chip", "chip-type");
            Label st = new Label(d.getStatutParticipation());
            st.getStyleClass().addAll("badge", statusClass(d.getStatutParticipation()));
            Label date = new Label("Inscrit le " + (d.getDateInscription() == null ? "-" : d.getDateInscription().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            date.getStyleClass().add("muted");
            top.getChildren().addAll(title, type, st, date);

            HBox actions = new HBox(10);
            actions.getStyleClass().add("obj-card-actions");
            actions.setAlignment(Pos.CENTER_LEFT);
            Button mod = new Button("Modifier");
            mod.getStyleClass().add("btn-ghost");
            mod.setOnAction(evt -> ParticipationFormController.openModal(d.getEvenement(), d.getParticipationId(), this::refresh));
            Button del = new Button("Annuler");
            del.getStyleClass().add("btn-danger");
            del.setOnAction(evt -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Annuler cette participation ?", ButtonType.YES, ButtonType.NO);
                a.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        participationService.supprimer(d.getParticipationId());
                        refresh();
                    }
                });
            });
            actions.getChildren().addAll(mod, del);
            card.getChildren().addAll(top, actions);
            cardsContainer.getChildren().add(card);
        }
    }

    private String statusClass(String s) {
        String v = s == null ? "" : s.toLowerCase();
        if (v.contains("présent") || v.contains("confirm")) return "badge-success";
        if (v.contains("attente")) return "badge-warning";
        return "badge-danger";
    }
}
