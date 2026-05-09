package tn.esprit.testmental.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import tn.esprit.testmental.dao.QuestionDAO;
import tn.esprit.testmental.model.Question;
import tn.esprit.testmental.model.TestMental;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PasserTestController {

    @FXML private Label labelQuestion;
    @FXML private Label labelProgress;
    @FXML private RadioButton radioA;
    @FXML private RadioButton radioB;
    @FXML private RadioButton radioC;

    private ToggleGroup groupAnswers;
    private List<String> userAnswers = new ArrayList<>();
    private List<Question> questions = new ArrayList<>();
    private int index = 0;
    private int score = 0;
    private TestMental currentTest;

    private QuestionDAO questionDAO = new QuestionDAO();

    private static final String EMAIL_DEST     = "adembenamara880@gmail.com";
    private static final String EMAIL_FROM     = "adembenamara880@gmail.com";
    @FXML private Label labelTimer;
    private javafx.animation.Timeline timeline;
    private int secondesRestantes;
    @FXML
    public void initialize() {
        groupAnswers = new ToggleGroup();
        radioA.setToggleGroup(groupAnswers);
        radioB.setToggleGroup(groupAnswers);
        radioC.setToggleGroup(groupAnswers);
    }
    @FXML
    private void retourDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/community/user-dashboard.fxml")
            );
            Parent root = loader.load(); //
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void initTest(TestMental test) {
        this.currentTest = test;
        List<Question> all = questionDAO.afficher();
        questions.clear();
        userAnswers.clear();
        for (Question q : all) {
            if (q.getTestMentalId() == test.getId()) {
                questions.add(q);
            }
        }
        if (questions.isEmpty()) {
            labelQuestion.setText("Aucune question disponible");
            labelProgress.setText("0/0");
            return;
        }
        index = 0;
        score = 0;
        Platform.runLater(this::showQuestion);
        // Démarrer timer
        secondesRestantes = currentTest.getDuree() * 60;
        startTimer();
    }

    private void showQuestion() {
        Question q = questions.get(index);
        labelQuestion.setText(q.getContenu());
        radioA.setText(q.getChoixA());
        radioB.setText(q.getChoixB());
        radioC.setText(q.getChoixC());
        labelProgress.setText("Question " + (index + 1) + "/" + questions.size());
        groupAnswers.selectToggle(null);
    }
    private void startTimer() {
        if (timeline != null) timeline.stop();

        timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(1),
                        e -> {
                            secondesRestantes--;
                            int min = secondesRestantes / 60;
                            int sec = secondesRestantes % 60;
                            labelTimer.setText(String.format("⏱ %02d:%02d", min, sec));

                            // Rouge quand moins de 60 secondes
                            if (secondesRestantes <= 60) {
                                labelTimer.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            }

                            // Temps écoulé
                            if (secondesRestantes <= 0) {
                                timeline.stop();
                                Platform.runLater(() -> {
                                    new Alert(Alert.AlertType.WARNING,
                                            "⏰ Temps écoulé ! Le test se termine automatiquement.")
                                            .showAndWait();
                                    finishTest();
                                });
                            }
                        }
                )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }
    @FXML
    private void next() {
        RadioButton selected = (RadioButton) groupAnswers.getSelectedToggle();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez choisir une réponse !").showAndWait();
            return;
        }

        Question q = questions.get(index);
        userAnswers.add(selected.getText());

        String bonneReponse = q.getBonneReponse().trim().toUpperCase();
        String reponseUser = "";
        if (selected == radioA) reponseUser = "A";
        else if (selected == radioB) reponseUser = "B";
        else if (selected == radioC) reponseUser = "C";

        if (reponseUser.equals(bonneReponse)) score++;

        index++;
        if (index < questions.size()) {
            showQuestion();
        } else {
            finishTest();
        }
    }

    private void finishTest() {
        if (timeline != null) timeline.stop();
        double finalScore = (double) score / questions.size();
        String interpretation = getInterpretation(currentTest.getTitre(), finalScore);

        // Affichage immédiat
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat du test");
        alert.setHeaderText("Score: " + score + "/" + questions.size()
                + " (" + (int)(finalScore * 100) + "%)");
        alert.setContentText(interpretation);
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();

        // Groq + Mailjet en arrière-plan
        double scoreFinal = finalScore;
        String interpFinal = interpretation;

        new Thread(() -> {
            String analyseIA = callGroqAPI(buildPrompt());
            String sujet = "Résultat Test Mental : " + currentTest.getTitre();
            // ✅ analyseIA passé en paramètre
            String corps = buildEmailBody(scoreFinal, interpFinal, analyseIA);
            sendMailjet(sujet, corps);
        }).start();
    }

    // ✅ analyseIA en paramètre
    private String buildEmailBody(double finalScore, String interpretation, String analyseIA) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RÉSULTAT DU TEST MENTAL ===\n\n");
        sb.append("Test     : ").append(currentTest.getTitre()).append("\n");
        sb.append("Score    : ").append(score).append("/").append(questions.size())
                .append(" (").append((int)(finalScore * 100)).append("%)\n\n");
        sb.append("Interprétation :\n").append(interpretation).append("\n\n");
        sb.append("Analyse IA (Groq) :\n").append(analyseIA).append("\n\n");
        sb.append("--- Détail des réponses ---\n");
        for (int i = 0; i < questions.size(); i++) {
            sb.append("Q").append(i + 1).append(": ").append(questions.get(i).getContenu()).append("\n");
            sb.append("   Réponse: ").append(userAnswers.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String buildPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un psychologue. Analyse ce test mental en 5 lignes max.\n\n");
        for (int i = 0; i < questions.size(); i++) {
            prompt.append("Q").append(i+1).append(": ").append(questions.get(i).getContenu()).append("\n");
            prompt.append("Réponse: ").append(userAnswers.get(i)).append("\n\n");
        }
        prompt.append("Score: ").append(score).append("/").append(questions.size());
        return prompt.toString();
    }

    private String getInterpretation(String nomTest, double score) {
        String nom = nomTest.toLowerCase();

        if (nom.contains("stress") || nom.contains("travail")) {
            if (score >= 0.75) return "✅ NORMAL — Vous gérez bien le stress au travail.";
            else if (score >= 0.5) return "⚠️ LÉGER — Quelques signes de stress, pensez à vous reposer.";
            else if (score >= 0.25) return "🔶 MODÉRÉ — Niveau de stress préoccupant, consultez un professionnel.";
            else return "🔴 SÉVÈRE — Stress élevé détecté, consultation recommandée urgemment.";
        }

        if (nom.contains("sommeil") || nom.contains("récupération")) {
            if (score >= 0.75) return "✅ NORMAL — Votre qualité de sommeil est bonne.";
            else if (score >= 0.5) return "⚠️ LÉGER — Quelques troubles du sommeil détectés.";
            else if (score >= 0.25) return "🔶 MODÉRÉ — Troubles du sommeil significatifs.";
            else return "🔴 SÉVÈRE — Troubles du sommeil importants, consultez un médecin.";
        }

        if (nom.contains("estime") || nom.contains("confiance")) {
            if (score >= 0.75) return "✅ NORMAL — Bonne estime de soi.";
            else if (score >= 0.5) return "⚠️ LÉGER — Estime de soi légèrement faible.";
            else if (score >= 0.25) return "🔶 MODÉRÉ — Manque de confiance notable.";
            else return "🔴 SÉVÈRE — Faible estime de soi, suivi psychologique recommandé.";
        }

        if (nom.contains("logique") || nom.contains("cognitif")) {
            if (score >= 0.75) return "✅ EXCELLENT — Très bonnes capacités logiques.";
            else if (score >= 0.5) return "✅ BON — Capacités logiques satisfaisantes.";
            else if (score >= 0.25) return "⚠️ MOYEN — Capacités logiques à améliorer.";
            else return "🔶 FAIBLE — Difficultés logiques détectées.";
        }

        if (score >= 0.75) return "✅ NORMAL — Résultat satisfaisant.";
        else if (score >= 0.5) return "⚠️ LÉGER — Quelques points à surveiller.";
        else if (score >= 0.25) return "🔶 MODÉRÉ — Résultat préoccupant.";
        else return "🔴 SÉVÈRE — Résultat critique, consultation recommandée.";
    }

    private void sendMailjet(String sujet, String corps) {
        try {
            java.net.URL url = new java.net.URL("https://api.mailjet.com/v3.1/send");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String credentials = "7e8cf8ebbb4ed978789641433fe10ff9:07655310f4f8a500ef7936490e04742b";
            String encoded = java.util.Base64.getEncoder().encodeToString(
                    credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);

            String safeCorps = corps
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            String json = "{"
                    + "\"Messages\":[{"
                    + "\"From\":{\"Email\":\"adembenamara880@gmail.com\",\"Name\":\"Test Mental\"},"
                    + "\"To\":[{\"Email\":\"adembenamara880@gmail.com\",\"Name\":\"Admin\"}],"
                    + "\"Subject\":\"" + sujet + "\","
                    + "\"TextPart\":\"" + safeCorps + "\""
                    + "}]"
                    + "}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            System.out.println("Mailjet response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String callGroqAPI(String prompt) {
        try {
            java.net.URL url = new java.net.URL("https://api.groq.com/openai/v1/chat/completions");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
           conn.setRequestProperty("Authorization", "Bearer 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");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String safePrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String json = "{"
                    + "\"model\":\"llama-3.3-70b-versatile\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}],"
                    + "\"max_tokens\":300"
                    + "}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            String raw = response.toString();
            int start = raw.indexOf("\"content\":\"") + 11;
            int end = raw.indexOf("\"", start);
            if (start > 10 && end > start) {
                return raw.substring(start, end)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
            }
            return "Analyse IA indisponible.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur Groq.";
        }
    }
}