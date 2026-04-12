package com.bekri.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UtilisateurRole {
    USER("user"),
    COACH("coach"),
    ADMIN("admin");

    private final String value;

    UtilisateurRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UtilisateurRole fromJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toLowerCase();
        for (UtilisateurRole r : values()) {
            if (r.value.equals(s) || r.name().equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rôle inconnu : " + raw);
    }
}
