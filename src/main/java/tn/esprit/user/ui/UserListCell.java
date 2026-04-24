package tn.esprit.user.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.session.SessionManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.UtilisateurService;

import java.util.function.Consumer;

public class UserListCell extends ListCell<Utilisateur> {

    private final Consumer<Utilisateur> onEditAction;
    private final UtilisateurService utilisateurService;
    private final Runnable onRefresh;

    public UserListCell(Consumer<Utilisateur> onEditAction,
                        UtilisateurService utilisateurService,
                        Runnable onRefresh) {
        this.onEditAction = onEditAction;
        this.utilisateurService = utilisateurService;
        this.onRefresh = onRefresh;
    }

    @Override
    protected void updateItem(Utilisateur user, boolean empty) {
        super.updateItem(user, empty);
        if (empty || user == null) {
            setGraphic(null);
            setStyle("-fx-padding: 0; -fx-background-color: transparent;");
            return;
        }

        Label avatar = new Label(user.getInitials());
        avatar.setStyle(
                "-fx-background-color: " + getRoleColor(user.getRole()) + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-min-width: 44px; -fx-min-height: 44px; -fx-max-width: 44px; " +
                        "-fx-max-height: 44px; -fx-background-radius: 22px; -fx-alignment: center;"
        );

        Label nameLabel = new Label(user.getFullName().trim());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        VBox nameBox = new VBox(2, nameLabel, emailLabel);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label roleBadge = new Label(user.getRole() != null ? user.getRole().name() : "-");
        roleBadge.setStyle(
                "-fx-background-color: " + getRoleBgColor(user.getRole()) + "; " +
                        "-fx-text-fill: " + getRoleColor(user.getRole()) + "; " +
                        "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold;"
        );

        String statutDisplay = (user.getStatutKey() != null && !user.getStatutKey().isEmpty())
                ? user.getStatutKey().toUpperCase() : "-";
        Label statutBadge = new Label(statutDisplay);
        statutBadge.setStyle(
                "-fx-background-color: " + getStatutBgColor(user.getStatut()) + "; " +
                        "-fx-text-fill: " + getStatutColor(user.getStatut()) + "; " +
                        "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 11px;"
        );

        Button editBtn = new Button("Modifier");
        String editStyle = "-fx-background-color: #1e3a5f; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 24; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;";
        editBtn.setStyle(editStyle);
        wirePrimaryHover(editBtn, editStyle);
        editBtn.setOnAction(e -> {
            if (onEditAction != null) {
                onEditAction.accept(user);
            }
        });

        Utilisateur currentAdmin = SessionManager.getInstance().getCurrentUser();
        boolean canDelete = currentAdmin != null
                && !UtilisateurRole.ADMIN.equals(user.getRole())
                && !user.getId().equals(currentAdmin.getId());

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-padding: 10 24; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        deleteBtn.setVisible(canDelete);
        deleteBtn.setManaged(canDelete);
        deleteBtn.setOnAction(e -> {
            if (showDeleteConfirmation(user)) {
                try {
                    utilisateurService.deleteById(user.getId());
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                } catch (Exception ex) {
                    showErrorDialog("Impossible de supprimer l'utilisateur.", ex.getMessage());
                }
            }
        });

        HBox badgeBox = new HBox(8, roleBadge, statutBadge);
        badgeBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionsBox = new HBox(8, editBtn, deleteBtn);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, avatar, nameBox, badgeBox, spacer, actionsBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; " +
                        "-fx-padding: 12 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);"
        );

        setGraphic(row);
        setStyle("-fx-padding: 6 8; -fx-background-color: transparent;");
    }

    private boolean showDeleteConfirmation(Utilisateur user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Confirmer la suppression");
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 28 32 12 32; -fx-background-color: white;");
        content.setAlignment(Pos.CENTER);

        Label icon = new Label("X");
        icon.setStyle("-fx-font-size: 40px; -fx-text-fill: #e74c3c;");

        Label title = new Label("Supprimer cet utilisateur ?");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox userCard = new VBox(4);
        userCard.setAlignment(Pos.CENTER);
        userCard.setStyle("-fx-background-color: #fdf2f2; -fx-background-radius: 8; " +
                "-fx-padding: 12 20; -fx-border-color: #fadbd8; " +
                "-fx-border-radius: 8; -fx-border-width: 1;");

        Label userName = new Label(user.getFullName());
        userName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #c0392b;");

        Label userEmail = new Label(user.getEmail());
        userEmail.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        userCard.getChildren().addAll(userName, userEmail);

        Label warning = new Label("Cette action est irreversible.");
        warning.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12px; -fx-font-weight: bold;");

        content.getChildren().addAll(icon, title, userCard, warning);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);");

        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteType, cancelType);

        Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteType);
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 24; -fx-font-weight: bold;");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelBtn.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #555; " +
                "-fx-background-radius: 8; -fx-padding: 8 24;");

        dialog.setResultConverter(btn -> btn == deleteType);
        return dialog.showAndWait().orElse(false);
    }

    private void showErrorDialog(String titleText, String message) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Erreur");
        error.setHeaderText(titleText);
        error.setContentText(message);
        error.showAndWait();
    }

    private void wirePrimaryHover(Button button, String baseStyle) {
        String hoverStyle = baseStyle.replace("#1e3a5f", "#2c5282");
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    private String getRoleColor(UtilisateurRole role) {
        if (role == null) return "#95a5a6";
        return switch (role) {
            case ADMIN -> "#8e44ad";
            case COACH -> "#2980b9";
            default -> "#27ae60";
        };
    }

    private String getRoleBgColor(UtilisateurRole role) {
        if (role == null) return "#f0f0f0";
        return switch (role) {
            case ADMIN -> "#f5eef8";
            case COACH -> "#eaf4fb";
            default -> "#eafaf1";
        };
    }

    private String getStatutColor(UtilisateurStatut statut) {
        if (statut == null) return "#95a5a6";
        return switch (statut) {
            case ACTIF -> "#27ae60";
            case INACTIF -> "#e67e22";
            case BANNI -> "#e74c3c";
        };
    }

    private String getStatutBgColor(UtilisateurStatut statut) {
        if (statut == null) return "#f0f0f0";
        return switch (statut) {
            case ACTIF -> "#eafaf1";
            case INACTIF -> "#fef9e7";
            case BANNI -> "#fdedec";
        };
    }
}
