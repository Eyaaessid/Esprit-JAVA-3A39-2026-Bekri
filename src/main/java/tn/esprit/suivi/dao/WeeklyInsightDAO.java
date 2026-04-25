package tn.esprit.suivi.dao;

import tn.esprit.suivi.model.CategorySummary;
import tn.esprit.suivi.model.WeeklyInsightResult;
import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

public class WeeklyInsightDAO {
    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }


    private static final String SQL_WEEK_ANSWERS =
            """
            SELECT
                s.date AS day_date,
                q.category AS category,
                r.valeur AS valeur
            FROM suivi_quotidien s
            JOIN reponse_suivi r ON r.suivi_id = s.id
            JOIN question_evaluation q ON q.id = r.question_id
            WHERE s.utilisateur_id = ?
              AND s.date >= ?
              AND s.date <= ?
            ORDER BY s.date ASC
            """;

    public WeeklyInsightResult getWeeklyInsight(int userId, LocalDate start, LocalDate end) {
        Map<String, List<Double>> categoryScores = new LinkedHashMap<>();
        Map<LocalDate, List<Double>> dailyScores = new LinkedHashMap<>();

        Connection cnx = getCnx();
        try (PreparedStatement ps = cnx.prepareStatement(SQL_WEEK_ANSWERS)) {

            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(start));
            ps.setDate(3, java.sql.Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("day_date").toLocalDate();
                    String category = rs.getString("category");
                    String valeur = rs.getString("valeur");

                    double score = convertResponseToScore(valeur);

                    String catKey = normalizeCategory(category);
                    categoryScores.computeIfAbsent(catKey, k -> new ArrayList<>()).add(score);
                    dailyScores.computeIfAbsent(date, k -> new ArrayList<>()).add(score);
                }
            }

        } catch (SQLException e) {
            WeeklyInsightResult empty = new WeeklyInsightResult();
            empty.setTotalSubmittedDays(0);
            empty.setCategorySummaries(List.of());
            return empty;
        }

        WeeklyInsightResult result = new WeeklyInsightResult();
        result.setTotalSubmittedDays(dailyScores.size());

        // Build category summaries (avg always computed if there are answers)
        List<CategorySummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : categoryScores.entrySet()) {
            String cat = entry.getKey();
            List<Double> scores = entry.getValue();

            CategorySummary cs = new CategorySummary();
            cs.setCategory(cat);
            cs.setCountAnswers(scores.size());
            cs.setAvgNumericScore(scores.isEmpty() ? null : round1(avg(scores)));
            summaries.add(cs);
        }
        result.setCategorySummaries(summaries);

        // Best / worst day based on daily average score
        LocalDate bestDay = null;
        LocalDate worstDay = null;
        Double bestScore = null;
        Double worstScore = null;

        for (Map.Entry<LocalDate, List<Double>> entry : dailyScores.entrySet()) {
            List<Double> scores = entry.getValue();
            if (scores == null || scores.isEmpty()) continue;

            double dayAvg = avg(scores);

            if (bestScore == null || dayAvg > bestScore) {
                bestScore = dayAvg;
                bestDay = entry.getKey();
            }
            if (worstScore == null || dayAvg < worstScore) {
                worstScore = dayAvg;
                worstDay = entry.getKey();
            }
        }

        result.setBestDay(bestDay);
        result.setWorstDay(worstDay);
        result.setBestScore(bestScore == null ? null : round1(bestScore));
        result.setWorstScore(worstScore == null ? null : round1(worstScore));

        return result;
    }

    private static double avg(List<Double> list) {
        double sum = 0;
        for (double d : list) sum += d;
        return sum / list.size();
    }

    private static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String normalizeCategory(String s) {
        if (s == null) return "autre";
        String v = s.trim().toLowerCase(Locale.ROOT);
        return v.isBlank() ? "autre" : v;
    }

    
    public static double convertResponseToScore(String valeur) {
        if (valeur == null) return 0.0;

        String v = valeur.trim().toLowerCase(Locale.ROOT);
        v = Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        v = v.replace('–', '-').replace('—', '-');
        v = v.replaceAll("\\s+", " ");

        return switch (v) {
            case "positive", "positif",
                 "tres bien",
                 "oui, plusieurs",
                 "tres optimiste",
                 "oui, beaucoup",
                 "excellente", "excellent",
                 "oui, tres",
                 "plus de 8h",
                 "tres motive", "tres motivee",
                 "tres equilibree", "tres equilibre",
                 "intense",
                 "eleve", "elevee",
                 "plus de 30 min",
                 "energique",
                 "oui",
                 "non",
                 "tous",
                 "jamais",
                 "0 fois" -> 100.0;

            case "neutre",
                 "correct", "correcte",
                 "oui, un peu",
                 "un peu",
                 "oui, moyennement", "moyennement",
                 "6-8h",
                 "partiellement",
                 "presque", "presque (1.5-2l)",
                 "moyenne",
                 "la plupart",
                 "moderee", "modere",
                 "moyen",
                 "10-30 min",
                 "normal",
                 "legers",
                 "oui, moderees",
                 "1-2 fois" -> 66.0;

            case "negative", "negatif",
                 "pas terrible",
                 "plutot pessimiste",
                 "mauvaise", "mauvais",
                 "moins de 6h",
                 "demotive", "demotivee",
                 "peu equilibree", "peu equilibre",
                 "aucune", "aucun",
                 "faible",
                 "moins de 10 min",
                 "fatigue", "fatiguee",
                 "oui, tres fortes",
                 "3 fois ou plus",
                 "souvent", "toujours" -> 33.0;

            default -> 0.0;
        };
    }
}