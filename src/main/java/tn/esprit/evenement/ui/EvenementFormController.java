package tn.esprit.evenement.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.service.EvenementService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneData;
import tn.esprit.shared.SceneManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class EvenementFormController implements Initializable {
    @FXML private Label formTitle;
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpDateDebut;
    @FXML private Spinner<Integer> spinHeureDebut;
    @FXML private Spinner<Integer> spinMinDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private Spinner<Integer> spinHeureFin;
    @FXML private Spinner<Integer> spinMinFin;
    @FXML private TextField txtLienSession;
    @FXML private TextField txtCapacite;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Label errTitre;
    @FXML private Label errGlobal;

    private final EvenementService evenementService = new EvenementService();
    private Evenement editing;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cmbType.getItems().setAll("EVENT", "SESSION");
        cmbStatut.getItems().setAll("OPEN", "CLOSED", "PLANIFIE", "ANNULE", "COMPLET");
        setupSpinner(spinHeureDebut, 0, 23, 9);
        setupSpinner(spinMinDebut, 0, 59, 0);
        setupSpinner(spinHeureFin, 0, 23, 17);
        setupSpinner(spinMinFin, 0, 59, 0);

        editing = SceneData.getInstance().get(Evenement.class);
        SceneData.getInstance().clear();
        if (editing != null) {
            formTitle.setText("Modifier l'événement");
            txtTitre.setText(editing.getTitre());
            txtDescription.setText(editing.getDescription());
            txtLienSession.setText(editing.getLien_session() == null ? "" : editing.getLien_session());
            txtCapacite.setText(String.valueOf(editing.getCapacite_max()));
            cmbType.setValue(editing.getType());
            cmbStatut.setValue(editing.getStatut());
            if (editing.getDate_debut() != null) {
                dpDateDebut.setValue(editing.getDate_debut().toLocalDate());
                spinHeureDebut.getValueFactory().setValue(editing.getDate_debut().getHour());
                spinMinDebut.getValueFactory().setValue(editing.getDate_debut().getMinute());
            }
            if (editing.getDate_fin() != null) {
                dpDateFin.setValue(editing.getDate_fin().toLocalDate());
                spinHeureFin.getValueFactory().setValue(editing.getDate_fin().getHour());
                spinMinFin.getValueFactory().setValue(editing.getDate_fin().getMinute());
            }
        } else {
            formTitle.setText("Créer un événement");
        }
    }

    @FXML
    private void handleRetour(ActionEvent e) throws Exception {
        SceneManager.switchTo("coach-evenements");
    }
    @FXML private void handleAccueil(ActionEvent e) throws Exception { SceneManager.switchTo("user-dashboard"); }
    @FXML private void handleLogout(ActionEvent e) throws Exception { SessionManager.getInstance().logout(); SceneManager.switchTo("login"); }
    @FXML private void handleEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("evenements-list"); }
    @FXML private void handleMesParticipations(ActionEvent e) throws Exception { SceneManager.switchTo("mes-participations"); }
    @FXML private void handleMesFavoris(ActionEvent e) throws Exception { SceneManager.switchTo("mes-favoris"); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { SceneManager.switchTo("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { SceneManager.switchTo("coach-dashboard"); }

    @FXML
    private void handleEnregistrer(ActionEvent e) throws Exception {
        hideErrors();
        String validation = validateForm();
        if (validation != null) {
            showGlobalError(validation);
            return;
        }
        Evenement ev = editing == null ? new Evenement() : editing;
        ev.setTitre(txtTitre.getText().trim());
        ev.setDescription(txtDescription.getText() == null ? "" : txtDescription.getText().trim());
        ev.setLien_session(txtLienSession.getText() == null ? "" : txtLienSession.getText().trim());
        ev.setCapacite_max(Integer.parseInt(txtCapacite.getText().trim()));
        ev.setType(cmbType.getValue());
        ev.setStatut(cmbStatut.getValue());
        ev.setDate_debut(LocalDateTime.of(dpDateDebut.getValue(), java.time.LocalTime.of(spinHeureDebut.getValue(), spinMinDebut.getValue())));
        ev.setDate_fin(LocalDateTime.of(dpDateFin.getValue(), java.time.LocalTime.of(spinHeureFin.getValue(), spinMinFin.getValue())));
        if (editing == null) {
            ev.setCoach_id(SessionManager.getInstance().getCurrentUser().getId());
            evenementService.ajouter(ev);
        } else {
            evenementService.modifier(ev);
        }
        SceneManager.switchTo("coach-evenements");
    }

    private String validateForm() {
        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            errTitre.setText("Le titre est obligatoire.");
            errTitre.setVisible(true);
            errTitre.setManaged(true);
            return "Veuillez corriger les erreurs du formulaire.";
        }
        if (dpDateDebut.getValue() == null || dpDateFin.getValue() == null) return "Les dates début/fin sont obligatoires.";
        if (cmbType.getValue() == null || cmbStatut.getValue() == null) return "Type et statut sont obligatoires.";
        int cap;
        try { cap = Integer.parseInt(txtCapacite.getText().trim()); } catch (Exception ex) { return "Capacité invalide."; }
        if (cap <= 0) return "La capacité doit être strictement positive.";
        LocalDateTime debut = LocalDateTime.of(dpDateDebut.getValue(), java.time.LocalTime.of(spinHeureDebut.getValue(), spinMinDebut.getValue()));
        LocalDateTime fin = LocalDateTime.of(dpDateFin.getValue(), java.time.LocalTime.of(spinHeureFin.getValue(), spinMinFin.getValue()));
        if (!fin.isAfter(debut)) return "La date de fin doit être après la date de début.";
        return null;
    }

    private void setupSpinner(Spinner<Integer> spinner, int min, int max, int initial) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        spinner.setEditable(true);
    }

    private void showGlobalError(String message) {
        errGlobal.setText(message);
        errGlobal.setVisible(true);
        errGlobal.setManaged(true);
    }

    private void hideErrors() {
        errTitre.setVisible(false);
        errTitre.setManaged(false);
        errGlobal.setVisible(false);
        errGlobal.setManaged(false);
    }
}
