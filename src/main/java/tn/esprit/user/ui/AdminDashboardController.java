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
            return;
        }
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";
        welcomeLabel.setText("Bonjour Admin, " + prenom + " " + nom + " 👋");
    }

    @FXML
    private void handleAccueil() {
        try {
            SceneManager.switchTo("admin-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleQuestions() {
        try {
            SceneManager.switchTo("questions");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleUtilisateurs() {
        try {
            SceneManager.switchTo("admin-users");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleProfil() {
        try {
            SceneManager.switchTo("edit-profile");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            SceneManager.switchTo("login");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }
}
