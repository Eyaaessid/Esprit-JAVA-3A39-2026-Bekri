package tn.esprit.utils;

import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                PROPERTIES.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String value = PROPERTIES.getProperty(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
