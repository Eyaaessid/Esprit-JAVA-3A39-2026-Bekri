package com.bekri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Correspond à AuthResponseDTO côté API : { message, utilisateur, token }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {

    private String message;
    private UtilisateurResponse utilisateur;
    private String token;

    public AuthResponse() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UtilisateurResponse getUtilisateur() { return utilisateur; }
    public void setUtilisateur(UtilisateurResponse utilisateur) { this.utilisateur = utilisateur; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
