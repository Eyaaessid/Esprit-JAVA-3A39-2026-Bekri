package tn.esprit.user.shell;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.community.core.CommunityNavigator;
import tn.esprit.community.ui.CommentsController;
import tn.esprit.community.ui.PostDetailsController;
import tn.esprit.community.ui.PostsController;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.CommunityNavigation;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

public class UserAppShellController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentStack;
    @FXML private VBox coachSection;

    private final Map<UserShellRoute, String> routes = new EnumMap<>(UserShellRoute.class);

    @FXML
    private void initialize() {
        registerRoutes();
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        boolean coach = user != null && user.getRole() == UtilisateurRole.COACH;
        if (coachSection != null) {
            coachSection.setVisible(coach);
            coachSection.setManaged(coach);
        }

        UserShellNavigator.bind(this);
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    UserShellNavigator.unbind(this);
                }
            });
        }

        UserShellRoute pending = UserShellNavigator.consumePendingRoute();
        loadRoute(pending != null ? pending : UserShellRoute.DASHBOARD);
    }

    private void registerRoutes() {
        routes.put(UserShellRoute.DASHBOARD, "/fxml/content/user-dashboard.fxml");
        routes.put(UserShellRoute.OBJECTIFS, "/fxml/content/objectifs.fxml");
        routes.put(UserShellRoute.SUIVI_TODAY, "/fxml/content/suivi-today.fxml");
        routes.put(UserShellRoute.PLAN_WEEKLY, "/fxml/content/plan-weekly.fxml");
        routes.put(UserShellRoute.WEEKLY_INSIGHT, "/fxml/content/weekly-insight.fxml");
        routes.put(UserShellRoute.CHAT_COACH, "/fxml/content/chat-coach.fxml");
        routes.put(UserShellRoute.TEST_PSY, "/fxml/content/test.fxml");
        routes.put(UserShellRoute.PROFIL_PSY, "/fxml/content/profil-psychologique.fxml");
        routes.put(UserShellRoute.TEST_MENTAL, "/fxml/content/test-mental.fxml");
        routes.put(UserShellRoute.PROFILE, "/fxml/content/profile.fxml");
        routes.put(UserShellRoute.EVENTS_LIST, "/fxml/content/events-list.fxml");
        routes.put(UserShellRoute.EVENTS_PARTICIPATIONS, "/fxml/content/events-participations.fxml");
        routes.put(UserShellRoute.EVENTS_FAVORIS, "/fxml/content/events-favoris.fxml");
        routes.put(UserShellRoute.EVENTS_DETAIL, "/fxml/content/events-detail.fxml");
        routes.put(UserShellRoute.POSTS_LIST, "/fxml/content/posts-list.fxml");
        routes.put(UserShellRoute.POST_DETAIL, "/fxml/content/post-detail.fxml");
        routes.put(UserShellRoute.POST_COMMENTS, "/fxml/content/post-comments.fxml");
    }

    public void loadRoute(UserShellRoute route) {
        String path = routes.get(route);
        if (path == null) {
            return;
        }
        try {
            if (isCommunityRoute(route)) {
                ensureCommunitySession();
            }
            URL url = getClass().getResource(path);
            if (url == null) {
                throw new IOException("FXML introuvable: " + path);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent node = loader.load();
            wireCommunityController(route, loader.getController());
            contentStack.getChildren().setAll(node);
            StackPane.setAlignment(node, javafx.geometry.Pos.TOP_LEFT);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger " + path, e);
        }
    }

    private static boolean isCommunityRoute(UserShellRoute route) {
        return route == UserShellRoute.POSTS_LIST
                || route == UserShellRoute.POST_DETAIL
                || route == UserShellRoute.POST_COMMENTS;
    }

    private void ensureCommunitySession() {
        try {
            javafx.stage.Stage st = (javafx.stage.Stage) contentStack.getScene().getWindow();
            CommunityNavigator.init(st);
            CommunityNavigation.syncSessionUser();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void wireCommunityController(UserShellRoute route, Object controller) {
        if (route == UserShellRoute.POSTS_LIST && controller instanceof PostsController pc) {
            pc.setAppState(CommunityNavigator.getContext());
        } else if (route == UserShellRoute.POST_DETAIL && controller instanceof PostDetailsController pdc) {
            pdc.setAppState(CommunityNavigator.getContext());
        } else if (route == UserShellRoute.POST_COMMENTS && controller instanceof CommentsController cc) {
            cc.setAppState(CommunityNavigator.getContext());
        }
    }

    @FXML
    private void openProfile() {
        UserShellNavigator.navigate(UserShellRoute.PROFILE);
    }

    @FXML
    private void logout() {
        UserShellNavigator.logout();
    }

    @FXML private void goDashboard() { UserShellNavigator.navigate(UserShellRoute.DASHBOARD); }
    @FXML private void goObjectifs() { UserShellNavigator.navigate(UserShellRoute.OBJECTIFS); }
    @FXML private void goSuiviToday() { UserShellNavigator.navigate(UserShellRoute.SUIVI_TODAY); }
    @FXML private void goPlanWeekly() { UserShellNavigator.navigate(UserShellRoute.PLAN_WEEKLY); }
    @FXML private void goWeeklyInsight() { UserShellNavigator.navigate(UserShellRoute.WEEKLY_INSIGHT); }
    @FXML private void goCommunity() {
        javafx.stage.Stage st = (javafx.stage.Stage) contentStack.getScene().getWindow();
        CommunityNavigation.openPosts(st);
    }
    @FXML private void goChatCoach() { UserShellNavigator.navigate(UserShellRoute.CHAT_COACH); }
    @FXML private void goTestPsy() { UserShellNavigator.navigate(UserShellRoute.TEST_PSY); }
    @FXML private void goProfilPsy() { UserShellNavigator.navigate(UserShellRoute.PROFIL_PSY); }
    @FXML private void goTestMental() { UserShellNavigator.navigate(UserShellRoute.TEST_MENTAL); }
    @FXML private void goProfile() { UserShellNavigator.navigate(UserShellRoute.PROFILE); }
    @FXML private void goEventsList() { UserShellNavigator.navigate(UserShellRoute.EVENTS_LIST); }
    @FXML private void goEventsParticipations() { UserShellNavigator.navigate(UserShellRoute.EVENTS_PARTICIPATIONS); }
    @FXML private void goEventsFavoris() { UserShellNavigator.navigate(UserShellRoute.EVENTS_FAVORIS); }
    @FXML private void goCoachEvenements() {
        try {
            UserShellNavigator.leaveShellToScene("coach-evenements");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML private void goCoachDashboard() {
        try {
            UserShellNavigator.leaveShellToScene("coach-dashboard");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
