package tn.esprit.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tn.esprit.plan.model.WeeklyPlan;
import tn.esprit.utils.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeeklyPlanService {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public record FormData(
            double poidsKg,
            double tailleCm,
            int age,
            String sexe,
            String objectif,
            String exercice,
            String restrictions
    ) {}

    public double computeImc(double poidsKg, double tailleCm) {
        if (poidsKg <= 0 || tailleCm <= 0) return 0;
        double tailleM = tailleCm / 100.0;
        double imc = poidsKg / (tailleM * tailleM);
        return Math.round(imc * 10.0) / 10.0;
    }

    public WeeklyPlan generateWeeklyPlan(FormData form) throws Exception {
        return generateWeeklyPlan(form, false);
    }

    private WeeklyPlan generateWeeklyPlan(FormData form, boolean strictRetry) throws Exception {
        String apiKey = AppConfig.get("groq.api.key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Clé Groq manquante. Ajoutez groq.api.key dans config.properties.");
        }
        String model = AppConfig.get("groq.model");
        if (model == null || model.isBlank()) {
            model = "llama-3.1-8b-instant";
        }

        double imc = computeImc(form.poidsKg, form.tailleCm);
        String prompt = buildPrompt(form, imc, strictRetry);

        JsonNode payload = MAPPER.createObjectNode()
                .put("model", model)
                .put("temperature", 0.6)
                .put("max_tokens", 3000)
                .put("stream", false)
                .set("messages", MAPPER.createArrayNode()
                        .add(MAPPER.createObjectNode()
                                .put("role", "system")
                                .put("content", "Tu es un coach bien-être et nutritionniste expert. Tu réponds UNIQUEMENT en JSON valide, sans texte libre, sans markdown, sans backticks."))
                        .add(MAPPER.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(35))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Groq a répondu avec le code HTTP " + response.statusCode());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("");

        try {
            WeeklyPlan plan = parseWeeklyPlan(content);
            if (plan.getImc() == null || plan.getImc() == 0) {
                plan.setImc(imc);
            }
            ensureDays(plan);
            return plan;
        } catch (Exception parseEx) {
            if (!strictRetry) {
                return generateWeeklyPlan(form, true);
            }
            throw parseEx;
        }
    }

    private WeeklyPlan parseWeeklyPlan(String aiContent) throws Exception {
        String cleaned = cleanupJson(aiContent);
        WeeklyPlan plan = MAPPER.readValue(cleaned, WeeklyPlan.class);
        if (plan == null) {
            throw new IllegalStateException("Réponse IA vide.");
        }
        if (plan.getResume() == null || plan.getResume().isBlank()) {
            throw new IllegalStateException("JSON invalide: champ resume manquant.");
        }
        return plan;
    }

    private String cleanupJson(String content) {
        if (content == null) return "";
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }
        // Sometimes Groq adds leading/trailing text; keep best-effort extract first/last braces.
        int first = cleaned.indexOf('{');
        int last = cleaned.lastIndexOf('}');
        if (first >= 0 && last > first) {
            cleaned = cleaned.substring(first, last + 1).trim();
        }
        return cleaned;
    }

    private String buildPrompt(FormData form, double imc, boolean strictRetry) {
        String objectifLabel = mapObjectif(form.objectif);
        String exerciceLabel = mapExercice(form.exercice);
        String sexe = safe(form.sexe);
        String restrictions = safe(form.restrictions);

        StringBuilder sb = new StringBuilder();
        sb.append("Génère un plan hebdomadaire nutrition + sport sur 7 jours.\n")
                .append("Profil utilisateur:\n")
                .append("- poids_kg: ").append(form.poidsKg).append("\n")
                .append("- taille_cm: ").append(form.tailleCm).append("\n")
                .append("- age: ").append(form.age).append("\n")
                .append("- sexe: ").append(sexe).append("\n")
                .append("- objectif: ").append(objectifLabel).append("\n")
                .append("- activité/exercice: ").append(exerciceLabel).append("\n")
                .append("- restrictions: ").append(restrictions.isBlank() ? "aucune" : restrictions).append("\n")
                .append("- imc: ").append(String.format(Locale.US, "%.1f", imc)).append("\n\n");

        sb.append("Contraintes IMPORTANTES:\n")
                .append("- Réponds UNIQUEMENT avec un OBJET JSON valide.\n")
                .append("- Aucun texte hors JSON. Pas de markdown. Pas de backticks.\n")
                .append("- Les clés doivent être EXACTEMENT celles du schéma.\n")
                .append("- Les jours doivent être exactement: lundi, mardi, mercredi, jeudi, vendredi, samedi, dimanche.\n")
                .append("- Mardi et dimanche: mettre une journée de repos dans exercices (type='Repos', duree='—', intensite='—', description='Repos').\n")
                .append("- Les champs repas doivent contenir des propositions réalistes et cohérentes avec l'objectif.\n\n");

        if (strictRetry) {
            sb.append("DERNIER RAPPEL: si tu ajoutes autre chose que du JSON strict, la réponse est rejetée.\n\n");
        }

        sb.append("Schéma JSON STRICT à respecter exactement:\n")
                .append("{\n")
                .append("  \"resume\": \"...\",\n")
                .append("  \"imc\": 0.0,\n")
                .append("  \"calories_journalieres\": 0,\n")
                .append("  \"conseils_generaux\": [\"...\"],\n")
                .append("  \"repas\": {\n")
                .append("    \"lundi\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"},\n")
                .append("    \"mardi\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"},\n")
                .append("    \"mercredi\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"},\n")
                .append("    \"jeudi\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"},\n")
                .append("    \"vendredi\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"},\n")
                .append("    \"samedi\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"},\n")
                .append("    \"dimanche\": {\"petit_dejeuner\":\"...\",\"dejeuner\":\"...\",\"diner\":\"...\",\"collation\":\"...\"}\n")
                .append("  },\n")
                .append("  \"exercices\": {\n")
                .append("    \"lundi\": {\"type\":\"...\",\"duree\":\"...\",\"intensite\":\"...\",\"description\":\"...\"},\n")
                .append("    \"mardi\": {\"type\":\"Repos\",\"duree\":\"—\",\"intensite\":\"—\",\"description\":\"Repos\"},\n")
                .append("    \"mercredi\": {\"type\":\"...\",\"duree\":\"...\",\"intensite\":\"...\",\"description\":\"...\"},\n")
                .append("    \"jeudi\": {\"type\":\"...\",\"duree\":\"...\",\"intensite\":\"...\",\"description\":\"...\"},\n")
                .append("    \"vendredi\": {\"type\":\"...\",\"duree\":\"...\",\"intensite\":\"...\",\"description\":\"...\"},\n")
                .append("    \"samedi\": {\"type\":\"...\",\"duree\":\"...\",\"intensite\":\"...\",\"description\":\"...\"},\n")
                .append("    \"dimanche\": {\"type\":\"Repos\",\"duree\":\"—\",\"intensite\":\"—\",\"description\":\"Repos\"}\n")
                .append("  },\n")
                .append("  \"hydratation\": {\"litres_par_jour\": 0.0, \"conseils\": [\"...\"]},\n")
                .append("  \"sommeil\": {\"heures_recommandees\": \"7-9h\", \"conseils\": [\"...\"]}\n")
                .append("}\n");

        return sb.toString();
    }

    private void ensureDays(WeeklyPlan plan) {
        List<String> days = List.of("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche");

        if (plan.getRepas() == null) {
            plan.setRepas(new LinkedHashMap<>());
        }
        if (plan.getExercices() == null) {
            plan.setExercices(new LinkedHashMap<>());
        }

        for (String d : days) {
            plan.getRepas().putIfAbsent(d, new WeeklyPlan.RepasDay());
            plan.getExercices().putIfAbsent(d, defaultExerciseForDay(d));
        }
    }

    private WeeklyPlan.ExerciceDay defaultExerciseForDay(String day) {
        WeeklyPlan.ExerciceDay e = new WeeklyPlan.ExerciceDay();
        if ("mardi".equals(day) || "dimanche".equals(day)) {
            e.setType("Repos");
            e.setDuree("—");
            e.setIntensite("—");
            e.setDescription("Repos");
        } else {
            e.setType("—");
            e.setDuree("—");
            e.setIntensite("—");
            e.setDescription("—");
        }
        return e;
    }

    private String mapObjectif(String objectif) {
        String o = safe(objectif).toLowerCase(Locale.ROOT).trim();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("perte", "Perte de poids");
        map.put("prise", "Prise de masse");
        map.put("maintien", "Maintien / santé");
        map.put("sante", "Maintien / santé");
        map.put("performance", "Performance");

        for (Map.Entry<String, String> e : map.entrySet()) {
            if (o.contains(e.getKey())) return e.getValue();
        }
        return objectif == null || objectif.isBlank() ? "Maintien / santé" : objectif;
    }

    private String mapExercice(String exercice) {
        String a = safe(exercice).toLowerCase(Locale.ROOT).trim();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("debut", "Débutant");
        map.put("inter", "Intermédiaire");
        map.put("avance", "Avancé");
        map.put("faible", "Faible");
        map.put("modere", "Modéré");
        map.put("intense", "Intense");
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (a.contains(e.getKey())) return e.getValue();
        }
        return exercice == null || exercice.isBlank() ? "Modéré" : exercice;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
