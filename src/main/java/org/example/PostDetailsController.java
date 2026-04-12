package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.community.model.Post;
import org.example.community.model.PostFormData;
import org.example.community.model.UserSummary;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class PostDetailsController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label dbStatusLabel;
    @FXML
    private ComboBox<UserSummary> currentUserComboBox;
    @FXML
    private Label selectedPostTitleLabel;
    @FXML
    private Label selectedPostMetaLabel;
    @FXML
    private Label selectedPostCategoryLabel;
    @FXML
    private Label selectedPostStatsLabel;
    @FXML
    private Label selectedPostRiskLabel;
    @FXML
    private Label selectedPostContentLabel;
    @FXML
    private ImageView postImageView;
    @FXML
    private Button editPostButton;
    @FXML
    private Button deletePostButton;

    private AppState appState;

    public void setAppState(AppState appState) {
        this.appState = appState;
        bindCurrentUser();
        refreshDatabaseStatus();
        refreshUsers();
        refreshPost();
    }

    @FXML
    private void handleBackToPosts() {
        try {
            Navigator.showPostsView();
        } catch (IOException exception) {
            showError("Failed to go back to posts: " + exception.getMessage());
        }
    }

    @FXML
    private void handleOpenComments() {
        Post post = appState.getCurrentPost();
        if (post == null) {
            showError("No post is selected.");
            return;
        }

        try {
            Navigator.showCommentsView(post);
        } catch (IOException exception) {
            showError("Failed to open comments: " + exception.getMessage());
        }
    }

    @FXML
    private void handleEditPost() {
        Post post = appState.getCurrentPost();
        if (post == null) {
            showError("No post is selected.");
            return;
        }

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
            refreshPost();
        } catch (Exception exception) {
            showError("Failed to update the post: " + exception.getMessage());
        }
    }

    @FXML
    private void handleDeletePost() {
        Post post = appState.getCurrentPost();
        if (post == null) {
            showError("No post is selected.");
            return;
        }

        if (!post.canBeEditedBy(appState.getCurrentUser())) {
            showError("You can only delete your own posts unless the selected user is admin.");
            return;
        }

        if (!confirm("Delete this post?", "The Java version keeps the Symfony soft-delete behavior.")) {
            return;
        }

        try {
            appState.getPostDao().softDelete(post.getId());
            appState.setCurrentPost(null);
            Navigator.showPostsView();
        } catch (Exception exception) {
            showError("Failed to delete the post: " + exception.getMessage());
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

    private void refreshPost() {
        try {
            Optional<Post> refreshed = appState.reloadCurrentPost();
            if (refreshed.isEmpty()) {
                showError("This post is no longer available.");
                Navigator.showPostsView();
                return;
            }
            renderPost(refreshed.get());
        } catch (Exception exception) {
            showError("Failed to load post details: " + exception.getMessage());
        }
    }

    private void renderPost(Post post) {
        selectedPostTitleLabel.setText(post.getTitre());
        selectedPostMetaLabel.setText(post.getAuthorDisplayName() + " - " + DATE_FORMATTER.format(post.getCreatedAt()));
        selectedPostCategoryLabel.setText(post.getCategorie() == null || post.getCategorie().isBlank() ? "No category" : post.getCategorie());
        selectedPostCategoryLabel.getStyleClass().setAll("pill-label", cssCategoryClass(post.getCategorie()));
        selectedPostStatsLabel.setText(post.getLikesCount() + " likes - " + post.getCommentsCount() + " comments");
        selectedPostRiskLabel.setText(buildRiskText(post));
        selectedPostContentLabel.setText(post.getContenu());
        appState.getMediaStorageService().loadImage(post.getMediaUrl()).ifPresentOrElse(
                postImageView::setImage,
                () -> postImageView.setImage(null)
        );

        boolean canEdit = post.canBeEditedBy(appState.getCurrentUser());
        editPostButton.setDisable(!canEdit);
        deletePostButton.setDisable(!canEdit);
    }

    private void bindCurrentUser() {
        currentUserComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                appState.setCurrentUser(newValue);
                Post post = appState.getCurrentPost();
                if (post != null) {
                    boolean canEdit = post.canBeEditedBy(newValue);
                    editPostButton.setDisable(!canEdit);
                    deletePostButton.setDisable(!canEdit);
                }
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

    private String buildRiskText(Post post) {
        String emotion = post.getEmotion() == null || post.getEmotion().isBlank() ? "n/a" : post.getEmotion();
        return "Emotion: " + emotion + " - Risk: " + post.getRiskLevel() + " - Sensitive: " + (post.isSensitive() ? "yes" : "no");
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
        alert.setHeaderText("Post details screen");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
