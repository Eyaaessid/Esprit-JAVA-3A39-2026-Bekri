package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.service.UtilisateurService;

public class ResetPasswordController {

    @FXML private TextField tokenField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button resetButton;
    @FXML private Hyperlink backToLoginLink;

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    public void setToken(String token) {
        if (tokenField != null) {
            tokenField.setText(token == null ? "" : token.trim());
        }
    }

    @FXML
    private void handleReset() {
        String token = tokenField.getText() == null ? "" : tokenField.getText().trim();
        String p1 = passwordField.getText() == null ? "" : passwordField.getText();
        String p2 = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (token.isBlank()) {
            showError("Veuillez entrer le code.");
            return;
        }
        if (p1.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }
        if (!p1.equals(p2)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        resetButton.setDisable(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        new Thread(() -> {
            UtilisateurService.ResetResult result = utilisateurService.resetPassword(token, p1);
            Platform.runLater(() -> {
                resetButton.setDisable(false);
                switch (result) {
                    case SUCCESS -> {
                        statusLabel.setStyle("-fx-text-fill: #16a34a;");
                        statusLabel.setText("Mot de passe réinitialisé ! Redirection...");
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try {
                                    SceneManager.switchTo("login");
                                } catch (Exception ignored) {
                                }
                            });
                        }).start();
                    }
                    case EXPIRED_TOKEN -> showError("Ce code a expiré. Veuillez en demander un nouveau.");
                    case INVALID_TOKEN -> showError("Code invalide. Vérifiez l'email et réessayez.");
                    case WEAK_PASSWORD -> showError("Le mot de passe doit contenir au moins 6 caractères.");
                }
            });
        }).start();
    }

    @FXML
    private void goToLogin() {
        try {
            SceneManager.switchTo("login");
        } catch (Exception ignored) {
        }
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #dc2626;");
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}

