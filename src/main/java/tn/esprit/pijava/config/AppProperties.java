package tn.esprit.pijava.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {
    private final Properties properties = new Properties();

    public AppProperties() {
        this("application.properties");
    }

    public AppProperties(String resourceName) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing configuration file: " + resourceName);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load configuration file: " + resourceName, e);
        }
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }
}
