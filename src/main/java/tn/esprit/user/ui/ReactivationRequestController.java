package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.exception.ReactivationException;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.utils.EmailService;

import java.io.IOException;

public class ReactivationRequestController {
    @FXML private Label messageLabel;
    @FXML private TextField emailField;
    @FXML private TextArea reasonArea;
    @FXML private Label statusLabel;
    @FXML private Button submitButton;

    private final AccountStatusService accountStatusService =
            new AccountStatusService(new UtilisateurDao(), EmailService.getInstance());

    @FXML
    private void initialize() {
        if (messageLabel != null) {
            messageLabel.setText(
                    "Si votre compte a ete desactive par un administrateur, vous pouvez envoyer une demande de reactivation."
            );
        }
        hideStatus();
    }

    public void setEmail(String email) {
        if (emailField != null && email != null && !email.isBlank()) {
            emailField.setText(email.trim());
        }
    }

    @FXML
    private void handleSubmit() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String reason = reasonArea.getText() == null ? "" : reasonArea.getText().trim();

        submitButton.setDisable(true);
        runAsync(() -> {
            try {
                accountStatusService.submitReactivationRequest(email, reason);
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showSuccess("Votre demande a ete envoyee. Vous recevrez un email apres traitement.");
                    reasonArea.clear();
                });
            } catch (ReactivationException e) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showError("Impossible d'envoyer la demande pour le moment.");
                });
            }
        });
    }

    @FXML
    private void handleBackToLogin() {
        try {
            SceneManager.switchTo("login");
        } catch (IOException e) {
            System.err.println("[ReactivationRequestController.handleBackToLogin] " + e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #b85d49;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #305764;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
            statusLabel.setText("");
        }
    }

    private void runAsync(Runnable runnable) {
        Thread thread = new Thread(runnable, "reactivation-request-task");
        thread.setDaemon(true);
        thread.start();
    }
}
