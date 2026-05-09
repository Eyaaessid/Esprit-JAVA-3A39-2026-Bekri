package tn.esprit.shared;

import tn.esprit.profil.service.ProfilPsychologiqueService;
import tn.esprit.session.SessionManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;

import java.io.IOException;

public final class PsychologicalProfileNavigation {
    private static final ProfilPsychologiqueService PROFIL_SERVICE = new ProfilPsychologiqueService();

    private PsychologicalProfileNavigation() {
    }

    public static boolean hasProfile(Utilisateur user) {
        return user != null
                && user.getId() != null
                && PROFIL_SERVICE.hasProfil(user.getId());
    }

    public static boolean shouldForceTestOnLogin(Utilisateur user) {
        return user != null
                && user.getId() != null
                && user.getRole() == UtilisateurRole.USER
                && !hasProfile(user);
    }

    public static boolean canAccessTest(Utilisateur user) {
        return user != null
                && user.getId() != null
                && user.getRole() != UtilisateurRole.ADMIN
                && !hasProfile(user);
    }

    public static void openPostLoginDestination() throws IOException {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (shouldForceTestOnLogin(user)) {
            SceneManager.switchTo("test");
            return;
        }
        openDashboardForCurrentUser();
    }

    public static void openDashboardForCurrentUser() throws IOException {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        SceneManager.switchTo(resolveDashboardScene(user));
    }

    public static void openTestIfAllowedOrDashboard() throws IOException {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (canAccessTest(user)) {
            SceneManager.switchTo("test");
            return;
        }
        SceneManager.switchTo(resolveDashboardScene(user));
    }

    private static String resolveDashboardScene(Utilisateur user) {
        if (user == null) {
            return "login";
        }
        if (user.getRole() == UtilisateurRole.ADMIN) {
            return "admin-dashboard";
        }
        if (user.getRole() == UtilisateurRole.COACH) {
            return "coach-dashboard";
        }
        return "user-dashboard";
    }
}
