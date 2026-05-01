package tn.esprit.community.ui;

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
import tn.esprit.community.core.CommunityContext;
import tn.esprit.community.core.CommunityNavigator;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.PostFormData;
import tn.esprit.community.model.RiskAnalysisResult;
import tn.esprit.community.model.UserSummary;
import tn.esprit.shared.SceneManager;

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
    @FXML
    private Button likeButton;
    @FXML
    private Button saveButton;

    private CommunityContext appState;

    public void setAppState(CommunityContext appState) {
        this.appState = appState;
        bindCurrentUser();
        refreshDatabaseStatus();
        refreshUsers();
        refreshPost();
    }

    @FXML
    private void handleBackToPosts() {
        try {
            CommunityNavigator.showPostsView();
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
            CommunityNavigator.showCommentsView(post);
        } catch (IOException exception) {
            showError("Failed to open comments: " + exception.getMessage());
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException exception) {
            showError("Failed to go back to dashboard: " + exception.getMessage());
        }
    }

    @FXML
    private void handleToggleLike() {
        Post post = appState.getCurrentPost();
        UserSummary user = appState.getCurrentUser();
        if (post == null || user == null) {
            showError("Select a user and a post first.");
            return;
        }
        try {
            boolean liked = appState.getLikeDao().toggleLike(post.getId(), user.id());
            if (liked) {
                appState.getPostInteractionService().notifyLike(post, user);
            }
            refreshPost();
        } catch (SQLException exception) {
            showError("Failed to toggle like: " + exception.getMessage());
        }
    }

    @FXML
    private void handleToggleSave() {
        Post post = appState.getCurrentPost();
        UserSummary user = appState.getCurrentUser();
        if (post == null || user == null) {
            showError("Select a user and a post first.");
            return;
        }
        try {
            appState.getSavedPostDao().toggleSaved(post.getId(), user.id());
            refreshPost();
        } catch (SQLException exception) {
            showError("Failed to toggle saved post: " + exception.getMessage());
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

            RiskAnalysisResult analysis = appState.getPostModerationService().analyze(post.getContenu());
            post.setEmotion(analysis.emotion());
            post.setRiskLevel(analysis.riskLevel());
            post.setSensitive(analysis.sensitive());

            appState.getPostDao().update(post);
            if ("high".equalsIgnoreCase(analysis.riskLevel())) {
                appState.getPostInteractionService().notifyHighRisk(post, appState.getCurrentUser(), analysis.matchedSignals());
                showInfo("Risk Alert", "High-risk signals were detected. Admin notifications were created.");
            }
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
            CommunityNavigator.showPostsView();
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
            currentUserComboBox.setDisable(true);
        } catch (SQLException exception) {
            showError("Failed to load users: " + exception.getMessage());
        }
    }

    private void refreshPost() {
        try {
            Optional<Post> refreshed = appState.reloadCurrentPost();
            if (refreshed.isEmpty()) {
                showError("This post is no longer available.");
                CommunityNavigator.showPostsView();
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
        refreshLikeSaveButtons(post);
    }

    private void refreshLikeSaveButtons(Post post) {
        UserSummary user = appState.getCurrentUser();
        if (user == null) {
            likeButton.setText("Like");
            saveButton.setText("Save");
            return;
        }
        try {
            boolean liked = appState.getLikeDao().hasUserLiked(post.getId(), user.id());
            boolean saved = appState.getSavedPostDao().hasUserSaved(post.getId(), user.id());
            likeButton.setText(liked ? "Unlike" : "Like");
            saveButton.setText(saved ? "Unsave" : "Save");
        } catch (SQLException exception) {
            likeButton.setText("Like");
            saveButton.setText("Save");
        }
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
                    refreshLikeSaveButtons(post);
                }
            }
        });
    }

    private void refreshDatabaseStatus() {
        if (dbStatusLabel == null) {
            return;
        }
        try (java.sql.Connection ignored = tn.esprit.community.db.DatabaseConnection.getConnection()) {
            dbStatusLabel.setText("Connected to bekri_db");
            dbStatusLabel.getStyleClass().setAll("status-pill", "status-ok");
        } catch (SQLException exception) {
            dbStatusLabel.setText("DB connection failed");
            dbStatusLabel.getStyleClass().setAll("status-pill", "status-error");
        }
    }

    private PostFormController openPostForm() {
        try {
            FXMLLoader loader = new FXMLLoader(CommunityStandaloneApp.class.getResource("/fxml/community/post-form.fxml"));
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bekri JavaFX");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}




