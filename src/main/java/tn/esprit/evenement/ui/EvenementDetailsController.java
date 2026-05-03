package tn.esprit.evenement.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.service.CalendrierService;
import tn.esprit.evenement.service.EvenementService;
import tn.esprit.evenement.service.FavoriService;
import tn.esprit.evenement.service.MeteoService;
import tn.esprit.evenement.service.ParticipationService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneData;
import tn.esprit.user.shell.UserShellNavigator;
import tn.esprit.user.shell.UserShellRoute;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class EvenementDetailsController implements Initializable {
    @FXML private Label lblTitre;
    @FXML private Label lblType;
    @FXML private Label lblStatut;
    @FXML private Label lblDescription;
    @FXML private Label lblDateDebut;
    @FXML private Label lblDateFin;
    @FXML private Label lblCapacite;
    @FXML private ProgressIndicator meteoLoading;
    @FXML private Label lblMeteoTemp;
    @FXML private Label lblMeteoDesc;
    @FXML private Label lblMeteoConseil;
    @FXML private Button btnParticiper;
    @FXML private Button btnFavori;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final FavoriService favoriService = new FavoriService();
    private final ParticipationService participationService = new ParticipationService();
    private final CalendrierService calendrierService = new CalendrierService();
    private final MeteoService meteoService = new MeteoService();
    private final EvenementService evenementService = new EvenementService();
    private Evenement event;
    private Utilisateur user;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        event = SceneData.getInstance().get(Evenement.class);
        SceneData.getInstance().clear();
        user = SessionManager.getInstance().getCurrentUser();
        if (event == null || user == null) return;
        lblTitre.setText(event.getTitre());
        lblType.setText(event.getType());
        lblStatut.setText(event.getStatut());
        lblDescription.setText(event.getDescription());
        lblDateDebut.setText(fmt(event.getDate_debut()));
        lblDateFin.setText(fmt(event.getDate_fin()));
        lblCapacite.setText(String.valueOf(event.getCapacite_max()));

        boolean isOwnerCoach = user.getRole() == UtilisateurRole.COACH && event.getCoach_id() == user.getId();
        btnModifier.setVisible(isOwnerCoach);
        btnModifier.setManaged(isOwnerCoach);
        btnSupprimer.setVisible(isOwnerCoach);
        btnSupprimer.setManaged(isOwnerCoach);
        updateFavoriButton();

        meteoService.getMeteoAsync("Tunis").thenAccept(m -> Platform.runLater(() -> {
            meteoLoading.setVisible(false);
            meteoLoading.setManaged(false);
            lblMeteoTemp.setText(String.format(Locale.ROOT, "%.1f°C", m.getTemperature()));
            lblMeteoDesc.setText(m.getDescription());
            lblMeteoConseil.setText(m.getConseil());
            lblMeteoTemp.setVisible(true); lblMeteoTemp.setManaged(true);
            lblMeteoDesc.setVisible(true); lblMeteoDesc.setManaged(true);
            lblMeteoConseil.setVisible(true); lblMeteoConseil.setManaged(true);
        }));
    }

    @FXML private void handleRetour(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_LIST); }
    @FXML private void handleAccueil(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.DASHBOARD); }
    @FXML private void handleLogout(ActionEvent e) { UserShellNavigator.logout(); }
    @FXML private void handleEvenements(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_LIST); }
    @FXML private void handleMesParticipations(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_PARTICIPATIONS); }
    @FXML private void handleMesFavoris(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_FAVORIS); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { UserShellNavigator.leaveShellToScene("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { UserShellNavigator.leaveShellToScene("coach-dashboard"); }
    @FXML private void handleParticiper(ActionEvent e) {
        if (!participationService.estDejaInscrit(user.getId(), event.getId())) {
            ParticipationFormController.openModal(event, null, () -> {});
        }
    }
    @FXML private void handleFavori(ActionEvent e) {
        boolean fav = favoriService.estEnFavori(user.getId(), event.getId());
        if (fav) favoriService.retirerFavori(user.getId(), event.getId());
        else favoriService.ajouterFavori(user.getId(), event.getId());
        updateFavoriButton();
    }
    @FXML private void handleGoogleCal(ActionEvent e) {
        try { Desktop.getDesktop().browse(URI.create(calendrierService.genererLienGoogleCalendar(event))); } catch (Exception ignored) {}
    }
    @FXML private void handleModifier(ActionEvent e) throws Exception {
        SceneData.getInstance().set(event);
        UserShellNavigator.leaveShellToScene("evenement-form");
    }
    @FXML private void handleSupprimer(ActionEvent e) throws Exception {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet événement ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                evenementService.supprimer(event.getId());
                try {
                    UserShellNavigator.leaveShellToScene("coach-evenements");
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void updateFavoriButton() {
        boolean fav = favoriService.estEnFavori(user.getId(), event.getId());
        btnFavori.setText(fav ? "❤️  Retirer des favoris" : "🤍  Ajouter aux favoris");
        if (fav && !btnFavori.getStyleClass().contains("btn-favori-active")) btnFavori.getStyleClass().add("btn-favori-active");
        if (!fav) btnFavori.getStyleClass().remove("btn-favori-active");
    }
    private String fmt(java.time.LocalDateTime d) { return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
}
