package tn.esprit.user.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TwoFactorBackupCodesController {
    private static List<String> pendingBackupCodes = new ArrayList<>();

    @FXML private VBox codesContainer;
    @FXML private Label statusLabel;
    @FXML private Button downloadButton;

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
    private void handleDownload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer les codes de secours");
        fileChooser.setInitialFileName("bekri-backup-codes.txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"));
        Stage stage = (Stage) downloadButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("Bekri Wellbeing - Codes de secours 2FA\n");
            fw.write("Conservez ces codes en lieu sûr.\n\n");
            for (String code : pendingBackupCodes) {
                fw.write(code + "\n");
            }
            statusLabel.setText("✓ Codes téléchargés avec succès.");
        } catch (IOException e) {
            statusLabel.setText("Erreur lors du téléchargement.");
        }
    }

    @FXML
    private void handleSaved() {
        pendingBackupCodes.clear();
        try {
            // All roles return to the unified profile screen (no role restriction on security features)
            SceneManager.switchTo("profile");
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
