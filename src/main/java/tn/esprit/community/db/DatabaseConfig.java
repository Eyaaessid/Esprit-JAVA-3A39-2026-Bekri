package tn.esprit.community.db;

public final class DatabaseConfig {
    private static final String DEFAULT_URL = "jdbc:mysql://127.0.0.1:3306/bekri_db?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private DatabaseConfig() {
    }

    public static String getUrl() {
        return readValue("DB_URL", "db.url", DEFAULT_URL);
    }

    public static String getUser() {
        return readValue("DB_USER", "db.user", DEFAULT_USER);
    }

    public static String getPassword() {
        return readValue("DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
    }

    private static String readValue(String envKey, String propertyKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return defaultValue;
    }
}

