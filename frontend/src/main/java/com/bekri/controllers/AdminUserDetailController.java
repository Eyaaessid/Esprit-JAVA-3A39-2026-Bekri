package com.bekri.controllers;

import com.bekri.models.UtilisateurResponse;
import com.bekri.services.ApiClient;
import com.bekri.utils.DialogHelper;
import com.bekri.utils.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminUserDetailController {

    @FXML private Label avatarLabel;
    @FXML private Label nameCard;
    @FXML private Label roleCard;
    @FXML private Label statutCard;
    @FXML private Label subtitleLabel;
    @FXML private Label prenomLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;
    @FXML private Label dateNaissanceLabel;
    @FXML private Label roleLabel;
    @FXML private Label statutLabel;
    @FXML private Label createdAtLabel;

    private UtilisateurResponse currentUser;

    public void setUser(UtilisateurResponse user) {
        this.currentUser = user;
        populate(user);
    }

    private void populate(UtilisateurResponse u) {
        avatarLabel.setText(u.getInitials());
        nameCard.setText(u.getFullName());

        roleCard.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        roleCard.setText(capitalize(u.getRole()));
        roleCard.getStyleClass().addAll("role-badge-small", "badge-" + u.getRole());

        String statut = u.getStatut() != null ? u.getStatut() : "—";
        statutCard.getStyleClass().removeIf(c -> c.startsWith("statut-"));
        statutCard.setText(capitalize(statut));
        statutCard.getStyleClass().addAll("statut-badge", "statut-" + statut.toLowerCase());

        subtitleLabel.setText(u.getEmail());
        prenomLabel.setText(nvl(u.getPrenom()));
        nomLabel.setText(nvl(u.getNom()));
        emailLabel.setText(nvl(u.getEmail()));
        telLabel.setText(nvl(u.getTelephone()));
        dateNaissanceLabel.setText(nvl(u.getDateNaissance()));
        roleLabel.setText(capitalize(u.getRole()));
        statutLabel.setText(capitalize(statut));
        createdAtLabel.setText(u.getCreatedAt() != null ? u.getCreatedAt().substring(0, 10) : "—");
    }

    @FXML
    private void handleEdit() {
        try {
            AdminEditUserController ctrl =
                    SceneManager.getInstance().switchToAndGetController("admin-edit-user");
            ctrl.setUser(currentUser);
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (!DialogHelper.showConfirm("Confirmer la suppression",
                "Supprimer définitivement " + currentUser.getFullName().trim()
                        + " ? Cette action est irréversible.")) {
            return;
        }
        new Thread(() -> {
            try {
                ApiClient.deleteUtilisateurPermanent(currentUser.getId());
                Platform.runLater(() -> {
                    try {
                        SceneManager.getInstance().switchTo("admin-dashboard");
                        DialogHelper.showSuccess("Succès", "Utilisateur supprimé avec succès.");
                    } catch (Exception e) {
                        DialogHelper.showError("Navigation", e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogHelper.showError("Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void goBack() {
        try {
            SceneManager.getInstance().switchTo("admin-dashboard");
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
