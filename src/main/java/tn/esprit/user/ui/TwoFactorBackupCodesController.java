package tn.esprit.user.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;

import java.util.ArrayList;
import java.util.List;

public class TwoFactorBackupCodesController {
    private static List<String> pendingBackupCodes = new ArrayList<>();

    @FXML private VBox codesContainer;
    @FXML private Label statusLabel;

    private Utilisateur sourceUser;

    public static void setPendingBackupCodes(List<String> codes) {
        pendingBackupCodes = (codes == null) ? new ArrayList<>() : new ArrayList<>(codes);
    }

    public void setSourceUser(Utilisateur user) {
        this.sourceUser = user;
    }

    @FXML
    private void initialize() {
        renderCodes();
    }

    @FXML
    private void handleCopyAll() {
        String allCodes = String.join("\n", pendingBackupCodes);
        ClipboardContent content = new ClipboardContent();
        content.putString(allCodes);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Codes copiés dans le presse-papiers.");
    }

    @FXML
    private void handleSaved() {
        pendingBackupCodes.clear();
        try {
            if (sourceUser != null && sourceUser.getRole() == UtilisateurRole.ADMIN) {
                SceneManager.switchTo("admin-dashboard");
            } else {
                SceneManager.switchTo("profile");
            }
        } catch (Exception e) {
            statusLabel.setText("Impossible de revenir au profil.");
        }
    }

    private void renderCodes() {
        codesContainer.getChildren().clear();
        for (String code : pendingBackupCodes) {
            Label label = new Label(code);
            label.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");
            codesContainer.getChildren().add(label);
        }
        if (pendingBackupCodes.isEmpty()) {
            statusLabel.setText("Aucun code disponible.");
        }
    }
}
