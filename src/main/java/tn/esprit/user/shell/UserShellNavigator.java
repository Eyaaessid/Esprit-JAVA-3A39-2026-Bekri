package tn.esprit.user.shell;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;

import java.io.IOException;

/**
 * Central navigation for the user application shell. Controllers should call
 * {@link #navigate(UserShellRoute)} instead of replacing the whole stage when the user is in the shell.
 */
public final class UserShellNavigator {

    private static UserAppShellController shell;
    private static UserShellRoute pendingRoute;

    private UserShellNavigator() {}

    public static void bind(UserAppShellController controller) {
        shell = controller;
    }

    public static void unbind(UserAppShellController controller) {
        if (shell == controller) {
            shell = null;
        }
    }

    public static boolean isBound() {
        return shell != null;
    }

    /**
     * Called once from {@link UserAppShellController} after bind to apply a route requested before the shell existed.
     */
    public static UserShellRoute consumePendingRoute() {
        UserShellRoute r = pendingRoute;
        pendingRoute = null;
        return r;
    }

    public static void navigate(UserShellRoute route) {
        if (shell != null) {
            shell.loadRoute(route);
            return;
        }
        pendingRoute = route;
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            pendingRoute = null;
            throw new RuntimeException(e);
        }
    }

    public static void openProfile() {
        navigate(UserShellRoute.PROFILE);
    }

    public static void logout() {
        SessionManager.getInstance().logout();
        shell = null;
        pendingRoute = null;
        try {
            SceneManager.switchTo("login");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Leave the user shell and show a full-screen scene (e.g. coach tools or event form). */
    public static void leaveShellToScene(String sceneKey) throws IOException {
        shell = null;
        pendingRoute = null;
        SceneManager.switchTo(sceneKey);
    }
}
