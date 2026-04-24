package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.ReactivationRequestDao;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.exception.ReactivationException;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.user.service.EmailService;

import java.io.IOException;
import java.util.regex.Pattern;

public class ReactivationRequestController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @FXML private TextField emailField;
    @FXML private TextArea reasonArea;
    @FXML private Label charCountLabel;
    @FXML private Label statusLabel;
    @FXML private Button submitButton;
    @FXML private Button returnButton;

    private final AccountStatusService accountStatusService = new AccountStatusService(
            new UtilisateurDao(),
            new ReactivationRequestDao(),
            new EmailService()
    );

    @FXML
    private void initialize() {
        reasonArea.textProperty().addListener((obs, oldValue, newValue) ->
                charCountLabel.setText((newValue == null ? 0 : newValue.length()) + " caracteres"));
        charCountLabel.setText("0 caracteres");
        returnButton.setVisible(true);
        returnButton.setManaged(true);
    }

    public void setPrefilledEmail(String email) {
        emailField.setText(email);
    }

    @FXML
    private void handleSubmit() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String reason = reasonArea.getText() == null ? "" : reasonArea.getText().trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showStatus("Veuillez saisir une adresse email valide.", true);
            return;
        }
        if (reason.length() < 10) {
            showStatus("Veuillez saisir une raison d'au moins 10 caracteres.", true);
            return;
        }

        submitButton.setDisable(true);
        runAsync(() -> {
            try {
                accountStatusService.submitReactivationRequest(email, reason);
                Platform.runLater(() -> {
                    emailField.setDisable(true);
                    reasonArea.setDisable(true);
                    showStatus("Votre demande a ete envoyee avec succes. Un administrateur l'examinera prochainement.", false);
                    returnButton.setVisible(true);
                    returnButton.setManaged(true);
                });
            } catch (ReactivationException e) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showStatus(e.getMessage(), true);
                });
            } catch (Exception e) {
                System.err.println("[ReactivationRequestController.handleSubmit] email=" + email + " " + e.getMessage());
                e.printStackTrace(System.err);
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showStatus("Une erreur est survenue lors de l'envoi de votre demande.", true);
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

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#dc3545" : "#217693") + ";");
    }

    private void runAsync(Runnable runnable) {
        Thread thread = new Thread(runnable, "reactivation-request-task");
        thread.setDaemon(true);
        thread.start();
    }
}
