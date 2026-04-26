package org.example.community.service;

import org.example.community.dao.PostNotificationDao;
import org.example.community.dao.UserDao;
import org.example.community.model.Post;
import org.example.community.model.UserSummary;

import java.sql.SQLException;
import java.util.List;

public class PostInteractionService {
    private final PostNotificationDao postNotificationDao;
    private final UserDao userDao;

    public PostInteractionService(PostNotificationDao postNotificationDao, UserDao userDao) {
        this.postNotificationDao = postNotificationDao;
        this.userDao = userDao;
    }

    public void notifyLike(Post post, UserSummary actor) throws SQLException {
        if (post == null || actor == null || post.getUserId() == actor.id()) {
            return;
        }
        String message = actor.displayName() + " liked your post \"" + post.getTitre() + "\".";
        postNotificationDao.insert(post.getUserId(), actor.id(), post.getId(), "like", message);
    }

    public void notifyComment(Post post, UserSummary actor, String comment) throws SQLException {
        if (post == null || actor == null || post.getUserId() == actor.id()) {
            return;
        }
        String snippet = comment == null ? "" : comment.trim();
        if (snippet.length() > 120) {
            snippet = snippet.substring(0, 120) + "...";
        }
        String message = actor.displayName() + " commented on your post \"" + post.getTitre() + "\". \"" + snippet + "\"";
        postNotificationDao.insert(post.getUserId(), actor.id(), post.getId(), "comment", message);
    }

    public void notifyHighRisk(Post post, UserSummary actor, List<String> signals) throws SQLException {
        if (post == null || actor == null) {
            return;
        }
        String signalText = signals == null || signals.isEmpty() ? "none" : String.join(", ", signals);
        String message = "High-risk content detected in post \"" + post.getTitre() + "\". Signals: " + signalText;

        for (Integer adminId : userDao.findAdminIds()) {
            postNotificationDao.insert(adminId, actor.id(), post.getId(), "risk", message);
        }

        if (post.getUserId() != actor.id()) {
            postNotificationDao.insert(post.getUserId(), actor.id(), post.getId(), "risk", message);
        }
    }
}
