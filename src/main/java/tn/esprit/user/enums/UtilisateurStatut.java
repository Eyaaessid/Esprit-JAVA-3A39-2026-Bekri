package tn.esprit.user.enums;

public enum UtilisateurStatut {
    ACTIF("actif"),
    INACTIF("inactif"),
    BLOQUE("bloque"),
    SUPPRIME("supprime");

    private final String value;

    UtilisateurStatut(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UtilisateurStatut fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Unknown statut: null");
        }
        if ("banni".equalsIgnoreCase(value)) {
            return BLOQUE;
        }
        for (UtilisateurStatut s : values()) {
            if (s.name().equalsIgnoreCase(value) || s.getValue().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown statut: " + value);
    }
}
