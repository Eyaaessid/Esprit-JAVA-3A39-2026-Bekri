package tn.esprit.community.ui;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.community.core.CommunityContext;
import tn.esprit.community.core.CommunityNavigator;
import tn.esprit.community.model.Comment;
import tn.esprit.community.model.CommentFormData;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.UserSummary;
import tn.esprit.user.shell.UserShellNavigator;
import tn.esprit.user.shell.UserShellRoute;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CommentsController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label dbStatusLabel;
    @FXML
    private ComboBox<UserSummary> currentUserComboBox;
    @FXML
    private Label postTitleLabel;
    @FXML
    private Label postSummaryLabel;
    @FXML
    private Label commentsCountLabel;
    @FXML
    private VBox commentsContainer;

    private CommunityContext appState;

    public void setAppState(CommunityContext appState) {
        this.appState = appState;
        bindCurrentUser();
        refreshDatabaseStatus();
        refreshUsers();
        refreshComments();
    }

    @FXML
    private void handleBackToPosts() {
        try {
            CommunityNavigator.showPostsView();
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Failed to go back to posts: " + exception.getMessage());
        }
    }

    @FXML
    private void handleBackToDetails() {
        try {
            Post post = appState.getCurrentPost();
            if (post == null) {
                showError("No post is selected.");
                return;
            }
            CommunityNavigator.showPostDetailsView(post);
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Failed to open post details: " + exception.getMessage());
        }
    }

    @FXML
    private void handleAddComment() {
        Post post = appState.getCurrentPost();
        UserSummary currentUser = appState.getCurrentUser();
        if (post == null) {
            showError("No post is selected.");
            return;
        }
        if (currentUser == null) {
            showError("Select an active user before adding a comment.");
            return;
        }

        CommentFormController controller = openCommentForm("Add Comment");
        if (controller == null) {
            return;
        }

        Optional<CommentFormData> result = controller.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            Comment comment = new Comment();
            comment.setPostId(post.getId());
            comment.setUserId(currentUser.id());
            comment.setContenu(result.get().contenu());
            appState.getCommentDao().insert(comment);
            appState.getPostInteractionService().notifyComment(post, currentUser, comment.getContenu());
            refreshComments();
        } catch (SQLException exception) {
            showError("Failed to add the comment: " + exception.getMessage());
        }
    }

    @FXML
    private void handleBackToDashboard() {
        UserShellNavigator.navigate(UserShellRoute.DASHBOARD);
    }

    private void refreshUsers() {
        if (currentUserComboBox == null) {
            return;
        }
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

    private void refreshComments() {
        try {
            Optional<Post> refreshed = appState.reloadCurrentPost();
            if (refreshed.isEmpty()) {
                showError("This post is no longer available.");
                CommunityNavigator.showPostsView();
                return;
            }

            Post post = refreshed.get();
            postTitleLabel.setText(post.getTitre());
            postSummaryLabel.setText(post.getAuthorDisplayName() + " - " + DATE_FORMATTER.format(post.getCreatedAt()));

            List<Comment> comments = appState.getCommentDao().findByPostId(post.getId());
            commentsCountLabel.setText(comments.size() + " comment(s)");
            commentsContainer.getChildren().clear();

            if (comments.isEmpty()) {
                Label emptyLabel = new Label("No comments yet for this post.");
                emptyLabel.getStyleClass().add("empty-label");
                commentsContainer.getChildren().add(emptyLabel);
                return;
            }

            for (Comment comment : comments) {
                commentsContainer.getChildren().add(createCommentCard(comment));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Failed to load comments: " + exception.getMessage());
        }
    }

    private VBox createCommentCard(Comment comment) {
        VBox card = new VBox(8);
        card.getStyleClass().add("comment-card");
        card.setPadding(new Insets(14));

        Label author = new Label(comment.getAuthorDisplayName());
        author.getStyleClass().add("comment-author");

        String metaText = DATE_FORMATTER.format(comment.getCreatedAt());
        if (comment.getUpdatedAt() != null) {
            metaText += " - edited";
        }
        Label meta = new Label(metaText);
        meta.getStyleClass().add("comment-meta");

        Label content = new Label(comment.getContenu());
        content.getStyleClass().add("comment-content");
        content.setWrapText(true);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("ghost-button");
        editButton.setDisable(!comment.canBeEditedBy(appState.getCurrentUser()));
        editButton.setOnAction(event -> handleEditComment(comment));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setDisable(!comment.canBeEditedBy(appState.getCurrentUser()));
        deleteButton.setOnAction(event -> handleDeleteComment(comment));

        actions.getChildren().addAll(editButton, deleteButton);
        card.getChildren().addAll(author, meta, content, actions);
        return card;
    }

    private void handleEditComment(Comment comment) {
        if (!comment.canBeEditedBy(appState.getCurrentUser())) {
            showError("You can only edit your own comments unless the selected user is admin.");
            return;
        }

        CommentFormController controller = openCommentForm("Edit Comment");
        if (controller == null) {
            return;
        }

        controller.fillFromComment(comment);
        Optional<CommentFormData> result = controller.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            comment.setContenu(result.get().contenu());
            appState.getCommentDao().update(comment);
            refreshComments();
        } catch (SQLException exception) {
            showError("Failed to update the comment: " + exception.getMessage());
        }
    }

    private void handleDeleteComment(Comment comment) {
        if (!comment.canBeEditedBy(appState.getCurrentUser())) {
            showError("You can only delete your own comments unless the selected user is admin.");
            return;
        }

        if (!confirm("Delete this comment?", "Symfony deletes comments permanently.")) {
            return;
        }

        try {
            appState.getCommentDao().delete(comment.getId());
            refreshComments();
        } catch (SQLException exception) {
            showError("Failed to delete the comment: " + exception.getMessage());
        }
    }

    private void bindCurrentUser() {
        if (currentUserComboBox == null) {
            return;
        }
        currentUserComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                appState.setCurrentUser(newValue);
                refreshComments();
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

    private CommentFormController openCommentForm(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(CommunityStandaloneApp.class.getResource("/fxml/community/comment-form.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            CommentFormController controller = loader.getController();
            controller.setDialogStage(stage);
            return controller;
        } catch (IOException exception) {
            showError("Failed to open the comment form: " + exception.getMessage());
            return null;
        }
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
        alert.setHeaderText("Comments screen");
        alert.setContentText(message);
        alert.showAndWait();
    }
}




