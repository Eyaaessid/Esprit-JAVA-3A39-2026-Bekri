package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.community.model.Post;
import org.example.community.model.PostFormData;
import org.example.community.model.UserSummary;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostsController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label dbStatusLabel;
    @FXML
    private ComboBox<UserSummary> currentUserComboBox;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private VBox postsContainer;
    @FXML
    private Label feedInfoLabel;

    private AppState appState;
    private List<Post> posts = Collections.emptyList();

    @FXML
    public void initialize() {
        categoryComboBox.getItems().setAll("All", "Health", "Nutrition", "Mental", "Fitness");
        categoryComboBox.getSelectionModel().selectFirst();
    }

    public void setAppState(AppState appState) {
        this.appState = appState;
        bindCurrentUser();
        refreshDatabaseStatus();
        refreshUsers();
        refreshFeed();
    }

    @FXML
    private void handleRefresh() {
        refreshFeed();
    }

    @FXML
    private void handleSearch() {
        refreshFeed();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        categoryComboBox.getSelectionModel().selectFirst();
        refreshFeed();
    }

    @FXML
    private void handleCreatePost() {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            showError("Select an active user before creating a post.");
            return;
        }

        PostFormController controller = openPostForm();
        if (controller == null) {
            return;
        }

        try {
            controller.setUsers(appState.getUsers(), currentUser);
        } catch (SQLException exception) {
            showError("Failed to load users: " + exception.getMessage());
            return;
        }

        Optional<PostFormData> result = controller.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            Post post = new Post();
            post.setUserId(currentUser.id());
            post.setTitre(result.get().titre());
            post.setCategorie(result.get().categorie());
            post.setContenu(result.get().contenu());
            post.setEmotion(null);
            post.setRiskLevel("low");
            post.setSensitive(false);
            post.setMediaUrl(storeSelectedImage(result.get().selectedImagePath(), null, false));
            appState.getPostDao().insert(post);
            refreshFeed();
        } catch (Exception exception) {
            showError("Failed to create the post: " + exception.getMessage());
        }
    }

    private void refreshUsers() {
        try {
            appState.refreshUsers();
            currentUserComboBox.getItems().setAll(appState.getUsers());
            if (appState.getCurrentUser() != null) {
                currentUserComboBox.getSelectionModel().select(appState.getCurrentUser());
            }
        } catch (SQLException exception) {
            showError("Failed to load users: " + exception.getMessage());
        }
    }

    private void refreshFeed() {
        try {
            posts = appState.getPostDao().findAllVisible(searchField.getText(), categoryComboBox.getValue());
            renderFeed();
        } catch (SQLException exception) {
            postsContainer.getChildren().clear();
            showError("Failed to load posts: " + exception.getMessage());
        }
    }

    private void renderFeed() {
        postsContainer.getChildren().clear();
        feedInfoLabel.setText(posts.size() + " post(s) loaded");

        if (posts.isEmpty()) {
            Label emptyLabel = new Label("No posts found for the current filters.");
            emptyLabel.getStyleClass().add("empty-label");
            postsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Post post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(10);
        card.getStyleClass().add("post-card");
        card.setPadding(new Insets(18));

        Label title = new Label(post.getTitre());
        title.getStyleClass().add("post-card-title");

        Label meta = new Label(post.getAuthorDisplayName() + " - " + DATE_FORMATTER.format(post.getCreatedAt()));
        meta.getStyleClass().add("post-card-meta");

        String category = post.getCategorie() == null || post.getCategorie().isBlank() ? "No category" : post.getCategorie();
        Label tag = new Label(category);
        tag.getStyleClass().addAll("pill-label", cssCategoryClass(post.getCategorie()));

        Label excerpt = new Label(abbreviate(post.getContenu(), 170));
        excerpt.getStyleClass().add("post-card-excerpt");
        excerpt.setWrapText(true);

        Label stats = new Label(post.getLikesCount() + " likes - " + post.getCommentsCount() + " comments");
        stats.getStyleClass().add("post-card-stats");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("Post Details");
        detailsButton.getStyleClass().add("primary-button");
        detailsButton.setOnAction(event -> navigateToDetails(post));

        Button commentsButton = new Button("Comments");
        commentsButton.getStyleClass().add("ghost-button");
        commentsButton.setOnAction(event -> navigateToComments(post));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("ghost-button");
        editButton.setDisable(!post.canBeEditedBy(appState.getCurrentUser()));
        editButton.setOnAction(event -> handleEditPost(post));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setDisable(!post.canBeEditedBy(appState.getCurrentUser()));
        deleteButton.setOnAction(event -> handleDeletePost(post));

        actions.getChildren().addAll(detailsButton, commentsButton, spacer, editButton, deleteButton);
        card.getChildren().addAll(title, meta, tag, excerpt, stats, actions);
        return card;
    }

    private void handleEditPost(Post post) {
        if (!post.canBeEditedBy(appState.getCurrentUser())) {
            showError("You can only edit your own posts unless the selected user is admin.");
            return;
        }

        PostFormController controller = openPostForm();
        if (controller == null) {
            return;
        }

        try {
            controller.setUsers(appState.getUsers(), resolveAuthor(post.getUserId()));
        } catch (SQLException exception) {
            showError("Failed to load users: " + exception.getMessage());
            return;
        }

        controller.fillFromPost(post);
        Optional<PostFormData> result = controller.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            post.setTitre(result.get().titre());
            post.setCategorie(result.get().categorie());
            post.setContenu(result.get().contenu());
            post.setMediaUrl(storeSelectedImage(result.get().selectedImagePath(), post.getMediaUrl(), result.get().removeExistingImage()));
            appState.getPostDao().update(post);
            refreshFeed();
        } catch (Exception exception) {
            showError("Failed to update the post: " + exception.getMessage());
        }
    }

    private void handleDeletePost(Post post) {
        if (!post.canBeEditedBy(appState.getCurrentUser())) {
            showError("You can only delete your own posts unless the selected user is admin.");
            return;
        }

        if (!confirm("Delete this post?", "The Java version keeps the Symfony soft-delete behavior.")) {
            return;
        }

        try {
            appState.getPostDao().softDelete(post.getId());
            refreshFeed();
        } catch (SQLException exception) {
            showError("Failed to delete the post: " + exception.getMessage());
        }
    }

    private void navigateToDetails(Post post) {
        try {
            Navigator.showPostDetailsView(post);
        } catch (IOException exception) {
            showError("Failed to open post details: " + exception.getMessage());
        }
    }

    private void navigateToComments(Post post) {
        try {
            Navigator.showCommentsView(post);
        } catch (IOException exception) {
            showError("Failed to open comments: " + exception.getMessage());
        }
    }

    private void bindCurrentUser() {
        currentUserComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                appState.setCurrentUser(newValue);
                refreshFeed();
            }
        });
    }

    private void refreshDatabaseStatus() {
        if (dbStatusLabel == null) {
            return;
        }
        try (java.sql.Connection ignored = org.example.db.DatabaseConnection.getConnection()) {
            dbStatusLabel.setText("Connected to bekri_db");
            dbStatusLabel.getStyleClass().setAll("status-pill", "status-ok");
        } catch (SQLException exception) {
            dbStatusLabel.setText("DB connection failed");
            dbStatusLabel.getStyleClass().setAll("status-pill", "status-error");
        }
    }

    private PostFormController openPostForm() {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("/org/example/post-form.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Post Form");
            stage.setScene(new Scene(root));
            PostFormController controller = loader.getController();
            controller.setDialogStage(stage);
            return controller;
        } catch (IOException exception) {
            showError("Failed to open the post form: " + exception.getMessage());
            return null;
        }
    }

    private UserSummary resolveAuthor(int userId) {
        try {
            return appState.getUsers().stream()
                    .filter(user -> user.id() == userId)
                    .findFirst()
                    .orElse(appState.getCurrentUser());
        } catch (SQLException exception) {
            return appState.getCurrentUser();
        }
    }

    private String storeSelectedImage(Path selectedImagePath, String existingMediaUrl, boolean removeExistingImage) throws IOException {
        if (selectedImagePath != null) {
            return appState.getMediaStorageService().storeImage(selectedImagePath);
        }
        if (removeExistingImage) {
            return null;
        }
        return existingMediaUrl;
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert.showAndWait().filter(buttonType -> buttonType.getButtonData().isDefaultButton()).isPresent();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Bekri JavaFX");
        alert.setHeaderText("Posts screen");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String cssCategoryClass(String category) {
        if (category == null || category.isBlank()) {
            return "pill-neutral";
        }
        return switch (category.toLowerCase()) {
            case "health" -> "pill-health";
            case "nutrition" -> "pill-nutrition";
            case "mental" -> "pill-mental";
            case "fitness" -> "pill-fitness";
            default -> "pill-neutral";
        };
    }
}
