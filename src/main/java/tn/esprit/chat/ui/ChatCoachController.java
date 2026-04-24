package tn.esprit.chat.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.chat.service.ChatProviders;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

public class ChatCoachController implements Initializable {

    @FXML private Label subtitleLabel;
    @FXML private Label scopeLabel;
    @FXML private Label providerLabel;
    @FXML private Label errorLabel;

    @FXML private ScrollPane messagesScroll;
    @FXML private VBox messagesBox;

    @FXML private TextField inputField;

    private final ChatProviders providers = new ChatProviders();

    // Greetings allowed (normal chat)
    private static final Set<String> GREETING_KEYWORDS = Set.of(
            "salut", "bonjour", "coucou", "hello", "hey", "hi",
            "ca va", "ça va", "cv", "hru", "how are you"
    );

    // Health/wellbeing keywords (expand as you like)
    private static final Set<String> HEALTH_KEYWORDS = Set.of(
            "sante", "santé", "bien-etre", "bien etre", "bienetre", "forme", "habitude", "habitudes", "routine",
            "stress", "anx", "anxi", "angoisse", "humeur", "triste", "deprime", "déprime", "burnout",
            "sommeil", "dorm", "insomnie", "fatigue", "energie", "énergie",
            "nutrition", "manger", "poids", "hydrat", "eau",
            "activité", "activite", "sport", "marche", "respiration",
            "motivation", "social", "relation", "confiance", "relax",
            "douleur", "mal", "tete", "tête"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startNewChat();
    }

