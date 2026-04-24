package tn.esprit.user.enums;

public enum UtilisateurRole {
    USER("user"),
    ADMIN("admin"),
    COACH("coach");

    private final String value;

    UtilisateurRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UtilisateurRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Unknown role: null");
        }
        for (UtilisateurRole r : values()) {
            if (r.name().equalsIgnoreCase(value) || r.getValue().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
