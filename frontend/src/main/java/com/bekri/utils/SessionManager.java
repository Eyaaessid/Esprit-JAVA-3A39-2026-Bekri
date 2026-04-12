package com.bekri.utils;

import com.bekri.models.UtilisateurResponse;

/**
 * Singleton — stocke le token JWT et les infos utilisateur en session (heritage en session).
 * Accessible depuis n'importe quel Controller JavaFX.
 */
public class SessionManager {

    private static SessionManager instance;

    private String token;
    private UtilisateurResponse currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UtilisateurResponse getCurrentUser() { return currentUser; }
    public void setCurrentUser(UtilisateurResponse currentUser) { this.currentUser = currentUser; }

    public boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isCoach() {
        return currentUser != null && "coach".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isLoggedIn() {
        return token != null && currentUser != null;
    }

    public void clear() {
        token = null;
        currentUser = null;
    }
}
