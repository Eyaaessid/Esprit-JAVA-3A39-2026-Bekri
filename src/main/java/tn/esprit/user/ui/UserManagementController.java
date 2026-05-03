package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.UtilisateurService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class UserManagementController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @FXML private Label pageSubtitleLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label totalUsersCountLabel;
    @FXML private Label membresCountLabel;
    @FXML private Label coachsCountLabel;
    @FXML private Label adminsCountLabel;
    @FXML private Label resultsCountLabel;
    @FXML private Label emptyStateLabel;
    @FXML private TableView<Utilisateur> usersTable;
    @FXML private TableColumn<Utilisateur, Utilisateur> userColumn;
    @FXML private TableColumn<Utilisateur, String> emailColumn;
    @FXML private TableColumn<Utilisateur, Utilisateur> roleColumn;
    @FXML private TableColumn<Utilisateur, Utilisateur> statusColumn;
    @FXML private TableColumn<Utilisateur, String> createdAtColumn;
    @FXML private TableColumn<Utilisateur, Utilisateur> actionsColumn;

    private final ObservableList<Utilisateur> users = FXCollections.observableArrayList();
    private final UtilisateurService utilisateurService = new UtilisateurService();

    @FXML
    private void initialize() {
        roleFilterCombo.setItems(FXCollections.observableArrayList("Tous les roles", "USER", "COACH", "ADMIN"));
        roleFilterCombo.setValue("Tous les roles");

        statusFilterCombo.setItems(FXCollections.observableArrayList("Tous", "ACTIF", "INACTIF", "BLOQUE"));
        statusFilterCombo.setValue("Tous");

        userColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        emailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getEmail())));
        roleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        createdAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDate(data.getValue().getCreatedAt())));
        actionsColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

        usersTable.setItems(users);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        configureUserColumn();
        configureRoleColumn();
        configureStatusColumn();
        configureActionsColumn();
        loadData();
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchTo("admin-dashboard");
        } catch (IOException e) {
            System.err.println("[UserManagementController.handleBack] " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        roleFilterCombo.setValue("Tous les roles");
        statusFilterCombo.setValue("Tous");
        loadData();
    }

    private void loadData() {
        runAsync(() -> {
            try {
                List<Utilisateur> filteredUsers = applyFilters(utilisateurService.getAllUsers());
                Map<String, Integer> roleStats = utilisateurService.getRoleStats();

                Platform.runLater(() -> {
                    users.setAll(filteredUsers);
                    int total = roleStats.getOrDefault("total", 0);
                    pageSubtitleLabel.setText(total + " utilisateurs au total");
                    totalUsersCountLabel.setText(String.valueOf(total));
                    membresCountLabel.setText(String.valueOf(roleStats.getOrDefault("user", 0)));
                    coachsCountLabel.setText(String.valueOf(roleStats.getOrDefault("coach", 0)));
                    adminsCountLabel.setText(String.valueOf(roleStats.getOrDefault("admin", 0)));
                    resultsCountLabel.setText(filteredUsers.size() + " resultats");

                    boolean empty = filteredUsers.isEmpty();
                    usersTable.setVisible(!empty);
                    usersTable.setManaged(!empty);
                    emptyStateLabel.setVisible(empty);
                    emptyStateLabel.setManaged(empty);
                });
            } catch (Exception e) {
                System.err.println("[UserManagementController.loadData] " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    private List<Utilisateur> applyFilters(List<Utilisateur> allUsers) {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String roleValue = roleFilterCombo.getValue();
        String statusValue = statusFilterCombo.getValue();

        return allUsers.stream()
                .filter(user -> query.isBlank() || matchesSearch(user, query))
                .filter(user -> roleValue == null || "Tous les roles".equalsIgnoreCase(roleValue)
                        || (user.getRole() != null && user.getRole().name().equalsIgnoreCase(roleValue)))
                .filter(user -> statusValue == null || "Tous".equalsIgnoreCase(statusValue)
                        || (user.getStatut() != null && user.getStatut().name().equalsIgnoreCase(statusValue)))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(Utilisateur user, String query) {
        return safeText(user.getNom()).toLowerCase(Locale.ROOT).contains(query)
                || safeText(user.getPrenom()).toLowerCase(Locale.ROOT).contains(query)
                || safeText(user.getEmail()).toLowerCase(Locale.ROOT).contains(query);
    }

    private void configureUserColumn() {
        userColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Utilisateur user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }

                Label avatar = new Label(user.getInitials().isBlank() ? "U" : user.getInitials());
                avatar.getStyleClass().add("users-avatar");

                Label fullName = new Label(safeText(user.getFullName()).trim());
                fullName.getStyleClass().add("users-name");

                Label email = new Label(safeText(user.getEmail()));
                email.getStyleClass().add("users-subtext");

                VBox nameBox = new VBox(2, fullName, email);
                HBox wrapper = new HBox(12, avatar, nameBox);
                wrapper.setFillHeight(false);
                setGraphic(wrapper);
            }
        });
    }

    private void configureRoleColumn() {
        roleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Utilisateur user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(formatRole(user.getRole()));
                badge.getStyleClass().addAll("users-pill", rolePillClass(user.getRole()));
                setGraphic(badge);
            }
        });
    }

    private void configureStatusColumn() {
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Utilisateur user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(user.getStatut() != null ? user.getStatut().name() : "-");
                badge.getStyleClass().addAll("users-pill", statusPillClass(user.getStatut()));
                setGraphic(badge);
            }
        });
    }

    private void configureActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Utilisateur user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }

                Button modifyBtn = createActionButton("✎", "action-btn-edit");
                modifyBtn.setTooltip(new Tooltip("Modifier le role et le statut"));
                modifyBtn.setOnAction(event -> openEditScreen(user));

                Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
                boolean canDelete = currentUser == null || !user.getId().equals(currentUser.getId());

                Button deleteBtn = createActionButton("🗑", "action-btn-delete");
                deleteBtn.setTooltip(new Tooltip("Supprimer"));
                deleteBtn.setDisable(!canDelete);
                deleteBtn.setOnAction(event -> confirmAndRun(
                        "Confirmer la suppression",
                        "Voulez-vous supprimer definitivement ce compte ?",
                        () -> executeAction(() -> utilisateurService.deleteById(user.getId()))
                ));

                HBox box = new HBox(8, modifyBtn, deleteBtn);
                setGraphic(box);
            }
        });
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("users-action-btn", styleClass);
        button.setMinWidth(38);
        button.setPrefWidth(38);
        button.setMaxWidth(38);
        button.setMinHeight(34);
        button.setPrefHeight(34);
        button.setMaxHeight(34);
        return button;
    }

    private void openEditScreen(Utilisateur user) {
        try {
            AdminUserEditController controller = SceneManager.switchToAndGetController("admin-edit-user");
            controller.setUser(user);
        } catch (Exception e) {
            System.err.println("[UserManagementController.openEditScreen] userId=" + user.getId() + " " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void confirmAndRun(String title, String content, Runnable action) {
        if (DialogHelper.showConfirm(title, content)) {
            action.run();
        }
    }

    private void executeAction(Runnable action) {
        runAsync(() -> {
            try {
                action.run();
                loadData();
            } catch (Exception e) {
                System.err.println("[UserManagementController.executeAction] " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    private String formatRole(UtilisateurRole role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case USER -> "Membre";
            case COACH -> "Coach";
            case ADMIN -> "Admin";
        };
    }

    private String rolePillClass(UtilisateurRole role) {
        if (role == null) {
            return "pill-neutral";
        }
        return switch (role) {
            case USER -> "pill-user";
            case COACH -> "pill-coach";
            case ADMIN -> "pill-admin";
        };
    }

    private String statusPillClass(UtilisateurStatut status) {
        if (status == null) {
            return "pill-neutral";
        }
        return switch (status) {
            case ACTIF -> "pill-active";
            case INACTIF -> "pill-inactive";
            case BLOQUE, SUPPRIME -> "pill-blocked";
        };
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_FORMATTER);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void runAsync(Runnable runnable) {
        Thread thread = new Thread(runnable, "user-management-task");
        thread.setDaemon(true);
        thread.start();
    }
}
