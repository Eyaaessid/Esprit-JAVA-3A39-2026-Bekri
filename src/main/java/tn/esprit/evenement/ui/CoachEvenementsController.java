package tn.esprit.evenement.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.service.EvenementService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneData;
import tn.esprit.shared.SceneManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CoachEvenementsController implements Initializable {
    @FXML private FlowPane cardsContainer;

    private final EvenementService evenementService = new EvenementService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refresh();
    }

    @FXML private void handleCreer(ActionEvent e) throws Exception { SceneManager.switchTo("evenement-form"); }
    @FXML private void handleAccueil(ActionEvent e) throws Exception { SceneManager.switchTo("user-dashboard"); }
    @FXML private void handleLogout(ActionEvent e) throws Exception { SessionManager.getInstance().logout(); SceneManager.switchTo("login"); }
    @FXML private void handleEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("evenements-list"); }
    @FXML private void handleMesParticipations(ActionEvent e) throws Exception { SceneManager.switchTo("mes-participations"); }
    @FXML private void handleMesFavoris(ActionEvent e) throws Exception { SceneManager.switchTo("mes-favoris"); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { SceneManager.switchTo("coach-dashboard"); }

    private void refresh() {
        cardsContainer.getChildren().clear();
        int coachId = SessionManager.getInstance().getCurrentUser().getId();
        for (Evenement e : evenementService.afficherAll()) {
            if (e.getCoach_id() == coachId) cardsContainer.getChildren().add(buildCard(e));
        }
    }

    private VBox buildCard(Evenement e) {
        VBox card = new VBox();
        card.getStyleClass().addAll("card", "obj-card");
        card.setPrefWidth(320);
        VBox top = new VBox(8);
        top.getStyleClass().add("obj-card-top");
        top.getChildren().addAll(
                new Label(e.getType()),
                title(e.getTitre()),
                muted("🔗 " + ((e.getLien_session() == null || e.getLien_session().isBlank()) ? "Aucun lien" : e.getLien_session())),
                muted("📅 " + fmt(e.getDate_debut()))
        );
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("obj-card-actions");
        Button edit = new Button("✏️ Modifier");
        edit.getStyleClass().addAll("btn-icon", "action-btn-edit");
        edit.setOnAction(evt -> {
            try {
                SceneData.getInstance().set(e);
                SceneManager.switchTo("evenement-form");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        Button del = new Button("🗑️ Supprimer");
        del.getStyleClass().addAll("btn-icon", "action-btn-delete");
        del.setOnAction(evt -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression ?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    evenementService.supprimer(e.getId());
                    refresh();
                }
            });
        });
        Button details = new Button("👁️ Détails");
        details.getStyleClass().add("btn-ghost");
        details.setOnAction(evt -> {
            try {
                SceneData.getInstance().set(e);
                SceneManager.switchTo("evenement-details");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        actions.getChildren().addAll(edit, del, details);
        card.getChildren().addAll(top, actions);
        return card;
    }

    private Label title(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("section-title");
        l.setWrapText(true);
        return l;
    }
    private Label muted(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("muted");
        return l;
    }
    private String fmt(java.time.LocalDateTime d) { return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
}
