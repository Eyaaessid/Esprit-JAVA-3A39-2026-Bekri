package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.utils.EmailService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AdminReactivationRequestsController {

    @FXML private ListView<Utilisateur> requestsListView;
    @FXML private Label pendingCountLabel;
    @FXML private Label emptyLabel;

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final AccountStatusService accountStatusService =
            new AccountStatusService(utilisateurDao, EmailService.getInstance());

    private final ObservableList<Utilisateur> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        requestsListView.setItems(items);
        requestsListView.setCellFactory(lv -> new ReactivationRequestCell());
        requestsListView.setFixedCellSize(Region.USE_COMPUTED_SIZE);
        loadRequests();
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    @FXML private void handleAccueil()       { navigateTo("admin-dashboard"); }
    @FXML private void handleQuestions()     { navigateTo("questions"); }
    @FXML private void handleUtilisateurs()  { navigateTo("admin-users"); }
    @FXML private void handleProfil()        { navigateTo("profile"); }
    @FXML private void handleRefresh()       { loadRequests(); }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        navigateTo("login");
    }

    // ── Data ────────────────────────────────────────────────────────────────

    private void loadRequests() {
        pendingCountLabel.setText("Chargement...");
        new Thread(() -> {
            try {
                // Load all INACTIF users deactivated by admin
                List<Utilisateur> all = utilisateurDao.findAll();
                List<Utilisateur> pending = all.stream()
                        .filter(u -> u.getStatut() == UtilisateurStatut.INACTIF
                                && "admin".equalsIgnoreCase(u.getDeactivatedBy()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    items.setAll(pending);
                    int count = pending.size();
                    pendingCountLabel.setText(count == 0
                            ? "Aucune demande en attente."
                            : count + " compte" + (count > 1 ? "s" : "") + " en attente de reactivation.");
                    showEmptyState(pending.isEmpty());
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        DialogHelper.showError("Erreur", "Impossible de charger les demandes: " + e.getMessage()));
            }
        }, "load-reactivation-requests").start();
    }

    private void approveReactivation(Utilisateur user) {
        if (!DialogHelper.showConfirm(
                "Reactiver le compte",
                "Reactiver le compte de " + user.getFullName() + " (" + user.getEmail() + ") ?\n\nL'utilisateur recevra un email de confirmation.",
                "Reactiver",
                "Annuler")) return;

        new Thread(() -> {
            try {
                accountStatusService.reactivateAccount(user);
                Platform.runLater(() -> {
                    DialogHelper.showInfo("Compte reactive",
                            user.getFullName() + " a ete reactive avec succes. Un email lui a ete envoye.");
                    loadRequests();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        DialogHelper.showError("Erreur", "Impossible de reactiver ce compte: " + e.getMessage()));
            }
        }, "approve-reactivation").start();
    }

    private void denyReactivation(Utilisateur user) {
        // Ask for a denial reason via a simple text input dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refuser la demande");
        dialog.setHeaderText("Motif du refus pour " + user.getFullName());
        dialog.setContentText("Motif (optionnel):");
        dialog.getDialogPane().setStyle(
                "-fx-background-color: white; -fx-font-family: 'Segoe UI';");

        dialog.showAndWait().ifPresent(reason -> {
            new Thread(() -> {
                try {
                    // Send denial email using emailService directly
                    EmailService.getInstance().sendReactivationDenied(user,
                            reason.isBlank() ? "Aucun motif fourni." : reason.trim());
                    Platform.runLater(() -> {
                        DialogHelper.showInfo("Demande refusee",
                                "L'utilisateur " + user.getFullName() + " a ete notifie du refus.");
                        // NOTE: account stays INACTIF — no status change needed
                        loadRequests();
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                            DialogHelper.showError("Erreur email", "Impossible d'envoyer le refus: " + e.getMessage()));
                }
            }, "deny-reactivation").start();
        });
    }

    private void showEmptyState(boolean empty) {
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        requestsListView.setVisible(!empty);
        requestsListView.setManaged(!empty);
    }

    private void navigateTo(String scene) {
        try {
            SceneManager.switchTo(scene);
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    // ── Inner cell ───────────────────────────────────────────────────────────

    /**
     * List cell that renders each pending reactivation request with
     * Approve / Deny buttons.
     */
    private class ReactivationRequestCell extends ListCell<Utilisateur> {

        @Override
        protected void updateItem(Utilisateur user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                setStyle("-fx-padding: 0; -fx-background-color: transparent;");
                return;
            }

            // Avatar initials
            Label avatar = new Label(user.getInitials());
            avatar.setStyle(
                    "-fx-background-color: #e67e22; " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                            "-fx-min-width: 44px; -fx-min-height: 44px; -fx-max-width: 44px; " +
                            "-fx-max-height: 44px; -fx-background-radius: 22px; -fx-alignment: center;");

            // Name + email
            Label nameLabel = new Label(user.getFullName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

            Label emailLabel = new Label(user.getEmail());
            emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

            // Role badge
            Label roleBadge = new Label(user.getRole() != null ? user.getRole().name() : "-");
            roleBadge.setStyle(
                    "-fx-background-color: #eaf4fb; -fx-text-fill: #2980b9; " +
                            "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;");

            // Status tag — always INACTIF here
            Label statusTag = new Label("INACTIF • Desactive par admin");
            statusTag.setStyle(
                    "-fx-background-color: #fef3cd; -fx-text-fill: #e67e22; " +
                            "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 11px;");

            // Deactivated-at date (if available)
            String dateStr = user.getDeactivatedAt() != null
                    ? "Desactive le " + user.getDeactivatedAt().toLocalDate()
                    : "Date inconnue";
            Label dateLabel = new Label(dateStr);
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

            VBox infoBox = new VBox(3, nameLabel, emailLabel, dateLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            HBox badgeBox = new HBox(8, roleBadge, statusTag);
            badgeBox.setAlignment(Pos.CENTER_LEFT);

            // Approve button
            Button approveBtn = new Button("✓  Reactiver");
            approveBtn.setStyle(
                    "-fx-background-color: #27ae60; -fx-text-fill: white; " +
                            "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold; " +
                            "-fx-cursor: hand; -fx-font-size: 13px;");
            approveBtn.setOnAction(e -> approveReactivation(user));

            // Deny button
            Button denyBtn = new Button("✗  Refuser");
            denyBtn.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                            "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold; " +
                            "-fx-cursor: hand; -fx-font-size: 13px;");
            denyBtn.setOnAction(e -> denyReactivation(user));

            HBox actionsBox = new HBox(8, approveBtn, denyBtn);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox leftCol = new VBox(6, infoBox, badgeBox);
            leftCol.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(leftCol, Priority.ALWAYS);

            HBox row = new HBox(12, avatar, leftCol, spacer, actionsBox);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; " +
                            "-fx-padding: 14 18; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 5, 0, 0, 1);");

            setGraphic(row);
            setStyle("-fx-padding: 6 8; -fx-background-color: transparent;");
        }
    }
}