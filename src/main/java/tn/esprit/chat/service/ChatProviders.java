package tn.esprit.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.utils.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class    ChatProviders {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public record ChatReply(String providerName, String text) {}

    /**
     * Groq-only chat.
     */
    public ChatReply chat(String systemPrompt, String userMessage) throws Exception {
        String groqKey = AppConfig.get("groq.api.key");
        if (groqKey == null || groqKey.isBlank()) {
            throw new IllegalStateException("Clé API manquante: groq.api.key (config.properties).");
        }
        return chatGroq(groqKey, systemPrompt, userMessage);
    }

    private ChatReply chatGroq(String apiKey, String systemPrompt, String userMessage) throws Exception {
        String model = AppConfig.get("groq.model");
        if (model == null || model.isBlank()) {
            model = "llama-3.1-8b-instant";
        }

        JsonNode payload = MAPPER.createObjectNode()
                .put("model", model)
                .put("temperature", 0.4)
                .put("max_tokens", 700)
                .set("messages", MAPPER.createArrayNode()
                        .add(MAPPER.createObjectNode().put("role", "system").put("content", systemPrompt))
                        .add(MAPPER.createObjectNode().put("role", "user").put("content", userMessage)));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(25))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Groq HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = MAPPER.readTree(resp.body());
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Groq réponse vide.");
        }

        return new ChatReply("Groq", text.trim());
    }
}