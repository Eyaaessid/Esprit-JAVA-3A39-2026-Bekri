package tn.esprit.user.ui;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    private void initialize() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            try { SceneManager.switchTo("login"); } catch (IOException ignored) {}
            return;
        }
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom    = user.getNom()    != null ? user.getNom()    : "";
        welcomeLabel.setText("Bonjour Admin, " + prenom + " " + nom + " 👋");
    }

    @FXML
    private void handleAccueil() {
        navigateTo("admin-dashboard");
    }

    @FXML
    private void handleQuestions() {
        navigateTo("questions");
    }

    @FXML
    private void handleUtilisateurs() {
        navigateTo("admin-users");
    }

    @FXML
    private void handleReactivations() {
        navigateTo("admin-reactivation-requests");
    }

    @FXML
    private void handleProfil() {
        navigateTo("profile");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        navigateTo("login");
    }

    private void navigateTo(String scene) {
        try {
            SceneManager.switchTo(scene);
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }
}