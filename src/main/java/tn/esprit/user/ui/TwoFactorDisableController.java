package tn.esprit.user.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
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

    @FXML private Label warningLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private Label statusLabel;

    private boolean regenerateMode;

    @FXML
    private void initialize() {
        updateWarningText();
    }

    public void setRegenerateMode(boolean regenerateMode) {
        this.regenerateMode = regenerateMode;
        updateWarningText();
    }

    @FXML
    private void handleConfirm() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
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
            if (regenerateMode) {
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

    private void updateWarningText() {
        if (warningLabel == null) {
            return;
        }
        warningLabel.setText(regenerateMode
                ? "Confirmez votre mot de passe pour régénérer vos codes de secours."
                : "Cette action supprimera la protection 2FA de votre compte.");
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #0f766e;");
    }
}
