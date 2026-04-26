package org.example;

import org.example.community.dao.CommentDao;
import org.example.community.dao.LikeDao;
import org.example.community.dao.PostDao;
import org.example.community.dao.PostNotificationDao;
import org.example.community.dao.SavedPostDao;
import org.example.community.dao.UserDao;
import org.example.community.model.Post;
import org.example.community.model.UserSummary;
import org.example.community.service.MediaStorageService;
import org.example.community.service.PostInteractionService;
import org.example.community.service.PostModerationService;
import org.example.community.service.PostRecommendationService;
import org.example.db.SchemaBootstrapService;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AppState {
    private final UserDao userDao = new UserDao();
    private final PostDao postDao = new PostDao();
    private final CommentDao commentDao = new CommentDao();
    private final LikeDao likeDao = new LikeDao();
    private final SavedPostDao savedPostDao = new SavedPostDao();
    private final PostNotificationDao postNotificationDao = new PostNotificationDao();
    private final MediaStorageService mediaStorageService = new MediaStorageService();
    private final PostModerationService postModerationService = new PostModerationService();
    private final PostInteractionService postInteractionService = new PostInteractionService(postNotificationDao, userDao);
    private final PostRecommendationService postRecommendationService = new PostRecommendationService(postDao, likeDao, savedPostDao);

    private List<UserSummary> users = Collections.emptyList();
    private UserSummary currentUser;
    private Post currentPost;

    public AppState() {
        try {
            new SchemaBootstrapService().ensureAdvancedCommunityTables();
        } catch (SQLException ignored) {
            // Keep app running even if bootstrap fails (existing DB may already be ready).
        }
    }

    public List<UserSummary> getUsers() throws SQLException {
        if (users.isEmpty()) {
            users = userDao.findAllActiveUsers();
            if (currentUser == null && !users.isEmpty()) {
                currentUser = users.get(0);
            }
        }
        return users;
    }

    public void refreshUsers() throws SQLException {
        users = userDao.findAllActiveUsers();
        if (currentUser != null) {
            int currentUserId = currentUser.id();
            currentUser = users.stream()
                    .filter(user -> user.id() == currentUserId)
                    .findFirst()
                    .orElse(users.isEmpty() ? null : users.get(0));
        } else if (!users.isEmpty()) {
            currentUser = users.get(0);
        }
    }

    public PostDao getPostDao() {
        return postDao;
    }

    public CommentDao getCommentDao() {
        return commentDao;
    }

    public LikeDao getLikeDao() {
        return likeDao;
    }

    public SavedPostDao getSavedPostDao() {
        return savedPostDao;
    }

    public PostNotificationDao getPostNotificationDao() {
        return postNotificationDao;
    }

    public MediaStorageService getMediaStorageService() {
        return mediaStorageService;
    }

    public PostModerationService getPostModerationService() {
        return postModerationService;
    }

    public PostInteractionService getPostInteractionService() {
        return postInteractionService;
    }

    public PostRecommendationService getPostRecommendationService() {
        return postRecommendationService;
    }

    public UserSummary getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserSummary currentUser) {
        this.currentUser = currentUser;
    }

    public Post getCurrentPost() {
        return currentPost;
    }

    public void setCurrentPost(Post currentPost) {
        this.currentPost = currentPost;
    }

    public Optional<Post> reloadCurrentPost() throws SQLException {
        if (currentPost == null) {
            return Optional.empty();
        }

        Optional<Post> refreshed = postDao.findVisibleById(currentPost.getId());
        refreshed.ifPresent(this::setCurrentPost);
        return refreshed;
    }
}
