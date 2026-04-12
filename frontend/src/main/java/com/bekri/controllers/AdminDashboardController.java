package com.bekri.controllers;

import com.bekri.models.UtilisateurResponse;
import com.bekri.services.ApiClient;
import com.bekri.utils.DialogHelper;
import com.bekri.utils.SceneManager;
import com.bekri.utils.SessionManager;
import com.bekri.views.UserListCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private ListView<UtilisateurResponse> usersListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label totalLabel;
    @FXML private Label membresLabel;
    @FXML private Label coachsLabel;
    @FXML private Label adminsLabel;
    @FXML private Label resultCountLabel;

    private final ObservableList<UtilisateurResponse> userItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleFilter.setItems(FXCollections.observableArrayList(
                "", "user", "coach", "admin"));
        statutFilter.setItems(FXCollections.observableArrayList(
                "", "actif", "bloque", "inactif", "supprime"));

        usersListView.setItems(userItems);
        usersListView.setCellFactory(lv -> new UserListCell(
                this::handleView,
                this::handleEdit,
                this::handleDelete
        ));
        usersListView.setMinHeight(0);
        usersListView.setFixedCellSize(88);
        VBox.setVgrow(usersListView, Priority.ALWAYS);

        searchField.setOnAction(e -> handleSearch());

        loadUsers(null, null, null);
    }

    private void loadUsers(String search, String role, String statut) {
        new Thread(() -> {
            try {
                List<UtilisateurResponse> users = ApiClient.getUtilisateurs(search, role, statut);
                Platform.runLater(() -> {
                    userItems.setAll(users);
                    applyStatsFromCurrentList();
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogHelper.showError("Erreur", e.getMessage()));
            }
        }).start();
    }

    private void applyStatsFromCurrentList() {
        List<UtilisateurResponse> users = List.copyOf(userItems);
        resultCountLabel.setText(users.size() + " résultat" + (users.size() > 1 ? "s" : ""));
        long membres = users.stream().filter(u -> "user".equalsIgnoreCase(u.getRole())).count();
        long coachs = users.stream().filter(u -> "coach".equalsIgnoreCase(u.getRole())).count();
        long admins = users.stream().filter(u -> "admin".equalsIgnoreCase(u.getRole())).count();
        totalLabel.setText(String.valueOf(users.size()));
        membresLabel.setText(String.valueOf(membres));
        coachsLabel.setText(String.valueOf(coachs));
        adminsLabel.setText(String.valueOf(admins));
    }

    @FXML
    private void handleSearch() {
        String search = searchField.getText().trim();
        String role = roleFilter.getValue();
        String statut = statutFilter.getValue();
        loadUsers(
                search.isEmpty() ? null : search,
                (role == null || role.isEmpty()) ? null : role,
                (statut == null || statut.isEmpty()) ? null : statut
        );
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        roleFilter.setValue(null);
        statutFilter.setValue(null);
        loadUsers(null, null, null);
    }

    @FXML
    private void handleAdd() {
        try {
            SceneManager.getInstance().switchTo("admin-add-user");
        } catch (Exception e) {
            DialogHelper.showError("Navigation", "Erreur de navigation : " + e.getMessage());
        }
    }

    private void handleView(UtilisateurResponse user) {
        try {
            AdminUserDetailController ctrl =
                    SceneManager.getInstance().switchToAndGetController("admin-user-detail");
            ctrl.setUser(user);
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void handleEdit(UtilisateurResponse user) {
        try {
            AdminEditUserController ctrl =
                    SceneManager.getInstance().switchToAndGetController("admin-edit-user");
            ctrl.setUser(user);
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void handleDelete(UtilisateurResponse user) {
        if (!DialogHelper.showConfirm("Confirmer la suppression",
                "Supprimer définitivement " + user.getFullName().trim()
                        + " ? Cette action est irréversible.")) {
            return;
        }
        new Thread(() -> {
            try {
                ApiClient.deleteUtilisateurPermanent(user.getId());
                Platform.runLater(() -> {
                    userItems.remove(user);
                    applyStatsFromCurrentList();
                    DialogHelper.showSuccess("Succès", "Utilisateur supprimé avec succès.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogHelper.showError("Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void showUsers() { /* déjà sur cet écran */ }

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
}
