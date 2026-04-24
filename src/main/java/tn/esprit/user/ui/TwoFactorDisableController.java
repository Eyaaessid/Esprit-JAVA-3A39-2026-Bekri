package tn.esprit.user.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.TwoFactorDAO;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.service.TwoFactorService;

import java.util.List;

public class TwoFactorDisableController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final TwoFactorDAO twoFactorDAO = new TwoFactorDAO();
    private final TwoFactorService twoFactorService = new TwoFactorService();

    @FXML private Label titleLabel;
    @FXML private Label warningLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button actionButton;

    private String mode = "disable";
    private Utilisateur user;

    @FXML
    private void initialize() {
        this.user = SessionManager.getInstance().getCurrentUser();
        applyModeUi();
    }

    public void setMode(String mode) {
        this.mode = mode == null ? "disable" : mode;
        applyModeUi();
    }

    public void setUser(Utilisateur user) {
        this.user = user;
    }

    @FXML
    private void handleConfirm() {
        if (user == null) {
            showStatus("Session invalide.", true);
            return;
        }
        String password = currentPasswordField.getText() != null ? currentPasswordField.getText() : "";
        if (password.isBlank()) {
            showStatus("Veuillez saisir votre mot de passe.", true);
            return;
        }
        if (!BCrypt.checkpw(password, user.getMotDePasse())) {
            showStatus("Mot de passe incorrect.", true);
            return;
        }
        try {
            if ("regenerate".equals(mode)) {
                List<String> plainCodes = twoFactorService.generateBackupCodes();
                String json = OBJECT_MAPPER.writeValueAsString(twoFactorService.hashBackupCodes(plainCodes));
                twoFactorDAO.updateBackupCodes(user.getId(), json);
                user.setBackupCodes(json);
                SessionManager.getInstance().setCurrentUser(user);
                TwoFactorBackupCodesController.setPendingBackupCodes(plainCodes);
                TwoFactorBackupCodesController ctrl = SceneManager.switchToAndGetController("two-factor-backup-codes");
                ctrl.setSourceUser(user);
                return;
            }

            twoFactorDAO.disableTwoFactor(user.getId());
            user.resetTwoFactorAuth();
            SessionManager.getInstance().setCurrentUser(user);
            showStatus("Double authentification désactivée.", false);
            SceneManager.switchTo("profile");
        } catch (Exception e) {
            showStatus("Action impossible pour le moment.", true);
        }
    }

    @FXML
    private void handleCancel() {
        try {
            SceneManager.switchTo("profile");
        } catch (Exception e) {
            showStatus("Navigation impossible.", true);
        }
    }

    private void applyModeUi() {
        if (warningLabel == null || titleLabel == null || actionButton == null) {
            return;
        }
        if ("regenerate".equals(mode)) {
            titleLabel.setText("Régénérer les codes de secours");
            warningLabel.setText("Confirmez votre mot de passe pour régénérer vos codes de secours.");
            warningLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            actionButton.setText("Régénérer les codes");
            actionButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
        } else {
            titleLabel.setText("Désactivation de l'authentification à deux facteurs");
            warningLabel.setText("Cette action supprimera la protection 2FA de votre compte.");
            warningLabel.setStyle("-fx-text-fill: #b42318; -fx-font-weight: bold;");
            actionButton.setText("Désactiver 2FA");
            actionButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #0f766e;");
    }
}
