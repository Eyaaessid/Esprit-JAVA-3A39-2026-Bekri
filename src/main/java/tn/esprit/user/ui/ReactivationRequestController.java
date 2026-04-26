package tn.esprit.user.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.shared.SceneManager;

import java.io.IOException;

public class ReactivationRequestController {
    @FXML private Label messageLabel;

    @FXML
    private void initialize() {
        if (messageLabel != null) {
            messageLabel.setText("Votre compte a ete desactive par un administrateur. Contactez support@bekri.tn pour le reactiver.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            SceneManager.switchTo("login");
        } catch (IOException e) {
            System.err.println("[ReactivationRequestController.handleBackToLogin] " + e.getMessage());
        }
    }
}
