package tn.esprit.community.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import tn.esprit.community.model.RiskAnalysisResult;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class PostModerationService {
    private static final List<String> HIGH_RISK = Arrays.asList(
            "suicide", "suicidal", "kill myself", "killing myself", "self-harm", "self harm",
            "want to die", "end my life", "end it all", "take my life", "hurt myself", "harm myself",
            "i wanna die", "wanna die", "wish i was dead", "je veux mourir", "envie de mourir",
            "me suicider", "je vais me tuer"
    );

    private static final List<String> MEDIUM_RISK = Arrays.asList(
            "worthless", "no reason to live", "give up", "hopeless", "no way out", "can't go on",
            "cant go on", "nothing matters", "idiot", "stupid", "bitch", "fuck", "shit"
    );

    private final String groqApiKey;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public PostModerationService() {
        Properties config = loadConfig();
        this.groqApiKey = config.getProperty("groq.api.key", "").trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public RiskAnalysisResult analyze(String content) {
        String text = content == null ? "" : content.trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return new RiskAnalysisResult("neutral", "low", false, List.of());
        }

        // Prefer Groq API when configured; fallback to local keyword heuristic if unavailable.
        RiskAnalysisResult groqResult = analyzeWithGroq(content);
        if (groqResult != null) {
            return groqResult;
        }

        List<String> signals = new ArrayList<>();
        String risk = "low";

        for (String keyword : HIGH_RISK) {
            if (text.contains(keyword)) {
                signals.add(keyword);
                risk = "high";
            }
        }

        if ("low".equals(risk)) {
            for (String keyword : MEDIUM_RISK) {
                if (text.contains(keyword)) {
                    signals.add(keyword);
                    risk = "medium";
                    break;
                }
            }
        }

        String emotion = detectEmotion(text);
        return new RiskAnalysisResult(emotion, risk, !"low".equals(risk), signals.stream().distinct().toList());
    }

    private RiskAnalysisResult analyzeWithGroq(String rawContent) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return null;
        }
        try {
            String systemMessage =
                    "You are a content moderation assistant for a wellbeing app. "
                            + "You must classify a post into: emotion, risk_level, sensitive, signals. "
                            + "Return ONLY valid JSON (no markdown, no extra text).";

            String userMessage =
                    "Analyze this post content and return JSON with exactly these keys:\n"
                            + "{\n"
                            + "  \"emotion\": one of [\"happy\",\"sad\",\"anxious\",\"stressed\",\"angry\",\"hopeful\",\"neutral\"],\n"
                            + "  \"risk_level\": one of [\"low\",\"medium\",\"high\"],\n"
                            + "  \"sensitive\": boolean,\n"
                            + "  \"signals\": array of short strings (keywords/reasons)\n"
                            + "}\n\n"
                            + "Post content:\n"
                            + rawContent;

            JsonObject payload = new JsonObject();
            payload.addProperty("model", "llama-3.1-8b-instant");
            payload.addProperty("temperature", 0.2);
            payload.addProperty("max_tokens", 220);

            JsonArray messages = new JsonArray();
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", systemMessage);
            messages.add(system);

            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", userMessage);
            messages.add(user);

            payload.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("[PostModerationService] Groq status=" + response.statusCode() + " body=" + response.body());
                return null;
            }

            String content = extractAssistantContent(response.body());
            if (content == null || content.isBlank()) {
                return null;
            }

            JsonObject obj = gson.fromJson(content, JsonObject.class);
            if (obj == null) {
                return null;
            }

            String emotion = safeEnum(obj.get("emotion"), "neutral");
            String risk = safeEnum(obj.get("risk_level"), "low");
            boolean sensitive = obj.has("sensitive") && obj.get("sensitive").isJsonPrimitive() && obj.get("sensitive").getAsBoolean();

            List<String> signals = new ArrayList<>();
            if (obj.has("signals") && obj.get("signals").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("signals")) {
                    if (el != null && el.isJsonPrimitive()) {
                        String s = el.getAsString();
                        if (s != null && !s.isBlank()) {
                            signals.add(s.trim());
                        }
                    }
                }
            }

            // Normalize risk/sensitive relationship
            if ("low".equalsIgnoreCase(risk)) {
                sensitive = false;
            }

            return new RiskAnalysisResult(emotion.toLowerCase(Locale.ROOT), risk.toLowerCase(Locale.ROOT), sensitive, signals.stream().distinct().toList());
        } catch (Exception e) {
            System.err.println("[PostModerationService] Groq analyze failed: " + e.getMessage());
            return null;
        }
    }

    private String extractAssistantContent(String json) {
        // Minimal extraction to avoid pulling a full OpenAI response model; content is in choices[0].message.content
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null) return null;
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            JsonObject first = choices.get(0).getAsJsonObject();
            if (first == null) return null;
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) return null;
            JsonElement content = message.get("content");
            return content == null ? null : content.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safeEnum(JsonElement value, String fallback) {
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        String s = value.getAsString();
        if (s == null || s.isBlank()) {
            return fallback;
        }
        return s.trim();
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = PostModerationService.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {
        }
        return props;
    }

    private String detectEmotion(String text) {
        if (containsAny(text, "happy", "joy", "great", "excited", "grateful")) return "happy";
        if (containsAny(text, "sad", "depressed", "cry", "empty", "lonely")) return "sad";
        if (containsAny(text, "anxious", "anxiety", "panic", "worried", "nervous")) return "anxious";
        if (containsAny(text, "stressed", "overwhelmed", "burnout", "pressure")) return "stressed";
        if (containsAny(text, "angry", "furious", "hate", "rage", "frustrated")) return "angry";
        if (containsAny(text, "hope", "recover", "improve", "better", "progress")) return "hopeful";
        return "neutral";
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}

