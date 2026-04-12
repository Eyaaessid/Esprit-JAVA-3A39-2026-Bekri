package com.bekri.config;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Affiche un message lisible lorsque l'échec au démarrage est dû à la connexion MySQL / JDBC.
 */
public class DatabaseConnectionFailureAnalyzer implements FailureAnalyzer {

    private static final String DESCRIPTION =
            "Connexion à la base MySQL refusée ou impossible. "
                    + "L'application ne peut pas joindre bekri_db (paramètres spring.datasource.* dans application.properties).";

    private static final String ACTION =
            "Démarrez MySQL, vérifiez que la base bekri_db existe, que l'hôte et le port sont corrects (127.0.0.1:3306), "
                    + "ainsi que le compte (spring.datasource.username / password).";

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!isLikelyDatasourceFailure(failure)) {
            return null;
        }
        return new FailureAnalysis(DESCRIPTION, ACTION, failure);
    }

    private static boolean isLikelyDatasourceFailure(Throwable failure) {
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = failure; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof SQLException) {
                return true;
            }
            String className = t.getClass().getName();
            if (className.equals("org.springframework.jdbc.CannotGetJdbcConnectionException")) {
                return true;
            }
            String msg = t.getMessage();
            if (msg == null) {
                continue;
            }
            String m = msg.toLowerCase(Locale.ROOT);
            if (m.contains("communications link failure")
                    || m.contains("connection refused")
                    || m.contains("could not open jpa entitymanager")
                    || m.contains("failed to obtain jdbc connection")
                    || m.contains("unable to obtain isolated jdbc connection")) {
                return true;
            }
        }
        return false;
    }
}
