package com.bekri.services;

import com.bekri.utils.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client dédié à l'endpoint /api/profil-psychologique.
 */
public class ProfilClient {

    private static final String URL = "http://localhost:8081/api/profil-psychologique";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public record ProfilData(Integer scoreGlobal, String profilType,
                              String dateEvaluation, String aiFeedback) {}

    public static ProfilData getProfil() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode json = MAPPER.readTree(response.body());
            return new ProfilData(
                    json.has("scoreGlobal") ? json.get("scoreGlobal").asInt() : 0,
                    json.has("profilType") ? json.get("profilType").asText() : "—",
                    json.has("dateEvaluation") && !json.get("dateEvaluation").isNull()
                            ? json.get("dateEvaluation").asText() : null,
                    json.has("aiFeedback") && !json.get("aiFeedback").isNull()
                            ? json.get("aiFeedback").asText() : null
            );
        }
        throw new Exception("Profil introuvable (HTTP " + response.statusCode() + ")");
    }

    public static boolean exists() {
        try {
            getProfil();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
