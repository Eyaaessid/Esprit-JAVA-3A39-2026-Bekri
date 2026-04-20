package tn.esprit.user.ui;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.service.UtilisateurService;
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private ListView<Utilisateur> usersListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label totalLabel;
    @FXML private Label membresLabel;
    @FXML private Label coachsLabel;
    @FXML private Label adminsLabel;
    @FXML private Label resultCountLabel;

    private final ObservableList<Utilisateur> userItems = FXCollections.observableArrayList();
    private final UtilisateurService utilisateurService = new UtilisateurService();

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

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchTo("admin-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void loadUsers(String search, String role, String statut) {
        new Thread(() -> {
            try {
                List<Utilisateur> users = utilisateurService.findUtilisateursFiltered(search, role, statut);
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
        List<Utilisateur> users = List.copyOf(userItems);
        resultCountLabel.setText(users.size() + " résultat" + (users.size() > 1 ? "s" : ""));
        long membres = users.stream().filter(u -> "user".equalsIgnoreCase(u.getRoleKey())).count();
        long coachs = users.stream().filter(u -> "coach".equalsIgnoreCase(u.getRoleKey())).count();
        long admins = users.stream().filter(u -> "admin".equalsIgnoreCase(u.getRoleKey())).count();
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
            SceneManager.switchTo("admin-add-user");
        } catch (Exception e) {
            DialogHelper.showError("Navigation", "Erreur de navigation : " + e.getMessage());
        }
    }

    private void handleView(Utilisateur user) {
        try {
            AdminUserDetailController ctrl =
                    SceneManager.switchToAndGetController("admin-user-detail");
            ctrl.setUser(user);
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void handleEdit(Utilisateur user) {
        try {
            AdminEditUserController ctrl =
                    SceneManager.switchToAndGetController("admin-edit-user");
            ctrl.setUser(user);
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void handleDelete(Utilisateur user) {
        if (!DialogHelper.showConfirm("Confirmer la suppression",
                "Supprimer définitivement " + user.getFullName().trim()
                        + " ? Cette action est irréversible.")) {
            return;
        }
        new Thread(() -> {
            try {
                utilisateurService.deleteUser(user.getId());
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
}
