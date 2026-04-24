package tn.esprit.user.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.TwoFactorDAO;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.TwoFactorService;
import tn.esprit.user.service.UtilisateurService;

import java.util.ArrayList;
import java.util.List;

public class TwoFactorLoginController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @FXML private TextField totpCodeField;
    @FXML private TextField backupCodeField;
    @FXML private VBox backupCodeContainer;
    @FXML private Label statusLabel;

    private final TwoFactorService twoFactorService = new TwoFactorService();
    private final TwoFactorDAO twoFactorDAO = new TwoFactorDAO();
    private final UtilisateurService utilisateurService = new UtilisateurService();
    private Utilisateur user;
    private int failedTotpAttempts;

    public void setUser(Utilisateur user) {
        this.user = user;
    }

    @FXML
    private void initialize() {
        totpCodeField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,6}") ? change : null
        ));
        backupCodeField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null
        ));
        if (backupCodeContainer != null) {
            backupCodeContainer.setManaged(false);
            backupCodeContainer.setVisible(false);
        }
    }

    @FXML
    private void handleVerify() {
        if (user == null) {
            showStatus("Session de connexion invalide.", true);
            return;
        }

        boolean backupMode = backupCodeContainer.isVisible();
        if (backupMode) {
            verifyBackupCode();
            return;
        }

        String code = totpCodeField.getText() != null ? totpCodeField.getText().trim() : "";
        if (!code.matches("\\d{6}")) {
            showStatus("Le code doit contenir 6 chiffres.", true);
            return;
        }

        if (twoFactorService.verifyCode(user.getTotpSecret(), code)) {
            completeLogin();
            return;
        }

        failedTotpAttempts++;
        if (failedTotpAttempts >= 5) {
            showStatus("Trop d'échecs TOTP. Utilisez un code de secours.", true);
        } else {
            showStatus("Code TOTP invalide.", true);
        }
    }

    @FXML
    private void handleUseBackupCode() {
        boolean show = !backupCodeContainer.isVisible();
        backupCodeContainer.setVisible(show);
        backupCodeContainer.setManaged(show);
        if (show) {
            showStatus("Entrez un code de secours.", false);
        }
    }

    @FXML
    private void handleCancel() {
        SessionManager.getInstance().clear();
        try {
            SceneManager.switchTo("login");
        } catch (Exception e) {
            showStatus("Navigation impossible.", true);
        }
    }

    private void verifyBackupCode() {
        String entered = backupCodeField.getText() != null ? backupCodeField.getText().trim() : "";
        if (entered.isBlank()) {
            showStatus("Veuillez saisir un code de secours.", true);
            return;
        }
        List<String> hashes = parseBackupCodes(user.getBackupCodes());
        if (!twoFactorService.verifyBackupCode(entered, hashes)) {
            showStatus("Code de secours invalide.", true);
            return;
        }
        List<String> updated = twoFactorService.removeUsedBackupCode(entered, hashes);
        try {
            String updatedJson = OBJECT_MAPPER.writeValueAsString(updated);
            twoFactorDAO.updateBackupCodes(user.getId(), updatedJson);
            user.setBackupCodes(updatedJson);
            completeLogin();
        } catch (Exception e) {
            showStatus("Impossible de mettre à jour les codes de secours.", true);
        }
    }

    private void completeLogin() {
        try {
            if (user.getStatut() == UtilisateurStatut.BLOQUE || user.getStatut() == UtilisateurStatut.SUPPRIME) {
                showStatus("Votre compte a été suspendu définitivement. Veuillez contacter le support.", true);
                return;
            }
            if (user.getStatut() == UtilisateurStatut.INACTIF) {
                showStatus("Votre compte est inactif. Veuillez repasser par l'écran de connexion pour demander une réactivation.", true);
                return;
            }
            SessionManager.getInstance().setCurrentUser(user);
            System.out.println("Logged in as: " + user.getClass().getSimpleName());
            System.out.println("isAdmin: " + SessionManager.getInstance().isAdmin());
            System.out.println("isCoach: " + SessionManager.getInstance().isCoach());
            utilisateurService.updateLastLogin(user.getId());
            if (SessionManager.getInstance().isAdmin()) {
                SceneManager.switchTo("admin-dashboard");
            } else if (SessionManager.getInstance().isCoach()) {
                SceneManager.switchTo("user-dashboard");
            } else {
                SceneManager.switchTo("user-dashboard");
            }
        } catch (Exception e) {
            showStatus("Erreur lors de la finalisation de la connexion.", true);
        }
    }

    private List<String> parseBackupCodes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #0f766e;");
    }
}
