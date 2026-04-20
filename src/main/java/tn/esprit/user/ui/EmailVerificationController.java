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
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.service.UtilisateurService;

public class EmailVerificationController {

    @FXML private Label infoLabel;
    @FXML private TextField tokenField;
    @FXML private Label statusLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Hyperlink backToLoginLink;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private Utilisateur user;

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    public void setUser(Utilisateur user) {
        this.user = user;
        if (infoLabel != null) {
            String email = user != null ? user.getEmail() : "";
            infoLabel.setText("Un code de vérification a été envoyé à " + email + ".");
        }

        new Thread(() -> {
            try {
                utilisateurService.sendVerificationEmail(user);
                Platform.runLater(() -> showSuccess("Code envoyé. Vérifiez votre boîte de réception."));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Impossible d'envoyer l'email de vérification. Réessayez."));
            }
        }).start();
    }

    @FXML
    private void handleVerify() {
        String token = tokenField.getText() == null ? "" : tokenField.getText().trim();
        if (token.isBlank()) {
            showError("Veuillez entrer le code.");
            return;
        }

        verifyButton.setDisable(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        new Thread(() -> {
            UtilisateurService.VerifyResult result = utilisateurService.verifyEmail(token);
            Platform.runLater(() -> {
                verifyButton.setDisable(false);
                switch (result) {
                    case SUCCESS -> {
                        showSuccess("Email vérifié ! Vous pouvez maintenant vous connecter.");
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try { SceneManager.switchTo("login"); } catch (Exception ignored) {}
                            });
                        }).start();
                    }
                    case EXPIRED_TOKEN -> {
                        showError("Ce code a expiré. Veuillez en demander un nouveau.");
                        resendButton.setDisable(false);
                    }
                    case INVALID_TOKEN -> showError("Code invalide ou déjà utilisé.");
                }
            });
        }).start();
    }

    @FXML
    private void handleResend() {
        if (user == null) {
            showError("Utilisateur introuvable.");
            return;
        }

        resendButton.setDisable(true);
        startCooldown(resendButton, 30);

        new Thread(() -> {
            try {
                utilisateurService.sendVerificationEmail(user);
                Platform.runLater(() -> showSuccess("Code renvoyé. Vérifiez votre boîte de réception."));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Impossible de renvoyer le code. Réessayez."));
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

    private void startCooldown(Button button, int seconds) {
        final int[] remaining = { seconds };
        String original = button.getText();
        button.setText("Renvoyer (" + remaining[0] + "s)");
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                button.setText(original);
                button.setDisable(false);
            } else {
                button.setText("Renvoyer (" + remaining[0] + "s)");
            }
        }));
        tl.setCycleCount(seconds);
        tl.play();
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #dc2626;");
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        statusLabel.setStyle("-fx-text-fill: #16a34a;");
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}