    @FXML
    private void handleSend() {
        if (errorLabel != null) errorLabel.setText("");

        String msg = inputField == null ? "" : inputField.getText();
        msg = msg == null ? "" : msg.trim();
        if (msg.isBlank()) return;

        addUserBubble(msg);
        inputField.clear();

        // 1) greeting -> local reply
        if (isGreeting(msg)) {
            addAssistantBubble("""
Salut ! 🙂
Tu veux parler de quoi côté santé/bien-être aujourd’hui ?
(Sommeil, stress, humeur, énergie, nutrition, activité…)
""".trim());
            return;
        }

        // 2) generic health goal -> ask clarification (still allowed)
        if (looksLikeGenericHealthGoal(msg) && !looksLikeSpecificHealthTopic(msg)) {
            addAssistantBubble("""
Bien sûr. Pour améliorer ta santé, tu veux agir surtout sur quoi ?
1) Sommeil  2) Stress/humeur  3) Activité  4) Nutrition  5) Énergie
""".trim());
            return;
        }

        // 3) hard gate: not health -> refuse (DO NOT call Groq)
        if (!looksLikeHealth(msg)) {
            addAssistantBubble("""
Je suis spécialisée uniquement dans le bien-être et la santé.
Je ne peux pas vous aider sur ce sujet. Avez-vous des questions sur votre santé ou votre bien-être ?
""".trim());
            return;
        }

        // 4) health -> call Groq
        final String userMsg = msg;
        final String systemPrompt = buildSystemPrompt();

        Thread worker = new Thread(() -> {
            try {
                ChatProviders.ChatReply reply = providers.chat(systemPrompt, userMsg);
                Platform.runLater(() -> {
                    if (providerLabel != null) providerLabel.setText("Provider: " + reply.providerName());
                    addAssistantBubble(reply.text());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (errorLabel != null) errorLabel.setText("Erreur: " + safe(e.getMessage()));
                    addAssistantBubble("Je n’arrive pas à répondre pour le moment. Vérifiez votre clé Groq puis réessayez.");
                });
            }
        }, "coach-chat-worker");

        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleNewChat() {
        startNewChat();
    }

    private void startNewChat() {
        if (messagesBox != null) messagesBox.getChildren().clear();
        if (providerLabel != null) providerLabel.setText("Provider: -");
        if (errorLabel != null) errorLabel.setText("");

        if (subtitleLabel != null) {
            subtitleLabel.setText("Je réponds uniquement aux sujets santé/bien-être (stress, sommeil, humeur, activité, nutrition...).");
        }
        if (scopeLabel != null) {
            scopeLabel.setText("Vous pouvez dire “salut”, puis poser votre question santé/bien-être.");
        }

        addAssistantBubble("""
Bonjour ! Je suis votre coach bien-être.
Comment vous sentez-vous aujourd’hui ? (stress, sommeil, humeur, énergie, activité, nutrition)
""".trim());
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleGoCheckIn() {
        try {
            SceneManager.switchTo("suivi-today");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleGoWeekly() {
        try {
            SceneManager.switchTo("weekly-insight");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleReload() { }

    private String buildSystemPrompt() {
        return """
You are an assistant, a caring and encouraging wellness coach on the Bekri platform.

LANGUAGE RULE:
- Detect the language of EACH user message and reply in the same language. Never mix languages.

EXPERTISE DOMAIN — You ONLY answer questions about:
- Mental health (stress, anxiety, depression, emotions)
- Physical health (exercise, pain, fatigue, recovery)
- Sleep
- Nutrition and hydration
- Wellness habits and motivation

OFF-TOPIC RULE — If outside the domain:
- French: "Je suis spécialisée uniquement dans le bien-être et la santé. Je ne peux pas vous aider sur ce sujet. Avez-vous des questions sur votre santé ou votre bien-être ?"
- English: "I'm only specialized in wellness and health topics. I can't help with that subject. Do you have any questions about your health or well-being?"

STYLE:
- Warm, short (3–6 sentences), actionable steps, end with a question.
- No diagnosis, no prescriptions. Encourage professional help when needed.
""".trim();
    }

    private boolean looksLikeHealth(String msg) {
        String s = normalize(msg);
        for (String k : HEALTH_KEYWORDS) {
            if (s.contains(normalize(k))) return true;
        }
        return false;
    }

    private boolean looksLikeSpecificHealthTopic(String msg) {
        String s = normalize(msg);
        return s.contains("sommeil") || s.contains("dorm") || s.contains("stress") || s.contains("humeur")
                || s.contains("nutrition") || s.contains("manger") || s.contains("sport") || s.contains("activite")
                || s.contains("energie") || s.contains("fatigue") || s.contains("hydrat") || s.contains("eau")
                || s.contains("douleur") || s.contains("mal");
    }

    private boolean looksLikeGenericHealthGoal(String msg) {
        String s = normalize(msg);
        return (s.contains("sante") || s.contains("forme") || s.contains("bien etre") || s.contains("bien-etre"))
                && (s.contains("amelior") || s.contains("ameliore") || s.contains("comment") || s.contains("je veux"));
    }

    private boolean isGreeting(String msg) {
        String s = normalize(msg);
        for (String g : GREETING_KEYWORDS) {
            if (s.contains(normalize(g))) return true;
        }
        // short greetings
        return s.equals("salut") || s.equals("hi") || s.equals("hello") || s.equals("hey");
    }

    private void addUserBubble(String text) {
        VBox bubble = new VBox();
        bubble.getStyleClass().add("chat-bubble-user");
        Label label = new Label(text);
        label.setWrapText(true);
        bubble.getChildren().add(label);

        HBox row = new HBox();
        row.setStyle("-fx-alignment: CENTER_RIGHT;");
        row.getChildren().add(bubble);

        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addAssistantBubble(String text) {
        VBox bubble = new VBox();
        bubble.getStyleClass().add("chat-bubble-assistant");
        Label label = new Label(text);
        label.setWrapText(true);
        bubble.getChildren().add(label);

        HBox row = new HBox();
        row.setStyle("-fx-alignment: CENTER_LEFT;");
        row.getChildren().add(bubble);

        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (messagesScroll == null) return;
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("ù", "u").replace("î", "i")
                .replace("ô", "o").replace("ç", "c")
                .trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}