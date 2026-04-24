package tn.esprit.user.ui;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.util.StringConverter;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.UtilisateurService;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminUserEditController implements Initializable {

    @FXML private ComboBox<UtilisateurRole> roleComboBox;
    @FXML private ComboBox<UtilisateurStatut> statutComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Label titleLabel;
    @FXML private Region roleIndicator;
    @FXML private Region statutIndicator;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private Utilisateur targetUser;
    private Runnable onSuccess;

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UtilisateurRole role) {
                if (role == null) return "";
                return switch (role) {
                    case ADMIN -> "Administrateur";
                    case COACH -> "Coach";
                    case USER -> "Utilisateur";
                };
            }

            @Override
            public UtilisateurRole fromString(String s) {
                return null;
            }
        });
        roleComboBox.getItems().setAll(UtilisateurRole.values());

        statutComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UtilisateurStatut statut) {
                if (statut == null) return "";
                return switch (statut) {
                    case ACTIF -> "Actif";
                    case INACTIF -> "Inactif";
                    case BANNI -> "Banni";
                };
            }

            @Override
            public UtilisateurStatut fromString(String s) {
                return null;
            }
        });
        statutComboBox.getItems().setAll(UtilisateurStatut.values());

        String primaryStyle = "-fx-background-color: #1e3a5f; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 24; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;";
        saveButton.setStyle(primaryStyle);
        wirePrimaryHover(saveButton, primaryStyle);

        cancelButton.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #555; -fx-background-radius: 8; " +
                "-fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13px;");
    }

    public void setUser(Utilisateur user) {
        this.targetUser = user;
        if (user == null) {
            return;
        }
        titleLabel.setText("Modifier : " + user.getFullName().trim());
        roleComboBox.setValue(user.getRole());
        statutComboBox.setValue(user.getStatut());
        updateRoleIndicator(user.getRole());
        updateStatutIndicator(user.getStatut());
        roleComboBox.valueProperty().addListener((obs, old, value) -> updateRoleIndicator(value));
        statutComboBox.valueProperty().addListener((obs, old, value) -> updateStatutIndicator(value));
    }

    @FXML
    private void handleSave() {
        UtilisateurRole selectedRole = roleComboBox.getValue();
        UtilisateurStatut selectedStatut = statutComboBox.getValue();

        if (selectedRole == null || selectedStatut == null) {
            showError("Veuillez selectionner un role et un statut.");
            return;
        }
        if (targetUser == null || targetUser.getId() == null) {
            showError("Utilisateur introuvable.");
            return;
        }

        try {
            utilisateurService.updateRoleAndStatus(targetUser.getId(), selectedRole, selectedStatut);
            showSuccess("Modifications enregistrees.");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
            pause.setOnFinished(e -> {
                try {
                    SceneManager.switchTo("admin-users");
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } catch (Exception ex) {
                    showError("Navigation impossible.");
                }
            });
            pause.play();
        } catch (Exception e) {
            showError("Erreur lors de la mise a jour : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        try {
            SceneManager.switchTo("admin-users");
        } catch (Exception e) {
            showError("Navigation impossible.");
        }
    }

    private void updateRoleIndicator(UtilisateurRole role) {
        if (role == null) return;
        String color = switch (role) {
            case ADMIN -> "#8e44ad";
            case COACH -> "#2980b9";
            case USER -> "#27ae60";
        };
        roleIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5; " +
                "-fx-min-width: 10; -fx-min-height: 10; -fx-max-width: 10; -fx-max-height: 10;");
    }

    private void updateStatutIndicator(UtilisateurStatut statut) {
        if (statut == null) return;
        String color = switch (statut) {
            case ACTIF -> "#27ae60";
            case INACTIF -> "#e67e22";
            case BANNI -> "#e74c3c";
        };
        statutIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5; " +
                "-fx-min-width: 10; -fx-min-height: 10; -fx-max-width: 10; -fx-max-height: 10;");
    }

    private void showSuccess(String message) {
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setText("\u2713 " + message);
        statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12px;");
    }

    private void showError(String message) {
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setText("\u26a0 " + message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
    }

    private void wirePrimaryHover(Button button, String baseStyle) {
        String hoverStyle = baseStyle.replace("#1e3a5f", "#2c5282");
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }
}
