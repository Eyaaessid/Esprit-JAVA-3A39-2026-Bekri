package com.bekri.controllers;

import com.bekri.models.UtilisateurResponse;
import com.bekri.services.ProfilClient;
import com.bekri.utils.DialogHelper;
import com.bekri.utils.SceneManager;
import com.bekri.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class UserDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label scoreLabel;
    @FXML private Label profilTypeLabel;
    @FXML private Label dateEvalLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label statRole;
    @FXML private Label statStatut;
    @FXML private Label statDate;
    @FXML private HBox coachNotice;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UtilisateurResponse user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        welcomeLabel.setText("Bonjour, " + user.getPrenom() + " !");
        roleLabel.setText(capitalize(user.getRole()));
        roleLabel.getStyleClass().add("badge-" + user.getRole());

        statRole.setText(capitalize(user.getRole()));
        statStatut.setText(user.getStatut() != null ? capitalize(user.getStatut()) : "—");
        statDate.setText(user.getCreatedAt() != null ? user.getCreatedAt().substring(0, 10) : "—");

        if ("coach".equalsIgnoreCase(user.getRole())) {
            coachNotice.setVisible(true);
            coachNotice.setManaged(true);
        }

        new Thread(() -> {
            try {
                ProfilClient.ProfilData profil = ProfilClient.getProfil();
                Platform.runLater(() -> {
                    scoreLabel.setText(profil.scoreGlobal() + " / 100");
                    profilTypeLabel.setText(profil.profilType());
                    dateEvalLabel.setText(profil.dateEvaluation() != null
                            ? profil.dateEvaluation().substring(0, 10) : "—");
                    feedbackLabel.setText(profil.aiFeedback() != null ? profil.aiFeedback() : "");
                });
            } catch (Exception e) {
                // pas encore de profil
            }
        }).start();
    }

    @FXML
    private void goToProfile() {
        try {
            SceneManager.getInstance().switchTo("edit-profile");
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        try {
            SceneManager.getInstance().switchTo("login");
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
