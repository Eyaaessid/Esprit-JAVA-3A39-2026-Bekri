package com.bekri.enums;

import java.util.Locale;

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

    public static UtilisateurStatut fromDbValue(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        for (UtilisateurStatut s : values()) {
            if (s.value.equals(v)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Statut utilisateur inconnu (DB) : " + raw);
    }
}
