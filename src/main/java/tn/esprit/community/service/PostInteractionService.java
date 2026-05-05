package tn.esprit.community.service;

import tn.esprit.community.dao.PostNotificationDao;
import tn.esprit.community.dao.UserDao;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.UserSummary;

import java.sql.SQLException;
import java.util.List;
import tn.esprit.utils.EmailService;

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

        try {
            String adminEmail = EmailService.getInstance().getAdminAlertAddress();
            System.out.println("[PostInteractionService] Admin alert address from config: " + adminEmail);
            if (adminEmail != null && !adminEmail.isBlank()) {
                String html = "<p>Bonjour administrateur,</p>"
                        + "<p>Un contenu Ã  haut risque a Ã©tÃ© dÃ©tectÃ© dans le post \"" + post.getTitre() + "\".</p>"
                        + "<p>Signaux : " + signalText + "</p>"
                        + "<p>Post ID : " + post.getId() + ", Auteur ID : " + post.getUserId() + "</p>";
                System.out.println("[PostInteractionService] Sending high-risk alert email to " + adminEmail + " for post " + post.getId());
                // Send synchronously so failures are visible and the email is not lost.
                EmailService.getInstance().sendHtmlEmail(adminEmail, "Bekri Admin", "Alerte haut risque Bekri", html);
                System.out.println("[PostInteractionService] High-risk email sent to " + adminEmail);
            } else {
                System.err.println("[PostInteractionService] No admin alert address configured.");
            }
        } catch (Exception e) {
            System.err.println("[PostInteractionService] Exception in high-risk email sending: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}

