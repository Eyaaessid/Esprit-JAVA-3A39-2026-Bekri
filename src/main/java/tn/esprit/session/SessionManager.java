package tn.esprit.session;

import tn.esprit.user.entity.Utilisateur;

public class SessionManager {
    private static SessionManager instance;
    private Utilisateur currentUser;
    private String token;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public Utilisateur getCurrentUser() { return currentUser; }
    public void setCurrentUser(Utilisateur u) { this.currentUser = u; }
    public String getToken() { return token; }
    public void setToken(String t) { this.token = t; }
    public boolean isLoggedIn() { return currentUser != null; }
    public boolean isAdmin() {
        return currentUser != null &&
               currentUser.getRole() != null &&
               currentUser.getRole().name().equalsIgnoreCase("ADMIN");
    }
    public void logout() { currentUser = null; token = null; }

    /** @deprecated JWT removed; kept for controller compatibility */
    public void clear() {
        logout();
    }
}
