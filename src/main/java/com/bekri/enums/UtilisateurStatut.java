package com.bekri.enums;

public enum UtilisateurStatut {
    ACTIF("actif"),
    BLOQUE("bloque"),
    INACTIF("inactif"),
    SUPPRIME("supprime");

    private final String value;

    UtilisateurStatut(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
