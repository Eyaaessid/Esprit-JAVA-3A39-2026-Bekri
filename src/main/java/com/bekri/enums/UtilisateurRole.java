package com.bekri.enums;

public enum UtilisateurRole {
    USER("user"),
    COACH("coach"),
    ADMIN("admin");

    private final String value;

    UtilisateurRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
