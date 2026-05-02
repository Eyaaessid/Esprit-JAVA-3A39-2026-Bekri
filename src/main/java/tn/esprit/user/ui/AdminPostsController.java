package tn.esprit.user.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import tn.esprit.community.dao.PostDao;
import tn.esprit.community.model.Post;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminPostsController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label totalPostsLabel;
    @FXML private Label totalCommentsLabel;
    @FXML private Label resultCountLabel;
    @FXML private Label emptyStateLabel;

    @FXML private TableView<Post> postsTable;
    @FXML private TableColumn<Post, Number> postIdColumn;
    @FXML private TableColumn<Post, String> postAuthorColumn;
    @FXML private TableColumn<Post, String> postTitleColumn;
    @FXML private TableColumn<Post, String> postCategoryColumn;
    @FXML private TableColumn<Post, Number> postLikesColumn;
    @FXML private TableColumn<Post, Number> postCommentsColumn;
    @FXML private TableColumn<Post, String> postCreatedColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortFilter;

    private final PostDao postDao = new PostDao();

    @FXML
    private void initialize() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            navigateTo("login");
            return;
        }

        // Set up filters
        categoryFilter.getItems().addAll("Toutes catégories", "Annonce", "Événement", "Discussion", "Aide");
        categoryFilter.setValue("Toutes catégories");
        sortFilter.getItems().addAll("Trier par", "Plus récents", "Plus anciens", "Plus populaires");
        sortFilter.setValue("Trier par");

        // Configure posts table
        configurePostsTable();
        loadData();
    }

    private void configurePostsTable() {
        postsTable.setPlaceholder(new Label("Aucun post disponible encore."));
        postIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        postAuthorColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getAuthorDisplayName()));
        postTitleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTitre()));
        postCategoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getCategorie())));
        postLikesColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getLikesCount()));
        postCommentsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCommentsCount()));
        postCreatedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDateTime(cell.getValue().getCreatedAt())));
    }

    private void loadData() {
        try {
            // Get filter values
            String category = categoryFilter.getValue();
            if ("Toutes catégories".equals(category)) {
                category = null;
            }
            String sort = sortFilter.getValue();
            String sortBy = null;
            if ("Plus récents".equals(sort)) {
                sortBy = "newest";
            } else if ("Plus anciens".equals(sort)) {
                sortBy = "oldest";
            } else if ("Plus populaires".equals(sort)) {
                sortBy = "popular";
            }

            List<Post> posts = postDao.findAllVisible(category, sortBy);
            totalPostsLabel.setText(String.valueOf(posts.size()));
            postsTable.setItems(FXCollections.observableArrayList(posts));

            // Update result count
            resultCountLabel.setText(posts.size() + " post" + (posts.size() != 1 ? "s" : "") + " trouvé" + (posts.size() != 1 ? "s" : ""));
            
            // Show/hide empty state
            boolean empty = posts.isEmpty();
            emptyStateLabel.setVisible(!empty);
            emptyStateLabel.setManaged(!empty);
            postsTable.setVisible(!empty);
            postsTable.setManaged(!empty);
        } catch (SQLException e) {
            DialogHelper.showError("Posts admin", "Impossible de charger les posts : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleReset() {
        categoryFilter.setValue("Toutes catégories");
        sortFilter.setValue("Trier par");
        loadData();
    }

    @FXML private void handleBack()       { navigateTo("admin-dashboard"); }
    @FXML private void handleAccueil()       { navigateTo("admin-dashboard"); }
    @FXML private void handleQuestions()     { navigateTo("questions"); }
    @FXML private void handleUtilisateurs()  { navigateTo("admin-users"); }
    @FXML private void handleReactivations() { navigateTo("admin-reactivation-requests"); }
    @FXML private void handlePosts()         { navigateTo("admin-posts"); }
    @FXML private void handleProfil()        { navigateTo("profile"); }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        navigateTo("login");
    }

    private void navigateTo(String scene) {
        try {
            SceneManager.switchTo(scene);
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
