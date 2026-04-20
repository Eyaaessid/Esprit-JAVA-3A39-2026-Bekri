package tn.esprit.user.ui;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

public class UserDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleSubtitleLabel;

    @FXML
    private void initialize() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";
        welcomeLabel.setText("Bonjour, " + prenom + " " + nom + " 👋");
        roleSubtitleLabel.setText("Votre rôle : " + formatRole(user.getRole()));
    }

    @FXML
    private void handleAccueil() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleObjectifs() {
        try {
            SceneManager.switchTo("objectifs");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleProfil() {
        try {
            SceneManager.switchTo("profile");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleTest() {
        try {
            SceneManager.switchTo("test");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleProfilPsy() {
        try {
            SceneManager.switchTo("profil-psychologique");
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

    private static String formatRole(UtilisateurRole role) {
        if (role == null) {
            return "—";
        }
        return switch (role) {
            case USER -> "Utilisateur";
            case COACH -> "Coach";
            case ADMIN -> "Administrateur";
        };
    }
}
