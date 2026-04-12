package org.example.community.model;

public record UserSummary(
        int id,
        String nom,
        String prenom,
        String role
) {
    public String displayName() {
        return prenom + " " + nom + " (" + role + ")";
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
