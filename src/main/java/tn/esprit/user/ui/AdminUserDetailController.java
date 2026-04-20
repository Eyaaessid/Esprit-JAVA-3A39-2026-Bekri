package tn.esprit.user.ui;

import tn.esprit.user.entity.Utilisateur;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.service.UtilisateurService;
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

    private Utilisateur currentUser;
    private final UtilisateurService utilisateurService = new UtilisateurService();

    public void setUser(Utilisateur user) {
        this.currentUser = user;
        populate(user);
    }

    private void populate(Utilisateur u) {
        avatarLabel.setText(u.getInitials());
        nameCard.setText(u.getFullName());

        roleCard.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        roleCard.setText(capitalize(u.getRoleKey()));
        roleCard.getStyleClass().addAll("role-badge-small", "badge-" + u.getRoleKey());

        String statut = u.getStatutKey() != null && !u.getStatutKey().isEmpty() ? u.getStatutKey() : "—";
        statutCard.getStyleClass().removeIf(c -> c.startsWith("statut-"));
        statutCard.setText(capitalize(statut));
        statutCard.getStyleClass().addAll("statut-badge", "statut-" + statut.toLowerCase());

        subtitleLabel.setText(u.getEmail());
        prenomLabel.setText(nvl(u.getPrenom()));
        nomLabel.setText(nvl(u.getNom()));
        emailLabel.setText(nvl(u.getEmail()));
        telLabel.setText(nvl(u.getTelephone()));
        dateNaissanceLabel.setText(u.getDateNaissance() != null ? u.getDateNaissance().toString() : "—");
        roleLabel.setText(capitalize(u.getRoleKey()));
        statutLabel.setText(capitalize(statut));
        createdAtLabel.setText(u.getCreatedAt() != null
                ? u.getCreatedAt().toLocalDate().toString() : "—");
    }

    @FXML
    private void handleEdit() {
        try {
            AdminEditUserController ctrl =
                    SceneManager.switchToAndGetController("admin-edit-user");
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
                utilisateurService.deleteUser(currentUser.getId());
                Platform.runLater(() -> {
                    try {
                        SceneManager.switchTo("admin-users");
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
            SceneManager.switchTo("admin-users");
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
