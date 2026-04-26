package tn.esprit.user.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.TwoFactorDAO;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.service.TwoFactorService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TwoFactorSetupController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final TwoFactorService twoFactorService = new TwoFactorService();
    private final TwoFactorDAO twoFactorDAO = new TwoFactorDAO();

    @FXML private ImageView qrCodeImageView;
    @FXML private Label secretLabel;
    @FXML private TextField verificationCodeField;
    @FXML private PasswordField currentPasswordField;
    @FXML private Label statusLabel;

    private Utilisateur user;

    @FXML
    private void initialize() {
        verificationCodeField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,6}") ? change : null
        ));
        user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            showStatus("Session invalide. Veuillez vous reconnecter.", true);
            return;
        }
        try {
            if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
                String secret = twoFactorService.generateSecret();
                twoFactorDAO.saveTotpSecret(user.getId(), secret);
                user.setTotpSecret(secret);
            }
            renderQr();
        } catch (Exception e) {
            showStatus("Impossible de préparer la configuration 2FA.", true);
        }
    }

    @FXML
    private void handleVerifyAndEnable() {
        String code = verificationCodeField.getText() != null ? verificationCodeField.getText().trim() : "";
        String password = currentPasswordField.getText() != null ? currentPasswordField.getText() : "";

        if (!code.matches("\\d{6}")) {
            showStatus("Le code de vérification doit contenir 6 chiffres.", true);
            return;
        }
        if (password.isBlank()) {
            showStatus("Veuillez confirmer votre mot de passe.", true);
            return;
        }
        if (!BCrypt.checkpw(password, user.getMotDePasse())) {
            showStatus("Mot de passe incorrect.", true);
            return;
        }
        if (!twoFactorService.verifyCode(user.getTotpSecret(), code)) {
            showStatus("Code TOTP invalide. Réessayez.", true);
            return;
        }

        try {
            List<String> plainCodes = twoFactorService.generateBackupCodes();
            List<String> hashes = twoFactorService.hashBackupCodes(plainCodes);
            String backupCodesJson = OBJECT_MAPPER.writeValueAsString(hashes);
            twoFactorDAO.enableTwoFactor(user.getId(), backupCodesJson);

            user.setTwoFactorEnabled(true);
            user.setBackupCodes(backupCodesJson);
            user.setTwoFactorEnabledAt(java.time.LocalDateTime.now());
            SessionManager.getInstance().setCurrentUser(user);

            TwoFactorBackupCodesController.setPendingBackupCodes(plainCodes);
            TwoFactorBackupCodesController ctrl = SceneManager.switchToAndGetController("two-factor-backup-codes");
            ctrl.setSourceUser(user);
        } catch (Exception e) {
            showStatus("Erreur lors de l'activation de la double authentification.", true);
        }
    }

    @FXML
    private void handleCancel() {
        navigateToProfile();
    }

    private void renderQr() {
        String uri = twoFactorService.getQrCodeUri(user);
        qrCodeImageView.setImage(twoFactorService.generateQrCodeImage(uri, 250));
        secretLabel.setText(user.getTotpSecret());
        if (user.getTwoFactorEnabledAt() != null) {
            showStatus("2FA active depuis " + user.getTwoFactorEnabledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), false);
        } else {
            showStatus("Scannez le QR code avec votre application d'authentification.", false);
        }
    }

    private void navigateToProfile() {
        try {
            SceneManager.switchTo("profile");
        } catch (Exception e) {
            showStatus("Navigation impossible vers le profil.", true);
        }
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #0f766e;");
    }
}
