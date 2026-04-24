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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
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
    @FXML private Label emptyStateLabel;

    private final ObservableList<Utilisateur> userItems = FXCollections.observableArrayList();

    // Shared service instance — passed into each cell so the same connection is used.
    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleFilter.setItems(FXCollections.observableArrayList(
                "", "user", "coach", "admin"));
        statutFilter.setItems(FXCollections.observableArrayList(
                "", "actif", "bloque", "inactif", "supprime"));

        usersListView.setItems(userItems);
        // Pass the shared service + a refresh runnable so delete reloads from DB
        usersListView.setCellFactory(lv -> new UserListCell(
                this::handleEdit,
                utilisateurService,
                this::refreshList
        ));
        usersListView.setMinHeight(0);
        usersListView.setFixedCellSize(Region.USE_COMPUTED_SIZE);
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

    // ── Data loading ────────────────────────────────────────────────────────

    private void loadUsers(String search, String role, String statut) {
        new Thread(() -> {
            try {
                List<Utilisateur> users = utilisateurService.findUtilisateursFiltered(search, role, statut);
                Map<String, Integer> stats = utilisateurService.getRoleStats();
                Platform.runLater(() -> {
                    userItems.setAll(users);
                    applyStatsFromDb(stats);
                    resultCountLabel.setText(users.size() + " résultat" + (users.size() > 1 ? "s" : ""));
                    showEmptyState(users.isEmpty());
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogHelper.showError("Erreur", e.getMessage()));
            }
        }).start();
    }

    /**
     * Full reload from DB — called after delete and after edit-save to keep the
     * list in sync with the actual database state.
     */
    public void refreshList() {
        // Preserve current filter state when refreshing
        String search = searchField.getText().trim();
        String role   = roleFilter.getValue();
        String statut = statutFilter.getValue();
        loadUsers(
                search.isEmpty() ? null : search,
                (role   == null || role.isEmpty())   ? null : role,
                (statut == null || statut.isEmpty()) ? null : statut
        );
    }

    private void applyStatsFromDb(Map<String, Integer> stats) {
        totalLabel.setText(String.valueOf(stats.getOrDefault("total", 0)));
        membresLabel.setText(String.valueOf(stats.getOrDefault("user", 0)));
        coachsLabel.setText(String.valueOf(stats.getOrDefault("coach", 0)));
        adminsLabel.setText(String.valueOf(stats.getOrDefault("admin", 0)));
    }

    // ── Handlers ────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String search = searchField.getText().trim();
        String role   = roleFilter.getValue();
        String statut = statutFilter.getValue();
        loadUsers(
                search.isEmpty() ? null : search,
                (role   == null || role.isEmpty())   ? null : role,
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

    private void handleEdit(Utilisateur user) {
        try {
            AdminUserEditController ctrl =
                    SceneManager.switchToAndGetController("admin-edit-user");
            ctrl.setUser(user);
            // When the edit dialog saves and closes, refresh this list
            ctrl.setOnSuccess(this::refreshList);
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(show);
            emptyStateLabel.setManaged(show);
        }
        usersListView.setVisible(!show);
        usersListView.setManaged(!show);
    }
}
