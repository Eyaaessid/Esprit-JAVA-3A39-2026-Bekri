package tn.esprit.utils;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherService {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public WeatherData fetchCurrentWeather() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
            return WeatherData.unavailable();
        }

        String apiKey = safe(props.getProperty("openweather.api.key"));
        String city = safe(props.getProperty("openweather.city"));
        if (city == null) city = "Tunis";
        if (apiKey == null || apiKey.equalsIgnoreCase("YOUR_KEY_HERE")) {
            return WeatherData.unavailable();
        }

        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity
                    + "&appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&units=metric&lang=fr";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return WeatherData.unavailable();
            }

            String body = response.body();
            String parsedCity = str(body, "\"name\"\\s*:\\s*\"([^\"]+)\"");
            String description = str(body, "\"description\"\\s*:\\s*\"([^\"]+)\"");
            Double temp = dbl(body, "\"temp\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
            Integer humidity = integer(body, "\"humidity\"\\s*:\\s*(\\d+)");
            Double wind = dbl(body, "\"speed\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

            if (temp == null || humidity == null || wind == null) {
                return WeatherData.unavailable();
            }
            return new WeatherData(
                    parsedCity == null ? city : parsedCity,
                    description == null ? "clair" : description,
                    temp,
                    humidity,
                    wind,
                    true
            );
        } catch (Exception ignored) {
            return WeatherData.unavailable();
        }
    }

    private static String safe(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String str(String content, String regex) {
        Matcher m = Pattern.compile(regex).matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private static Double dbl(String content, String regex) {
        String s = str(content, regex);
        return s == null ? null : Double.parseDouble(s);
    }

    private static Integer integer(String content, String regex) {
        String s = str(content, regex);
        return s == null ? null : Integer.parseInt(s);
    }
}
