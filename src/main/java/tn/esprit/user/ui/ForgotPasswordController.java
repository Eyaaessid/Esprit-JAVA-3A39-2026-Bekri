package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.service.UtilisateurService;
import tn.esprit.utils.EmailValidator;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label emailError;
    @FXML private Button sendButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private Hyperlink backToLoginLink;

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @FXML
    private void initialize() {
        if (emailError != null) {
            emailError.setVisible(false);
            emailError.setManaged(false);
        }
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
        }
    }

    @FXML
    private void handleSend() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        EmailValidator.ValidationResult result = EmailValidator.validate(email);
        if (!result.valid()) {
            String msg = result.errorMessage();
            if (result.suggestion() != null && !result.suggestion().isBlank()) {
                msg = msg + " " + result.suggestion();
            }
            showEmailError(msg);
            return;
        }

        hideEmailError();
        setBusy(true);

        new Thread(() -> {
            try {
                utilisateurService.requestPasswordReset(email);
                Platform.runLater(() -> {
                    setBusy(false);
                    statusLabel.setText("Code envoyé ! Vérifiez votre boîte Mailtrap.");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setVisible(true);
                    statusLabel.setManaged(true);

                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            try {
                                SceneManager.switchTo("reset-password");
                            } catch (Exception ignored) {}
                        });
                    }).start();
                });
            } catch (Exception e) {
                System.err.println("[ForgotPasswordController] ERROR: " + e.getMessage());
                e.printStackTrace(System.err);
                Platform.runLater(() -> {
                    setBusy(false);
                    showEmailError("Erreur lors de l'envoi du code: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        try {
            SceneManager.switchTo("login");
        } catch (Exception ignored) {
        }
    }

    private void setBusy(boolean busy) {
        sendButton.setDisable(busy);
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
    }

    private void showEmailError(String msg) {
        emailError.setText(msg);
        emailError.setVisible(true);
        emailError.setManaged(true);
    }

    private void hideEmailError() {
        emailError.setVisible(false);
        emailError.setManaged(false);
    }
}