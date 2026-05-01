package tn.esprit.services;

import tn.esprit.models.MeteoInfo;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class MeteoService {

    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";

    public CompletableFuture<MeteoInfo> getMeteoAsync(String ville) {
        return CompletableFuture.supplyAsync(() -> getMeteo(ville));
    }

    public MeteoInfo getMeteo(String ville) {
        String villeSafe = (ville == null || ville.isBlank()) ? "" : ville.trim();
        if (villeSafe.isBlank()) {
            return getMeteoIndisponible("Lieu non renseigné", "Lieu de l'événement manquant.");
        }

        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return getMeteoIndisponible(villeSafe, "Clé API météo absente. Configurez OPENWEATHER_API_KEY.");
        }

        try {
            String urlString = API_URL + "?q=" + URLEncoder.encode(villeSafe, StandardCharsets.UTF_8) +
                    "&appid=" + apiKey + "&units=metric&lang=fr";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = readResponse(conn.getInputStream());
                return parseMeteoResponse(response.toString(), villeSafe);
            }

            StringBuilder errorResponse = readResponse(conn.getErrorStream());
            if (responseCode == 401) {
                return getMeteoIndisponible(villeSafe, "Clé API météo invalide ou expirée.");
            }
            if (responseCode == 404) {
                return getMeteoIndisponible(villeSafe, "Lieu non reconnu par le service météo.");
            }
            if (responseCode >= 500) {
                return getMeteoIndisponible(villeSafe, "Service météo temporairement indisponible.");
            }
            if (errorResponse.length() > 0) {
                return getMeteoIndisponible(villeSafe, "Erreur météo: " + errorResponse);
            }
            return getMeteoIndisponible(villeSafe, "Météo indisponible pour le moment.");
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la météo: " + e.getMessage());
            return getMeteoIndisponible(villeSafe, "Impossible de joindre le service météo.");
        }
    }

    private StringBuilder readResponse(InputStream inputStream) {
        StringBuilder response = new StringBuilder();
        if (inputStream == null) {
            return response;
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (Exception ignored) {
        }
        return response;
    }

    private boolean isPlaceholderKey(String apiKey) {
        String key = apiKey == null ? "" : apiKey.trim().toLowerCase();
        return key.isBlank() || key.contains("votre_cle") || key.contains("your_api_key");
    }

    private MeteoInfo parseMeteoResponse(String jsonResponse, String requestedCity) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            if (json.has("cod") && json.getInt("cod") != 200) {
                String message = json.optString("message", "Météo indisponible pour le moment.");
                return getMeteoIndisponible(requestedCity, message);
            }

            String ville = json.optString("name", requestedCity);
            double temperature = json.getJSONObject("main").getDouble("temp");
            JSONArray weatherArray = json.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            String description = weather.optString("description", "État météo indisponible");
            String icone = weather.optString("icon", "");
            String conseil = genererConseil(description, temperature);

            return new MeteoInfo(ville, temperature, description, icone, conseil);
        } catch (Exception e) {
            System.err.println("Erreur parsing météo: " + e.getMessage());
            return getMeteoIndisponible(requestedCity, "Réponse météo invalide.");
        }
    }

    private String getApiKey() {
        String apiKey = System.getenv("OPENWEATHER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("openweather.api.key");
        }
        if (isPlaceholderKey(apiKey)) {
            return null;
        }
        return apiKey;
    }

    private String genererConseil(String description, double temperature) {
        description = description.toLowerCase();

        if (description.contains("pluie") || description.contains("rain")) {
            return "Prévoyez un lieu couvert ou des parapluies.";
        } else if (description.contains("orage") || description.contains("thunder")) {
            return "Privilégiez un lieu intérieur sécurisé.";
        } else if (description.contains("neige") || description.contains("snow")) {
            return "Vérifiez l'accessibilité du lieu.";
        } else if (temperature > 30) {
            return "Prévoyez de l'eau et des zones ombragées.";
        } else if (temperature < 5) {
            return "Prévoyez un lieu chauffé.";
        } else if (description.contains("nuage") || description.contains("cloud")) {
            return "Conditions correctes pour l'événement.";
        } else if (description.contains("clair") || description.contains("clear")) {
            return "Conditions favorables pour une activité extérieure.";
        } else {
            return "Consultez la météo avant le début de l'événement.";
        }
    }

    private MeteoInfo getMeteoIndisponible(String ville, String reason) {
        return new MeteoInfo(
                ville,
                Double.NaN,
                "Météo indisponible pour le moment",
                "",
                reason,
                false
        );
    }
}
