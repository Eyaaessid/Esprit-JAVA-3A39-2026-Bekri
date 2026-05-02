package tn.esprit.evenement.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.entity.ParticipationEvenement;
import tn.esprit.evenement.service.ParticipationService;
import tn.esprit.session.SessionManager;

import java.time.LocalDateTime;

public class ParticipationFormController {
    @FXML private ComboBox<String> cmbStatut;

    private final ParticipationService participationService = new ParticipationService();
    private Evenement evenement;
    private Integer participationId;
    private Runnable onSaved;

    @FXML
    private void initialize() {
        cmbStatut.getItems().setAll("INSCRIT", "ANNULE");
        cmbStatut.getSelectionModel().select("INSCRIT");
    }

    public void setData(Evenement evenement, Integer participationId, Runnable onSaved) {
        this.evenement = evenement;
        this.participationId = participationId;
        this.onSaved = onSaved;
    }

    @FXML
    private void handleEnregistrer() {
        if (participationId == null) {
            ParticipationEvenement p = new ParticipationEvenement();
            p.setUtilisateur_id(SessionManager.getInstance().getCurrentUser().getId());
            p.setEvenement_id(evenement.getId());
            p.setDate_inscription(LocalDateTime.now());
            p.setStatut(cmbStatut.getValue());
            participationService.ajouter(p);
        } else {
            ParticipationEvenement p = new ParticipationEvenement();
            p.setId(participationId);
            p.setStatut(cmbStatut.getValue());
            participationService.modifier(p);
        }
        close();
        if (onSaved != null) onSaved.run();
    }

    @FXML
    private void handleAnnuler() {
        close();
    }

    private void close() {
        Stage stage = (Stage) cmbStatut.getScene().getWindow();
        stage.close();
    }

    public static void openModal(Evenement e, Integer participationId, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(ParticipationFormController.class.getResource("/fxml/participation-form.fxml"));
            Parent root = loader.load();
            ParticipationFormController c = loader.getController();
            c.setData(e, participationId, onSaved);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Participation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
