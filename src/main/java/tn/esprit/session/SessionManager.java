package tn.esprit.session;

import tn.esprit.user.entity.Admin;
import tn.esprit.user.entity.Coach;
import tn.esprit.user.entity.Utilisateur;

public class SessionManager {
    private static SessionManager instance;
    private Utilisateur currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(Utilisateur user) { this.currentUser = user; }

    public Utilisateur getCurrentUser() { return currentUser; }

    public Admin getCurrentAdmin() {
        return (currentUser instanceof Admin admin) ? admin : null;
    }

    public Coach getCurrentCoach() {
        return (currentUser instanceof Coach coach) ? coach : null;
    }

    public boolean isAdmin() { return currentUser instanceof Admin; }

    public boolean isCoach() { return currentUser instanceof Coach; }

    public boolean isLoggedIn() { return currentUser != null; }

    /**
     * All authenticated users can use 2FA and face auth regardless of role.
     * This method exists to make the intent explicit in controllers.
     */
    public boolean canUseSecurityFeatures() {
        return isLoggedIn(); // no role restriction
    }

    public void logout() { clear(); }

    public void clear() { currentUser = null; }
}
