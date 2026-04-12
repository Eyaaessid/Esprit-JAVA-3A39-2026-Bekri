package com.bekri.services;

import com.bekri.models.AuthResponse;
import com.bekri.models.UtilisateurResponse;
import com.bekri.utils.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP vers l'API Spring Boot (port 8081).
 */
public class ApiClient {

    public static final String HOST = "http://localhost:8081";
    private static final String BASE_URL = HOST + "/api";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static String absoluteUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        return HOST + (pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl);
    }

    public static AuthResponse login(String email, String motDePasse) throws Exception {
        String body = MAPPER.writeValueAsString(new LinkedHashMap<>() {{
            put("email", email);
            put("motDePasse", motDePasse);
        }});
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), AuthResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de connexion");
    }

    public static AuthResponse register(String nom, String prenom, String email,
                                         String motDePasse, String telephone,
                                         String dateNaissance) throws Exception {
        String body = MAPPER.writeValueAsString(new LinkedHashMap<>() {{
            put("nom", nom);
            put("prenom", prenom);
            put("email", email);
            put("motDePasse", motDePasse);
            put("telephone", telephone);
            put("dateNaissance", dateNaissance);
        }});
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 201) {
            return MAPPER.readValue(response.body(), AuthResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur d'inscription");
    }

    public static UtilisateurResponse getMe() throws Exception {
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/me"))
                .GET())
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        throw new Exception("Impossible de récupérer le profil");
    }

    public static UtilisateurResponse updateMe(String nom, String prenom, String email,
                                               String telephone, String dateNaissance,
                                               String avatarOrNull, String motDePasseOrNull) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nom", nom);
        map.put("prenom", prenom);
        map.put("email", email);
        map.put("telephone", telephone);
        map.put("dateNaissance", dateNaissance);
        if (avatarOrNull != null) {
            map.put("avatar", avatarOrNull);
        }
        if (motDePasseOrNull != null && !motDePasseOrNull.isBlank()) {
            map.put("motDePasse", motDePasseOrNull);
        }
        String body = MAPPER.writeValueAsString(map);
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/me"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de mise à jour");
    }

    /**
     * POST multipart /api/utilisateurs/me/avatar — renvoie l'utilisateur mis à jour (URL avatar relative).
     */
    public static UtilisateurResponse uploadMyAvatar(Path filePath) throws Exception {
        byte[] fileBytes = Files.readAllBytes(filePath);
        if (fileBytes.length > 2L * 1024 * 1024) {
            throw new Exception("L'image ne doit pas dépasser 2 Mo.");
        }
        String filename = filePath.getFileName().toString();
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String boundary = "JavaFXFormBoundary" + System.currentTimeMillis();
        String crlf = "\r\n";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                + filename.replace("\"", "") + "\"" + crlf).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: " + contentType + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        baos.write(fileBytes);
        baos.write((crlf + "--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/me/avatar"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray())))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        JsonNode err;
        try {
            err = MAPPER.readTree(response.body());
        } catch (Exception e) {
            throw new Exception("Échec du téléversement (HTTP " + response.statusCode() + ")");
        }
        throw new Exception(err.has("message") ? err.get("message").asText() : "Échec du téléversement");
    }

    public static void submitProfil(int scoreGlobal, String profilType,
                                    String aiFeedback) throws Exception {
        String body = MAPPER.writeValueAsString(new LinkedHashMap<>() {{
            put("scoreGlobal", scoreGlobal);
            put("profilType", profilType);
            put("dateEvaluation", java.time.LocalDateTime.now().toString());
            put("aiFeedback", aiFeedback);
        }});
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profil-psychologique"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Erreur lors de l'enregistrement du profil psychologique");
        }
    }

    public static boolean hasProfilPsychologique() {
        try {
            HttpRequest request = withAuth(HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/profil-psychologique"))
                    .GET())
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<UtilisateurResponse> getUtilisateurs(String search, String role, String statut) throws Exception {
        StringBuilder url = new StringBuilder(BASE_URL + "/utilisateurs?");
        if (search != null && !search.isBlank()) {
            url.append("search=").append(search).append("&");
        }
        if (role != null && !role.isBlank()) {
            url.append("role=").append(role).append("&");
        }
        if (statut != null && !statut.isBlank()) {
            url.append("statut=").append(statut).append("&");
        }

        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET())
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            List<UtilisateurResponse> list = new ArrayList<>();
            ArrayNode arr = (ArrayNode) MAPPER.readTree(response.body());
            for (JsonNode node : arr) {
                list.add(MAPPER.treeToValue(node, UtilisateurResponse.class));
            }
            return list;
        }
        throw new Exception("Erreur lors de la récupération des utilisateurs");
    }

    public static UtilisateurResponse getUtilisateurById(int id) throws Exception {
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/" + id))
                .GET())
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        throw new Exception("Utilisateur introuvable");
    }

    public static UtilisateurResponse patchRole(int id, String role) throws Exception {
        String body = MAPPER.writeValueAsString(new LinkedHashMap<>() {{ put("role", role); }});
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/" + id + "/role"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de mise à jour du rôle");
    }

    public static UtilisateurResponse patchStatut(int id, String statut) throws Exception {
        String body = MAPPER.writeValueAsString(new LinkedHashMap<>() {{ put("statut", statut); }});
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/" + id + "/statut"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de mise à jour du statut");
    }

    public static UtilisateurResponse createUtilisateur(String nom, String prenom, String email,
                                                        String motDePasse, String telephone,
                                                        String dateNaissance, String role) throws Exception {
        String body = MAPPER.writeValueAsString(new LinkedHashMap<>() {{
            put("nom", nom);
            put("prenom", prenom);
            put("email", email);
            put("motDePasse", motDePasse);
            put("telephone", telephone);
            put("dateNaissance", dateNaissance);
            put("role", role);
        }});
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 201) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de création");
    }

    /**
     * Mise à jour complète d’un utilisateur (ADMIN ou soi-même). Le mot de passe n’est mis à jour que s’il est non vide.
     */
    public static UtilisateurResponse updateUtilisateur(int id, String nom, String prenom, String email,
                                                        String telephone, String dateNaissance, String role,
                                                        String motDePasseOrNull) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nom", nom);
        map.put("prenom", prenom);
        map.put("email", email);
        map.put("telephone", telephone);
        map.put("dateNaissance", dateNaissance);
        map.put("role", role);
        if (motDePasseOrNull != null && !motDePasseOrNull.isBlank()) {
            map.put("motDePasse", motDePasseOrNull);
        }
        String body = MAPPER.writeValueAsString(map);
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return MAPPER.readValue(response.body(), UtilisateurResponse.class);
        }
        JsonNode err = MAPPER.readTree(response.body());
        throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de mise à jour");
    }

    public static void deleteUtilisateur(int id) throws Exception {
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/utilisateurs/" + id))
                .DELETE())
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            JsonNode err = MAPPER.readTree(response.body());
            throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de suppression");
        }
    }

    /** Suppression définitive en base (ADMIN) — {@code DELETE /api/admin/utilisateurs/{id}}. */
    public static void deleteUtilisateurPermanent(int id) throws Exception {
        HttpRequest request = withAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/admin/utilisateurs/" + id))
                .DELETE())
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            JsonNode err;
            try {
                err = MAPPER.readTree(response.body());
            } catch (Exception e) {
                throw new Exception("Erreur de suppression (HTTP " + response.statusCode() + ")");
            }
            throw new Exception(err.has("message") ? err.get("message").asText() : "Erreur de suppression");
        }
    }

    private static HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
        String token = SessionManager.getInstance().getToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }
}
