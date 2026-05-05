package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.utils.EmailService;
import java.io.IOException;

public class AccountSettingsController {
    @FXML private Label statusBadgeLabel;
    @FXML private Label messageLabel;
    @FXML private Button deactivateButton;

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final AccountStatusService accountStatusService =
            new AccountStatusService(utilisateurDao, EmailService.getInstance());
    private Utilisateur currentUser;

    @FXML
    private void initialize() {
        Utilisateur sessionUser = SessionManager.getInstance().getCurrentUser();
        if (sessionUser == null || sessionUser.getId() == null) {
            return;
        }
        currentUser = utilisateurDao.findById(sessionUser.getId()).orElse(sessionUser);
        renderState();
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchTo("profile");
        } catch (IOException e) {
            System.err.println("[AccountSettingsController.handleBack] " + e.getMessage());
        }
    }

    @FXML
    private void handleDeactivate() {
        if (!DialogHelper.showConfirm(
                "Desactiver mon compte",
                "Votre compte sera desactive et vous serez deconnecte immediatement. Un code de reactivation a 6 chiffres vous sera envoye par email.",
                "Desactiver",
                "Annuler")) {
            return;
        }

        runAsync(() -> {
            try {
                accountStatusService.deactivateAccount(currentUser, "user");
                Platform.runLater(() -> {
                    DialogHelper.showInfo(
                            "Compte desactive",
                            "Votre compte a ete desactive. Verifiez votre email pour recuperer votre code de reactivation."
                    );
                    SessionManager.getInstance().logout();
                    try {
                        SceneManager.switchTo("login");
                    } catch (IOException e) {
                        System.err.println("[AccountSettingsController.handleDeactivate] " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("[AccountSettingsController.handleDeactivate] userId="
                        + currentUser.getId() + " " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    private void renderState() {
        UtilisateurStatut statut = currentUser.getStatut();
        String color = "#6c757d";
        if (statut != null) {
            color = switch (statut) {
                case ACTIF -> "#217693";
                case INACTIF -> "#fd7e14";
                case BLOQUE, SUPPRIME -> "#dc3545";
            };
        }
        statusBadgeLabel.setText(statut != null ? statut.name() : "-");
        statusBadgeLabel.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";"
                + "-fx-background-radius: 14; -fx-padding: 6 14; -fx-font-weight: bold;");

        deactivateButton.setVisible(false);
        deactivateButton.setManaged(false);

        if (statut == UtilisateurStatut.ACTIF) {
            messageLabel.setText("Votre compte est actif.");
            deactivateButton.setVisible(true);
            deactivateButton.setManaged(true);
            return;
        }

        if (statut == UtilisateurStatut.INACTIF) {
            if ("admin".equalsIgnoreCase(currentUser.getDeactivatedBy())) {
                messageLabel.setText("Votre compte a ete desactive par un administrateur. Depuis l'ecran de connexion, vous pouvez soumettre une demande de reactivation.");
            } else {
                messageLabel.setText("Votre compte est inactif. Utilisez votre code de reactivation recu par email depuis l'ecran de connexion.");
            }
            return;
        }

        messageLabel.setText("Votre compte a ete suspendu definitivement. Veuillez contacter support@bekri.tn.");
    }

    private void runAsync(Runnable runnable) {
        Thread thread = new Thread(runnable, "account-settings-task");
        thread.setDaemon(true);
        thread.start();
    }
}
