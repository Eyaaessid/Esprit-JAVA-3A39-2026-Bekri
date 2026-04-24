package tn.esprit.profil.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GroqAiService {

    private final String apiKey;
    private final HttpClient httpClient;

    public GroqAiService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(25))
                .build();
    }

    public String generateEmotionalInsight(int scoreGlobal, String profilType,
                                           int section1Sum, int section2Sum, int section3Sum) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String systemMessage = "Tu es l'AI Emotional Insight Assistant. Tu génères une analyse psychologique "
                    + "personnalisée en 3 parties. Jamais de diagnostic médical. Jamais d'objectifs "
                    + "ou de tâches chiffrées. Réponds uniquement en français.";
            String userMessage = "Tu es un assistant bien-être bienveillant et professionnel. Voici les résultats "
                    + "d'une évaluation psychologique. Génère une analyse personnalisée en TROIS parties.\n\n"
                    + "DONNÉES :\n"
                    + "- Score global (0–100, 100 = détresse maximale) : " + scoreGlobal + "\n"
                    + "- Profil : " + safe(profilType) + "\n"
                    + "- Score section Humeur/émotions (sur 20) : " + section1Sum + "\n"
                    + "- Score section Charge mentale/stress (sur 20) : " + section2Sum + "\n"
                    + "- Score section Sommeil/lien social (sur 20) : " + section3Sum + "\n\n"
                    + "Rédige exactement trois parties (sans titres numérotés visibles) :\n"
                    + "1) RÉSUMÉ ÉMOTIONNEL : explique ce que ce profil signifie émotionnellement, "
                    + "en t'appuyant sur les scores de chaque section.\n"
                    + "2) INTERPRÉTATION : niveau de vulnérabilité, points forts ou signaux d'alerte, "
                    + "ton soutenant, jamais alarmant, jamais médical.\n"
                    + "3) CONSEILS DE POSTURE MENTALE : uniquement conscience émotionnelle, "
                    + "bienveillance envers soi, recadrage cognitif, pauses mentales. "
                    + "INTERDIT : objectifs chiffrés, tâches, 'dormir 8h', 'boire X verres'.\n\n"
                    + "Réponds directement par les trois paragraphes, sans préambule.";

            String body = "{"
                    + "\"model\":\"llama-3.1-8b-instant\","
                    + "\"temperature\":0.5,"
                    + "\"max_tokens\":900,"
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemMessage) + "\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("Groq error status: " + response.statusCode() + " body=" + response.body());
                return null;
            }
            return extractContent(response.body());
        } catch (Exception e) {
            System.err.println("GroqAiService error: " + e.getMessage());
            return null;
        }
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
                    case '/' -> sb.append('/');
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
        String content = sb.toString().trim();
        return content.isEmpty() ? null : content;
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
