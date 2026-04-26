package org.example.community.service;

import org.example.community.model.RiskAnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

    public RiskAnalysisResult analyze(String content) {
        String text = content == null ? "" : content.trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return new RiskAnalysisResult("neutral", "low", false, List.of());
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
