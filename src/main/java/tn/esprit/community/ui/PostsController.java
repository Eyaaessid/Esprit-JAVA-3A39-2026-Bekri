package tn.esprit.community.ui;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.community.core.CommunityContext;
import tn.esprit.community.core.CommunityNavigator;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.PostFormData;
import tn.esprit.community.model.PostNotification;
import tn.esprit.community.model.RiskAnalysisResult;
import tn.esprit.community.model.UserSummary;
import tn.esprit.shared.PsychologicalProfileNavigation;
import tn.esprit.shared.SceneManager;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private HBox paginationBar;
    @FXML
    private Button btnPrev;
    @FXML
    private Button btnNext;
    @FXML
    private Label pageLabel;
    @FXML
    private Label feedInfoLabel;
    @FXML
    private Button savedFilterButton;
    @FXML
    private Button notificationsButton;
    @FXML
    private Label notificationBadgeLabel;

    private CommunityContext appState;
    private List<Post> posts = Collections.emptyList();
    private boolean showingSavedOnly = false;

    @FXML
    public void initialize() {
        categoryComboBox.getItems().setAll("All", "Health", "Nutrition", "Mental", "Fitness");
        categoryComboBox.getSelectionModel().selectFirst();
        if (btnPrev != null) {
            btnPrev.setDisable(true);
        }
        if (btnNext != null) {
            btnNext.setDisable(true);
        }
        if (pageLabel != null) {
            pageLabel.setText("Page 1 / 1");
        }
    }

    public void setAppState(CommunityContext appState) {
        this.appState = appState;
        bindCurrentUser();
        refreshDatabaseStatus();
        refreshUsers();
        updateSavedFilterButton();
        refreshFeed();
        refreshNotificationBadge();
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
                refreshNotificationBadge();
                return;
            }

            Stage stage = new Stage();
            stage.initOwner(SceneManager.getPrimaryStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Notifications");

            VBox root = new VBox(18);
            root.getStyleClass().add("notification-dialog");
            root.setPadding(new Insets(22));

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label("Notifications");
            title.getStyleClass().add("notification-dialog-title");
            Label subtitle = new Label(unread > 0 ? unread + " unread" : "All caught up");
            subtitle.getStyleClass().add("notification-dialog-subtitle");
            VBox titleBox = new VBox(4, title, subtitle);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button closeButton = new Button("Close");
            closeButton.getStyleClass().add("notification-close-button");
            closeButton.setOnAction(event -> stage.close());
            header.getChildren().addAll(titleBox, spacer, closeButton);

            VBox list = new VBox(12);
            for (PostNotification notification : notifications) {
                list.getChildren().add(createNotificationCard(notification));
            }

            ScrollPane scrollPane = new ScrollPane(list);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("notification-scroll-pane");
            scrollPane.setPrefViewportHeight(460);

            root.getChildren().addAll(header, scrollPane);

            Scene scene = new Scene(root, 720, 560);
            scene.getStylesheets().add(CommunityStandaloneApp.class.getResource("/css/community.css").toExternalForm());
            scene.getStylesheets().add(CommunityStandaloneApp.class.getResource("/css/community-posts.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (unread > 0) {
                appState.getPostNotificationDao().markAllRead(currentUser.id());
            }
            refreshNotificationBadge();
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
    private void handleBackToDashboard() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException exception) {
            showError("Failed to go back to dashboard: " + exception.getMessage());
        }
    }

    private void refreshUsers() {
        try {
            appState.refreshUsers();
            if (currentUserComboBox != null) {
                currentUserComboBox.getItems().setAll(appState.getUsers());
                if (appState.getCurrentUser() != null) {
                    currentUserComboBox.getSelectionModel().select(appState.getCurrentUser());
                }
                currentUserComboBox.setDisable(true);
            }
            refreshNotificationBadge();
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
        if (showingSavedOnly) {
            if (feedInfoLabel != null) {
                feedInfoLabel.setText(posts.size() + " saved post(s) loaded");
            }
            Label savedTitle = new Label("Saved Posts");
            savedTitle.getStyleClass().add("section-title");
            postsContainer.getChildren().add(savedTitle);

            if (posts.isEmpty()) {
                Label emptyLabel = new Label("No saved posts yet.");
                emptyLabel.getStyleClass().add("empty-label");
                postsContainer.getChildren().add(emptyLabel);
            } else {
                for (Post post : posts) {
                    postsContainer.getChildren().add(createPostCard(post));
                }
            }
            renderRecommendationsSection("Recommended from authors you saved", posts.stream().map(Post::getId).toList(), true);
            return;
        }

        if (feedInfoLabel != null) {
            feedInfoLabel.setText(posts.size() + " post(s) loaded");
        }

        if (posts.isEmpty()) {
            Label emptyLabel = new Label("No posts found for the current filters.");
            emptyLabel.getStyleClass().add("empty-label");
            postsContainer.getChildren().add(emptyLabel);
        } else {
            for (Post post : posts) {
                postsContainer.getChildren().add(createPostCard(post));
            }
        }
        renderRecommendationsSection("Recommended For You", posts.stream().map(Post::getId).toList(), false);
    }

    private void renderRecommendationsSection(String titleText, List<Integer> excludedFeedIds, boolean prioritizeSavedAuthors) {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            List<Post> recommended = appState.getPostRecommendationService()
                    .getRecommendedForUser(currentUser, 4, excludedFeedIds, prioritizeSavedAuthors);
            if (recommended.isEmpty()) {
                if (showingSavedOnly) {
                    Label emptyLabel = new Label("No recommendations available yet.");
                    emptyLabel.getStyleClass().add("empty-label");
                    postsContainer.getChildren().add(emptyLabel);
                }
                return;
            }

            Label title = new Label(titleText);
            title.getStyleClass().add("section-title");
            postsContainer.getChildren().add(title);

            for (Post post : recommended) {
                postsContainer.getChildren().add(createPostCard(post));
            }
        } catch (SQLException exception) {
            // Recommendations are optional; keep feed usable if query fails.
        }
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox();
        card.getStyleClass().add("post-card");
        card.setStyle("-fx-padding: 0;");

        HBox header = new HBox(12);
        header.getStyleClass().add("post-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(getInitials(post.getAuthorDisplayName()));
        avatar.getStyleClass().add("post-author-avatar");

        VBox authorDetails = new VBox(2);
        authorDetails.getStyleClass().add("post-author-details");
        Label authorName = new Label(post.getAuthorDisplayName());
        authorName.getStyleClass().add("post-author-name");
        Label timestamp = new Label(DATE_FORMATTER.format(post.getCreatedAt()));
        timestamp.getStyleClass().add("post-author-timestamp");
        authorDetails.getChildren().addAll(authorName, timestamp);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button menuButton = new Button("...");
        menuButton.getStyleClass().add("post-menu-button");
        menuButton.setStyle("-fx-font-size: 18; -fx-padding: 4;");

        header.getChildren().addAll(avatar, authorDetails, headerSpacer, menuButton);

        VBox content = new VBox();
        content.getStyleClass().add("post-content");
        content.setStyle("-fx-padding: 0;");

        Label title = new Label(post.getTitre());
        title.getStyleClass().add("post-title");
        title.setWrapText(true);

        HBox badgeRow = new HBox(8);
        badgeRow.getStyleClass().add("post-badge-row");

        String category = post.getCategorie() == null || post.getCategorie().isBlank() ? "General" : post.getCategorie();
        Label categoryBadge = new Label(category);
        categoryBadge.getStyleClass().add("post-category-badge");
        badgeRow.getChildren().add(categoryBadge);

        if (post.getEmotion() != null && !post.getEmotion().isBlank()) {
            Label emotionBadge = new Label(getEmotionEmoji(post.getEmotion()) + " " + capitalize(post.getEmotion()));
            emotionBadge.getStyleClass().addAll("emotion-badge", "emotion-" + post.getEmotion().toLowerCase(Locale.ROOT));
            badgeRow.getChildren().add(emotionBadge);
        }

        if (post.getRiskLevel() != null && !post.getRiskLevel().isBlank()) {
            Label riskBadge = new Label(getRiskEmoji(post.getRiskLevel()) + " " + post.getRiskLevel().toUpperCase(Locale.ROOT));
            riskBadge.getStyleClass().addAll("risk-badge", "risk-" + post.getRiskLevel().toLowerCase(Locale.ROOT));
            badgeRow.getChildren().add(riskBadge);
        }

        Label body = new Label(abbreviate(post.getContenu(), 280));
        body.getStyleClass().add("post-body");
        body.setWrapText(true);

        content.getChildren().addAll(title, badgeRow);
        if (post.getMediaUrl() != null && !post.getMediaUrl().isBlank()) {
            ImageView mediaPreview = new ImageView();
            mediaPreview.getStyleClass().add("post-media-preview");
            mediaPreview.setPreserveRatio(true);
            mediaPreview.setSmooth(true);
            mediaPreview.setCache(true);
            mediaPreview.setFitHeight(190);
            mediaPreview.setFitWidth(520);
            VBox.setMargin(mediaPreview, new Insets(8, 16, 8, 16));

            appState.getMediaStorageService().loadImage(post.getMediaUrl()).ifPresentOrElse(
                    mediaPreview::setImage,
                    () -> mediaPreview.setImage(null)
            );

            if (mediaPreview.getImage() != null) {
                content.getChildren().add(mediaPreview);
            } else {
                Label mediaPlaceholder = new Label("Image attached");
                mediaPlaceholder.getStyleClass().add("post-media-placeholder");
                content.getChildren().add(mediaPlaceholder);
            }
        }
        content.getChildren().add(body);

        HBox statsSection = new HBox(20);
        statsSection.getStyleClass().add("post-stats-section");
        statsSection.setAlignment(Pos.CENTER_LEFT);

        HBox likesStat = new HBox(4);
        likesStat.getStyleClass().add("post-stat");
        Label likesIcon = new Label("❤");
        likesIcon.getStyleClass().add("post-stat-icon");
        Label likesValue = new Label(post.getLikesCount() + " likes");
        likesValue.getStyleClass().add("post-stat-value");
        likesStat.getChildren().addAll(likesIcon, likesValue);

        HBox commentsStat = new HBox(4);
        commentsStat.getStyleClass().add("post-stat");
        Label commentsIcon = new Label("💬");
        commentsIcon.getStyleClass().add("post-stat-icon");
        Label commentsValue = new Label(post.getCommentsCount() + " comments");
        commentsValue.getStyleClass().add("post-stat-value");
        commentsStat.getChildren().addAll(commentsIcon, commentsValue);

        statsSection.getChildren().addAll(likesStat, commentsStat);

        HBox interactions = new HBox();
        interactions.getStyleClass().add("post-interactions");

        Button likeButton = new Button("👍 Like");
        likeButton.getStyleClass().add("interaction-button");
        likeButton.setOnAction(event -> {
            handleToggleLike(post);
            updateInteractionButton(likeButton, post, "like");
        });

        Button commentButton = new Button("💬 Comment");
        commentButton.getStyleClass().add("interaction-button");
        commentButton.setOnAction(event -> navigateToComments(post));

        Button saveButton = new Button("🔖 Save");
        saveButton.getStyleClass().add("interaction-button");
        saveButton.setOnAction(event -> {
            handleToggleSave(post);
            updateInteractionButton(saveButton, post, "save");
        });

        Button shareButton = new Button("↗ Share");
        shareButton.getStyleClass().add("interaction-button");
        shareButton.setOnAction(event -> handleShare(post));

        interactions.getChildren().addAll(likeButton, commentButton, saveButton, shareButton);
        refreshLikeSaveLabels(post, likeButton, saveButton);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("post-actions");

        Button detailsButton = new Button("View Details");
        detailsButton.getStyleClass().addAll("btn-primary", "posts-top-button");
        detailsButton.setOnAction(event -> navigateToDetails(post));

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("post-action-edit");
        editButton.setDisable(!post.canBeEditedBy(appState.getCurrentUser()));
        editButton.setOnAction(event -> handleEditPost(post));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("post-action-delete");
        deleteButton.setDisable(!post.canBeEditedBy(appState.getCurrentUser()));
        deleteButton.setOnAction(event -> handleDeletePost(post));

        actions.getChildren().addAll(detailsButton, editButton, deleteButton);

        card.getChildren().addAll(header, content, statsSection, interactions, actions);
        return card;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.split(" ");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(part.charAt(0));
            }
        }
        return initials.toString().toUpperCase().substring(0, Math.min(2, initials.length()));
    }

    private void updateInteractionButton(Button button, Post post, String type) {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            if ("like".equals(type)) {
                boolean liked = appState.getLikeDao().hasUserLiked(post.getId(), currentUser.id());
                button.setText(liked ? "❤ Unlike" : "👍 Like");
                if (liked) {
                    button.getStyleClass().add("like-active");
                } else {
                    button.getStyleClass().remove("like-active");
                }
            } else if ("save".equals(type)) {
                boolean saved = appState.getSavedPostDao().hasUserSaved(post.getId(), currentUser.id());
                button.setText(saved ? "🔖 Unsave" : "🔖 Save");
                if (saved) {
                    button.getStyleClass().add("save-active");
                } else {
                    button.getStyleClass().remove("save-active");
                }
            }
        } catch (SQLException ignored) {
        }
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

    @FXML
    private void handleToggleSavedFilter() {
        UserSummary currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            showError("Select an active user first.");
            return;
        }
        showingSavedOnly = !showingSavedOnly;
        updateSavedFilterButton();
        refreshFeed();
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

    private void handleShare(Post post) {
        ClipboardContent shareContent = new ClipboardContent();
        String sharedText = post.getTitre() + System.lineSeparator()
                + abbreviate(post.getContenu(), 400) + System.lineSeparator()
                + "Author: " + post.getAuthorDisplayName();
        shareContent.putString(sharedText);
        Clipboard.getSystemClipboard().setContent(shareContent);
        showInfo("Share", "Post details copied to your clipboard.");
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

    private String getRiskEmoji(String riskLevel) {
        if (riskLevel == null) {
            return "•";
        }
        return switch (riskLevel.toLowerCase(Locale.ROOT)) {
            case "high" -> "⚠";
            case "medium" -> "▲";
            default -> "✓";
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
            CommunityNavigator.showPostDetailsView(post);
        } catch (IOException exception) {
            showError("Failed to open post details: " + exception.getMessage());
        }
    }

    private void navigateToComments(Post post) {
        try {
            CommunityNavigator.showCommentsView(post);
        } catch (IOException exception) {
            showError("Failed to open comments: " + exception.getMessage());
        }
    }

    private void bindCurrentUser() {
        if (currentUserComboBox == null) {
            return;
        }
        currentUserComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                appState.setCurrentUser(newValue);
                refreshFeed();
            }
        });
    }

    private void updateSavedFilterButton() {
        if (savedFilterButton != null) {
            savedFilterButton.setText(showingSavedOnly ? "Show All Posts" : "Saved Posts");
        }
    }

    private void refreshNotificationBadge() {
        if (notificationBadgeLabel == null || appState == null) {
            return;
        }

        UserSummary currentUser = appState.getCurrentUser();

        if (currentUser == null) {
            notificationBadgeLabel.setVisible(false);
            notificationBadgeLabel.setManaged(false);
            if (notificationsButton != null) {
                notificationsButton.getStyleClass().remove("notification-button-active");
            }
            return;
        }

        try {
            int unread = appState.getPostNotificationDao().countUnread(currentUser.id());
            if (unread > 0) {
                notificationBadgeLabel.setText(unread > 99 ? "99+" : String.valueOf(unread));
                notificationBadgeLabel.setVisible(true);
                notificationBadgeLabel.setManaged(true);
                if (notificationsButton != null && !notificationsButton.getStyleClass().contains("notification-button-active")) {
                    notificationsButton.getStyleClass().add("notification-button-active");
                }
            } else {
                notificationBadgeLabel.setVisible(false);
                notificationBadgeLabel.setManaged(false);
                if (notificationsButton != null) {
                    notificationsButton.getStyleClass().remove("notification-button-active");
                }
            }
        } catch (SQLException exception) {
            notificationBadgeLabel.setVisible(false);
            notificationBadgeLabel.setManaged(false);
        }
    }

    private VBox createNotificationCard(PostNotification notification) {
        VBox card = new VBox(8);
        card.getStyleClass().add("notification-card");
        if (!notification.isRead()) {
            card.getStyleClass().add("notification-card-unread");
        }

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label typePill = new Label(formatNotificationType(notification.getType()));
        typePill.getStyleClass().add("notification-type-pill");
        typePill.getStyleClass().add("notification-type-" + safeNotificationType(notification.getType()));

        Label timeLabel = new Label(notification.getCreatedAt() == null ? "" : DATE_FORMATTER.format(notification.getCreatedAt()));
        timeLabel.getStyleClass().add("notification-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label unreadDot = new Label(notification.isRead() ? "" : "NEW");
        unreadDot.getStyleClass().add("notification-unread-dot");
        unreadDot.setVisible(!notification.isRead());
        unreadDot.setManaged(!notification.isRead());

        metaRow.getChildren().addAll(typePill, timeLabel, spacer, unreadDot);

        Label messageLabel = new Label(notification.getMessage());
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        String postTitle = notification.getPostTitle() == null || notification.getPostTitle().isBlank()
                ? "Post #" + notification.getPostId()
                : notification.getPostTitle();
        Label contextLabel = new Label(postTitle);
        contextLabel.getStyleClass().add("notification-context");

        card.getChildren().addAll(metaRow, messageLabel, contextLabel);
        return card;
    }

    private String formatNotificationType(String type) {
        if (type == null || type.isBlank()) {
            return "Update";
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "like" -> "Like";
            case "comment" -> "Comment";
            case "risk" -> "Risk";
            default -> "Update";
        };
    }

    private String safeNotificationType(String type) {
        if (type == null || type.isBlank()) {
            return "update";
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "like", "comment", "risk" -> type.toLowerCase(Locale.ROOT);
            default -> "update";
        };
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

    @FXML
    private void handleAccueil() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException exception) {
            showError("Failed to navigate to dashboard: " + exception.getMessage());
        }
    }

    @FXML
    private void handleObjectifs() {
        try {
            SceneManager.switchTo("objectifs");
        } catch (IOException exception) {
            showError("Failed to navigate to objectifs: " + exception.getMessage());
        }
    }

    @FXML
    private void handleDailyCheckIn() {
        try {
            SceneManager.switchTo("daily-checkin");
        } catch (IOException exception) {
            showError("Failed to navigate to daily check-in: " + exception.getMessage());
        }
    }

    @FXML
    private void handleWeekPlan() {
        try {
            SceneManager.switchTo("plan-weekly");
        } catch (IOException exception) {
            showError("Failed to navigate to week plan: " + exception.getMessage());
        }
    }

    @FXML
    private void handleWeeklyInsights() {
        try {
            SceneManager.switchTo("weekly-insight");
        } catch (IOException exception) {
            showError("Failed to navigate to weekly insights: " + exception.getMessage());
        }
    }

    @FXML
    private void handleCommunity() {
        // Already on posts page
    }

    @FXML
    private void handleChatBot() {
        try {
            SceneManager.switchTo("chat-coach");
        } catch (IOException exception) {
            showError("Failed to navigate to chatbot: " + exception.getMessage());
        }
    }

    @FXML
    private void handleTest() {
        try {
            PsychologicalProfileNavigation.openTestIfAllowedOrDashboard();
        } catch (IOException exception) {
            showError("Failed to navigate to psychological test: " + exception.getMessage());
        }
    }

    @FXML
    private void handleProfilPsy() {
        try {
            SceneManager.switchTo("profil-psychologique");
        } catch (IOException exception) {
            showError("Failed to navigate to psychological profile: " + exception.getMessage());
        }
    }

    @FXML
    private void handleProfil() {
        try {
            SceneManager.switchTo("profile");
        } catch (IOException exception) {
            showError("Failed to navigate to profile: " + exception.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            SceneManager.switchTo("login");
        } catch (IOException exception) {
            showError("Failed to navigate to login: " + exception.getMessage());
        }
    }

    @FXML
    private void handlePrev() {
        if (btnPrev != null) {
            btnPrev.setDisable(true);
        }
    }

    @FXML
    private void handleNext() {
        if (btnNext != null) {
            btnNext.setDisable(true);
        }
    }
}
