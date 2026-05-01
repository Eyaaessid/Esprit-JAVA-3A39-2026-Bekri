package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import org.example.community.model.PostNotification;
import org.example.community.model.RiskAnalysisResult;
import org.example.community.model.UserSummary;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostsController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label dbStatusLabel;
    @FXML private ComboBox<UserSummary> currentUserComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField searchField;
    @FXML private VBox postsContainer;
    @FXML private Label feedInfoLabel;
    @FXML private Button savedPostsButton;

    private AppState appState;
    private List<Post> posts = Collections.emptyList();
    private boolean showingSavedOnly = false;

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
        updateSavedPostsButton();
        refreshFeed();
    }

    @FXML private void handleRefresh() { refreshFeed(); }
    @FXML private void handleSearch() { refreshFeed(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        categoryComboBox.getSelectionModel().selectFirst();
        refreshFeed();
    }

    @FXML
    private void handleOpenNotifications() {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            showError("Select an active user first.");
            return;
        }

        try {
            List<PostNotification> notifications = appState.getPostNotificationDao().findForRecipient(currentUser.id(), 40);
            int unread = appState.getPostNotificationDao().countUnread(currentUser.id());
            if (notifications.isEmpty()) {
                showInfo("Notifications", "No notifications yet.");
                return;
            }

            String body = notifications.stream()
                    .map(n -> "- [" + (n.isRead() ? "read" : "unread") + "] " + n.getMessage())
                    .collect(Collectors.joining("\n"));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notifications");
            alert.setHeaderText("Unread: " + unread);
            alert.setContentText(body);
            alert.getDialogPane().setPrefWidth(780);
            alert.showAndWait();

            if (unread > 0) {
                appState.getPostNotificationDao().markAllRead(currentUser.id());
            }
        } catch (SQLException exception) {
            showError("Failed to load notifications: " + exception.getMessage());
        }
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

            RiskAnalysisResult analysis = appState.getPostModerationService().analyze(post.getContenu());
            post.setEmotion(analysis.emotion());
            post.setRiskLevel(analysis.riskLevel());
            post.setSensitive(analysis.sensitive());
            post.setMediaUrl(storeSelectedImage(result.get().selectedImagePath(), null, false));
            appState.getPostDao().insert(post);

            if ("high".equalsIgnoreCase(analysis.riskLevel())) {
                appState.getPostInteractionService().notifyHighRisk(post, currentUser, analysis.matchedSignals());
                showInfo("Risk Alert", "High-risk signals were detected. Admin notifications were created.");
            }

            refreshFeed();
        } catch (Exception exception) {
            showError("Failed to create the post: " + exception.getMessage());
        }
    }

    @FXML
    private void handleToggleSavedFilter() {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            showError("Select an active user first.");
            return;
        }
        showingSavedOnly = !showingSavedOnly;
        updateSavedPostsButton();
        refreshFeed();
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
            if (showingSavedOnly) {
                UserSummary currentUser = appState.getCurrentUser();
                posts = currentUser == null ? List.of() : appState.getSavedPostDao().findSavedPostsForUser(currentUser.id());
            } else {
                posts = appState.getPostDao().findAllVisible(searchField.getText(), categoryComboBox.getValue());
            }
            renderFeed();
        } catch (SQLException exception) {
            postsContainer.getChildren().clear();
            showError("Failed to load posts: " + exception.getMessage());
        }
    }

    private void renderFeed() {
        postsContainer.getChildren().clear();
        feedInfoLabel.setText(showingSavedOnly ? posts.size() + " saved post(s) loaded" : posts.size() + " post(s) loaded");

        renderRecommendations();

        if (posts.isEmpty()) {
            Label emptyLabel = new Label(showingSavedOnly ? "No saved posts yet." : "No posts found for the current filters.");
            emptyLabel.getStyleClass().add("empty-label");
            postsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Post post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    private void renderRecommendations() {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            List<Integer> visibleIds = posts.stream().map(Post::getId).toList();
            List<Post> recommended = appState.getPostRecommendationService()
                    .getRecommendedForUser(currentUser, 4, visibleIds, showingSavedOnly);
            if (recommended.isEmpty()) {
                return;
            }

            Label title = new Label(showingSavedOnly ? "Recommended from authors you saved" : "Recommended For You");
            title.getStyleClass().add("section-title");
            postsContainer.getChildren().add(title);

            for (Post post : recommended) {
                VBox rec = new VBox(6);
                rec.getStyleClass().add("detail-card");
                rec.setPadding(new Insets(12));

                Label postTitle = new Label(post.getTitre());
                postTitle.getStyleClass().add("post-card-title");
                Label meta = new Label(post.getLikesCount() + " likes - " + post.getCommentsCount() + " comments");
                meta.getStyleClass().add("muted-label");

                Button open = new Button("Open");
                open.getStyleClass().addAll("primary-button", "topbar-button-compact");
                open.setOnAction(event -> navigateToDetails(post));

                rec.getChildren().addAll(postTitle, meta, open);
                postsContainer.getChildren().add(rec);
            }
        } catch (SQLException exception) {
            // Keep feed usable if recommendations fail.
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

        HBox badgeRow = new HBox(8);
        badgeRow.getStyleClass().add("post-badge-row");

        String category = post.getCategorie() == null || post.getCategorie().isBlank() ? "No category" : post.getCategorie();
        Label categoryLabel = new Label(category);
        categoryLabel.getStyleClass().addAll("pill-label", cssCategoryClass(post.getCategorie()));
        badgeRow.getChildren().add(categoryLabel);

        if (post.getEmotion() != null && !post.getEmotion().isBlank()) {
            Label emotionLabel = new Label(getEmotionEmoji(post.getEmotion()) + " " + capitalize(post.getEmotion()));
            emotionLabel.getStyleClass().addAll("pill-label", "pill-neutral");
            badgeRow.getChildren().add(emotionLabel);
        }

        Label excerpt = new Label(abbreviate(post.getContenu(), 170));
        excerpt.getStyleClass().add("post-card-excerpt");
        excerpt.setWrapText(true);

        Label stats = new Label(post.getLikesCount() + " likes - " + post.getCommentsCount() + " comments");
        stats.getStyleClass().add("post-card-stats");

        HBox actions = new HBox(8);
        actions.getStyleClass().add("post-card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("View Details");
        detailsButton.getStyleClass().addAll("primary-button", "topbar-button-compact");
        detailsButton.setOnAction(event -> navigateToDetails(post));

        Button commentsButton = new Button("Comments");
        commentsButton.getStyleClass().addAll("ghost-button", "topbar-button-compact");
        commentsButton.setOnAction(event -> navigateToComments(post));

        Button likeButton = new Button();
        likeButton.getStyleClass().addAll("ghost-button", "topbar-button-compact");
        likeButton.setOnAction(event -> handleToggleLike(post));

        Button saveButton = new Button();
        saveButton.getStyleClass().addAll("ghost-button", "topbar-button-compact");
        saveButton.setOnAction(event -> handleToggleSave(post));
        refreshLikeSaveLabels(post, likeButton, saveButton);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().addAll("ghost-button", "topbar-button-compact");
        editButton.setDisable(!post.canBeEditedBy(appState.getCurrentUser()));
        editButton.setOnAction(event -> handleEditPost(post));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("danger-button", "topbar-button-compact");
        deleteButton.setDisable(!post.canBeEditedBy(appState.getCurrentUser()));
        deleteButton.setOnAction(event -> handleDeletePost(post));

        actions.getChildren().addAll(detailsButton, commentsButton, likeButton, saveButton, spacer, editButton, deleteButton);
        card.getChildren().addAll(title, meta, badgeRow, excerpt, stats, actions);
        return card;
    }

    private void handleToggleLike(Post post) {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            showError("Select an active user first.");
            return;
        }

        try {
            boolean liked = appState.getLikeDao().toggleLike(post.getId(), currentUser.id());
            if (liked) {
                appState.getPostInteractionService().notifyLike(post, currentUser);
            }
            refreshFeed();
        } catch (SQLException exception) {
            showError("Failed to toggle like: " + exception.getMessage());
        }
    }

    private void handleToggleSave(Post post) {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            showError("Select an active user first.");
            return;
        }

        try {
            appState.getSavedPostDao().toggleSaved(post.getId(), currentUser.id());
            refreshFeed();
        } catch (SQLException exception) {
            showError("Failed to toggle saved post: " + exception.getMessage());
        }
    }

    private void refreshLikeSaveLabels(Post post, Button likeButton, Button saveButton) {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            likeButton.setText("👍 Like");
            saveButton.setText("🔖 Save");
            return;
        }

        try {
            boolean liked = appState.getLikeDao().hasUserLiked(post.getId(), currentUser.id());
            boolean saved = appState.getSavedPostDao().hasUserSaved(post.getId(), currentUser.id());
            likeButton.setText(liked ? "❤ Unlike" : "👍 Like");
            saveButton.setText(saved ? "🔖 Unsave" : "🔖 Save");
        } catch (SQLException exception) {
            likeButton.setText("👍 Like");
            saveButton.setText("🔖 Save");
        }
    }

    private void updateSavedPostsButton() {
        if (savedPostsButton != null) {
            savedPostsButton.setText(showingSavedOnly ? "Show All Posts" : "Saved Posts");
        }
    }

    private String getEmotionEmoji(String emotion) {
        if (emotion == null) {
            return "🙂";
        }
        return switch (emotion.toLowerCase(Locale.ROOT)) {
            case "happy" -> "😊";
            case "sad" -> "😔";
            case "anxious" -> "😰";
            case "stressed" -> "😵";
            case "angry" -> "😠";
            case "hopeful" -> "🌱";
            default -> "🙂";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
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

            RiskAnalysisResult analysis = appState.getPostModerationService().analyze(post.getContenu());
            post.setEmotion(analysis.emotion());
            post.setRiskLevel(analysis.riskLevel());
            post.setSensitive(analysis.sensitive());

            appState.getPostDao().update(post);
            if ("high".equalsIgnoreCase(analysis.riskLevel())) {
                appState.getPostInteractionService().notifyHighRisk(post, appState.getCurrentUser(), analysis.matchedSignals());
                showInfo("Risk Alert", "High-risk signals were detected. Admin notifications were created.");
            }
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        return alert.showAndWait().filter(buttonType -> buttonType == ButtonType.OK).isPresent();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Bekri JavaFX");
        alert.setHeaderText("Posts screen");
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
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "health" -> "pill-health";
            case "nutrition" -> "pill-nutrition";
            case "mental" -> "pill-mental";
            case "fitness" -> "pill-fitness";
            default -> "pill-neutral";
        };
    }
}
