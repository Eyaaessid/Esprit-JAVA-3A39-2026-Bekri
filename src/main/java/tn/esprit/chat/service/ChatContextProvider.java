package tn.esprit.chat.service;

import tn.esprit.suivi.model.CategorySummary;
import tn.esprit.suivi.model.WeeklyInsightResult;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ChatContextProvider {

    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);

    /**
     * Build a concise context string that you can inject into your chatbot prompt.
     * Replaces the old weeklyInsightResult.getRows() usage.
     */
    public String buildWeeklyInsightContext(WeeklyInsightResult weeklyInsightResult) {
        if (weeklyInsightResult == null) {
            return "Insights hebdomadaires: aucune donnée.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Insights hebdomadaires (7 jours)\n");
        sb.append("- Jours complétés: ").append(weeklyInsightResult.getTotalSubmittedDays()).append("/7\n");

        if (weeklyInsightResult.getBestDay() != null) {
            sb.append("- Meilleur jour: ").append(weeklyInsightResult.getBestDay().format(DAY_FMT));
            if (weeklyInsightResult.getBestScore() != null) {
                sb.append(" (").append(weeklyInsightResult.getBestScore()).append("%)");
            }
            sb.append("\n");
        }

        if (weeklyInsightResult.getWorstDay() != null) {
            sb.append("- Jour le plus difficile: ").append(weeklyInsightResult.getWorstDay().format(DAY_FMT));
            if (weeklyInsightResult.getWorstScore() != null) {
                sb.append(" (").append(weeklyInsightResult.getWorstScore()).append("%)");
            }
            sb.append("\n");
        }

        List<CategorySummary> summaries = weeklyInsightResult.getCategorySummaries();
        if (summaries == null || summaries.isEmpty()) {
            sb.append("- Catégories: aucune.\n");
            return sb.toString();
        }

        sb.append("- Moyennes par catégorie:\n");
        for (CategorySummary s : summaries) {
            String category = (s.getCategory() == null || s.getCategory().isBlank()) ? "autre" : s.getCategory();
            String avg = (s.getAvgNumericScore() == null) ? "—" : (s.getAvgNumericScore() + "%");
            sb.append("  • ").append(category).append(": ").append(avg)
                    .append(" (réponses=").append(s.getCountAnswers()).append(")\n");
        }

        return sb.toString();
    }
}