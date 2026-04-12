package com.bekri.controllers;

import com.bekri.models.UtilisateurResponse;
import com.bekri.utils.SceneManager;
import com.bekri.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label prenomLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;
    @FXML private Label dateNaissanceLabel;
    @FXML private Label statutLabel;
    @FXML private Label createdAtLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UtilisateurResponse user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        avatarLabel.setText(user.getInitials());
        fullNameLabel.setText(user.getFullName());
        roleLabel.setText(capitalize(user.getRole()));
        roleLabel.getStyleClass().add("badge-" + user.getRole());

        prenomLabel.setText(nvl(user.getPrenom()));
        nomLabel.setText(nvl(user.getNom()));
        emailLabel.setText(nvl(user.getEmail()));
        telLabel.setText(nvl(user.getTelephone()));
        dateNaissanceLabel.setText(nvl(user.getDateNaissance()));
        statutLabel.setText(user.getStatut() != null ? capitalize(user.getStatut()) : "—");
        createdAtLabel.setText(user.getCreatedAt() != null ? user.getCreatedAt().substring(0, 10) : "—");
    }

    @FXML
    private void goToEditProfile() {
        try {
            SceneManager.getInstance().switchTo("edit-profile");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        try {
            String role = SessionManager.getInstance().getCurrentUser().getRole();
            if ("admin".equalsIgnoreCase(role)) {
                SceneManager.getInstance().switchTo("admin-dashboard");
            } else {
                SceneManager.getInstance().switchTo("user-dashboard");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        try {
            SceneManager.getInstance().switchTo("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
