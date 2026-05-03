package tn.esprit.shared;

import javafx.stage.Stage;
import tn.esprit.community.core.CommunityContext;
import tn.esprit.community.core.CommunityNavigator;
import tn.esprit.session.SessionManager;
import tn.esprit.user.entity.Utilisateur;

import java.sql.SQLException;

public final class CommunityNavigation {

    private CommunityNavigation() {
    }

    public static void openPosts(Stage stage) {
        try {
            CommunityNavigator.init(stage);
            syncSessionUser();
            CommunityNavigator.showPostsView();
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            if ((message == null || message.isBlank()) && e.getCause() != null) {
                message = e.getCause().getMessage();
            }
            DialogHelper.showError("Community", "Impossible d'ouvrir l'espace posts : " + (message == null ? e.getClass().getSimpleName() : message));
        }
    }

    /** Syncs {@link CommunityContext} users and current user from {@link SessionManager} (for shell + standalone). */
    public static void syncSessionUser() throws SQLException {
        syncCurrentSessionUser();
    }

    private static void syncCurrentSessionUser() throws SQLException {
        CommunityContext appState = CommunityNavigator.getContext();
        appState.refreshUsers();

        Utilisateur sessionUser = SessionManager.getInstance().getCurrentUser();
        if (sessionUser == null || sessionUser.getId() == null) {
            return;
        }

        appState.setCurrentUser(
                appState.getUsers().stream()
                        .filter(user -> user.id() == sessionUser.getId())
                        .findFirst()
                        .orElse(appState.getCurrentUser())
        );
    }
}
