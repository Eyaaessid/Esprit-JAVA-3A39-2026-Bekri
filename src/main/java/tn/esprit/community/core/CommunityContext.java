package tn.esprit.community.core;

import tn.esprit.community.dao.CommentDao;
import tn.esprit.community.dao.LikeDao;
import tn.esprit.community.dao.PostDao;
import tn.esprit.community.dao.PostNotificationDao;
import tn.esprit.community.dao.SavedPostDao;
import tn.esprit.community.dao.UserDao;
import tn.esprit.community.db.SchemaBootstrapService;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.UserSummary;
import tn.esprit.community.service.MediaStorageService;
import tn.esprit.community.service.PostInteractionService;
import tn.esprit.community.service.PostModerationService;
import tn.esprit.community.service.PostRecommendationService;
import tn.esprit.session.SessionManager;
import tn.esprit.user.entity.Utilisateur;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommunityContext {
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

    public CommunityContext() {
        try {
            new SchemaBootstrapService().ensureAdvancedCommunityTables();
        } catch (SQLException ignored) {
            // Keep app running even if bootstrap fails (existing DB may already be ready).
        }
        syncCurrentSessionUser();
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
        syncCurrentSessionUser();
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

    private void syncCurrentSessionUser() {
        Utilisateur sessionUser = SessionManager.getInstance().getCurrentUser();
        if (sessionUser == null || sessionUser.getId() == null || users.isEmpty()) {
            return;
        }

        currentUser = users.stream()
                .filter(user -> user.id() == sessionUser.getId())
                .findFirst()
                .orElse(currentUser);
    }
}


