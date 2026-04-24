package tn.esprit.user.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.exception.ReactivationException;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.user.service.EmailService;
import tn.esprit.user.dao.ReactivationRequestDao;
import tn.esprit.user.dao.UtilisateurDao;

public class SelfReactivationCodeController {
    @FXML private Label infoLabel;
    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private Label statusLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Hyperlink backToLoginLink;

    private final AccountStatusService accountStatusService = new AccountStatusService(
            new UtilisateurDao(),
            new ReactivationRequestDao(),
            new EmailService()
    );

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    public void setEmail(String email) {
        emailField.setText(email == null ? "" : email.trim());
        infoLabel.setText("Un code de reactivation a 6 chiffres a ete envoye a " + emailField.getText() + ".");
    }

    @FXML
    private void handleVerify() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String token = tokenField.getText() == null ? "" : tokenField.getText().trim();

        verifyButton.setDisable(true);
        runAsync(() -> {
            try {
                accountStatusService.reactivateWithCode(email, token);
                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    showSuccess("Compte reactive. Vous pouvez maintenant vous connecter.");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1800);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                        Platform.runLater(() -> {
                            try {
                                SceneManager.switchTo("login");
                            } catch (Exception ignored) {
                            }
                        });
                    }).start();
                });
            } catch (ReactivationException e) {
                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                System.err.println("[SelfReactivationCodeController.handleVerify] email=" + email + " " + e.getMessage());
                e.printStackTrace(System.err);
                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    showError("Impossible de verifier ce code pour le moment.");
                });
            }
        });
    }

    @FXML
    private void handleResend() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        resendButton.setDisable(true);
        startCooldown(resendButton, 30);
        runAsync(() -> {
            try {
                accountStatusService.sendSelfReactivationCode(email);
                Platform.runLater(() -> showSuccess("Un nouveau code a ete envoye."));
            } catch (ReactivationException e) {
                Platform.runLater(() -> {
                    resendButton.setDisable(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                System.err.println("[SelfReactivationCodeController.handleResend] email=" + email + " " + e.getMessage());
                e.printStackTrace(System.err);
                Platform.runLater(() -> {
                    resendButton.setDisable(false);
                    showError("Impossible de renvoyer le code.");
                });
            }
        });
    }

    @FXML
    private void goToLogin() {
        try {
            SceneManager.switchTo("login");
        } catch (Exception ignored) {
        }
    }

    private void startCooldown(Button button, int seconds) {
        final int[] remaining = { seconds };
        String original = "Renvoyer le code";
        button.setText("Renvoyer (" + remaining[0] + "s)");
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                button.setText(original);
                button.setDisable(false);
            } else {
                button.setText("Renvoyer (" + remaining[0] + "s)");
            }
        }));
        timeline.setCycleCount(seconds);
        timeline.play();
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #dc2626;");
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #217693;");
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void runAsync(Runnable runnable) {
        Thread thread = new Thread(runnable, "self-reactivation-code-task");
        thread.setDaemon(true);
        thread.start();
    }
}
