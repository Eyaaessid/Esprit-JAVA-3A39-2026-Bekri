package tn.esprit.profil.service;

import tn.esprit.utils.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WellbeingRecommendationService {

    public static class WellbeingRecommendation {
        private final String type;
        private final String label;
        private final String reason;
        private final String icon;

        public WellbeingRecommendation(String type, String label, String reason, String icon) {
            this.type = type;
            this.label = label;
            this.reason = reason;
            this.icon = icon;
        }

        public String getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public String getReason() {
            return reason;
        }

        public String getIcon() {
            return icon;
        }
    }

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "humeur", "sommeil", "activite", "nutrition", "hydratation", "poids"
    );

    public List<WellbeingRecommendation> getRecommendations(int scoreGlobal, String profilType, String prenom) {
        String apiKey = AppConfig.get("groq.api.key");
        if (apiKey == null || apiKey.isBlank()) {
            return getFallbackRecommendations(profilType);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(25))
                    .build();

            String userPrompt = "Tu es un coach bien-être. Score global: " + scoreGlobal + "/100. "
                    + "Profil: " + safe(profilType) + ". Propose 3 objectifs bien-être personnalisés"
                    + (prenom != null && !prenom.isBlank() ? " pour " + prenom : "") + ". "
                    + "Types valides: humeur, sommeil, activite, nutrition, hydratation, poids. "
                    + "Réponds UNIQUEMENT avec ce JSON (sans markdown): "
                    + "[{\"type\":\"sommeil\",\"label\":\"Sommeil\",\"reason\":\"Une phrase courte en français.\"}]";

            String body = "{"
                    + "\"model\":\"llama-3.1-8b-instant\","
                    + "\"temperature\":0.6,"
                    + "\"max_tokens\":400,"
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"" + escapeJson("Tu réponds uniquement par un tableau JSON valide, sans texte avant ou après.") + "\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return getFallbackRecommendations(profilType);
            }

            String content = extractContent(response.body());
            List<WellbeingRecommendation> parsed = parseRecommendations(content);
            return parsed.isEmpty() ? getFallbackRecommendations(profilType) : parsed;
        } catch (Exception e) {
            return getFallbackRecommendations(profilType);
        }
    }

    private List<WellbeingRecommendation> parseRecommendations(String content) {
        List<WellbeingRecommendation> list = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return list;
        }
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return list;
        }
        String array = content.substring(start + 1, end).trim();
        if (array.isBlank()) {
            return list;
        }

        String[] items = array.split("\\},\\s*\\{");
        for (String rawItem : items) {
            if (list.size() >= 4) {
                break;
            }
            String item = rawItem.trim();
            if (!item.startsWith("{")) item = "{" + item;
            if (!item.endsWith("}")) item = item + "}";

            String type = extractJsonValue(item, "type");
            String label = extractJsonValue(item, "label");
            String reason = extractJsonValue(item, "reason");
            if (type == null || label == null || reason == null) {
                continue;
            }
            type = type.toLowerCase(Locale.ROOT);
            if (!ALLOWED_TYPES.contains(type)) {
                continue;
            }
            list.add(new WellbeingRecommendation(type, label, reason, iconForType(type)));
        }
        return list;
    }

    private String extractJsonValue(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private String extractContent(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String key = "\"content\":\"";
        int start = json.indexOf(key);
        if (start < 0) {
            return null;
        }
        start += key.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private List<WellbeingRecommendation> getFallbackRecommendations(String profilType) {
        List<WellbeingRecommendation> list = new ArrayList<>();
        if ("Profil à risque élevé".equals(profilType)) {
            list.add(rec("humeur", "Humeur", "Renforcer l'equilibre emotionnel au quotidien.", "😊"));
            list.add(rec("sommeil", "Sommeil", "Retrouver un rythme plus reparateur et apaisant.", "🌙"));
            list.add(rec("activite", "Activite", "Remettre du mouvement pour relacher la pression mentale.", "🚴"));
            list.add(rec("nutrition", "Nutrition", "Stabiliser l'energie avec des reperes plus soutenants.", "🥗"));
            return list;
        }
        if ("Profil sensible".equals(profilType)) {
            list.add(rec("sommeil", "Sommeil", "Mieux recuperer pour reduire la charge emotionnelle.", "🌙"));
            list.add(rec("activite", "Activite", "Bouger en douceur pour evacuer les tensions.", "🚴"));
            list.add(rec("humeur", "Humeur", "Cultiver des espaces de regulation emotionnelle.", "😊"));
            return list;
        }
        if ("Profil stable".equals(profilType)) {
            list.add(rec("activite", "Activite", "Entretenir une dynamique positive dans le corps et l'esprit.", "🚴"));
            list.add(rec("sommeil", "Sommeil", "Preserver un sommeil regulier pour consolider l'equilibre.", "🌙"));
            list.add(rec("nutrition", "Nutrition", "Soutenir votre vitalite avec des choix simples et reguliers.", "🥗"));
            return list;
        }
        list.add(rec("activite", "Activite", "Poursuivre une routine active qui nourrit votre bien-etre.", "🚴"));
        list.add(rec("nutrition", "Nutrition", "Conserver une alimentation qui soutient votre energie.", "🥗"));
        list.add(rec("hydratation", "Hydratation", "Maintenir une bonne hydratation au fil de la journee.", "💧"));
        return list;
    }

    private WellbeingRecommendation rec(String type, String label, String reason, String icon) {
        return new WellbeingRecommendation(type, label, reason, icon);
    }

    private String iconForType(String type) {
        return switch (type) {
            case "humeur" -> "😊";
            case "sommeil" -> "🌙";
            case "activite" -> "🚴";
            case "nutrition" -> "🥗";
            case "hydratation" -> "💧";
            case "poids" -> "⚖️";
            default -> "✨";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
