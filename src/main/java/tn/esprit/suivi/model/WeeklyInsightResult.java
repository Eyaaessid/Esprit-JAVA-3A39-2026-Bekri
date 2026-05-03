package tn.esprit.suivi.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeeklyInsightResult {

    private int totalSubmittedDays;
    private List<CategorySummary> categorySummaries = new ArrayList<>();

    private LocalDate bestDay;
    private LocalDate worstDay;
    private Double bestScore;
    private Double worstScore;

    // ── NEW: per-day humeur averages for the line chart ──────────────────
    private Map<LocalDate, Double> dailyHumeurScores = new HashMap<>();

    public int getTotalSubmittedDays() {
        return totalSubmittedDays;
    }

    public void setTotalSubmittedDays(int totalSubmittedDays) {
        this.totalSubmittedDays = totalSubmittedDays;
    }

    public List<CategorySummary> getCategorySummaries() {
        return categorySummaries;
    }

    public void setCategorySummaries(List<CategorySummary> categorySummaries) {
        this.categorySummaries = categorySummaries;
    }

    public LocalDate getBestDay() {
        return bestDay;
    }

    public void setBestDay(LocalDate bestDay) {
        this.bestDay = bestDay;
    }

    public LocalDate getWorstDay() {
        return worstDay;
    }

    public void setWorstDay(LocalDate worstDay) {
        this.worstDay = worstDay;
    }

    public Double getBestScore() {
        return bestScore;
    }

    public void setBestScore(Double bestScore) {
        this.bestScore = bestScore;
    }

    public Double getWorstScore() {
        return worstScore;
    }

    public void setWorstScore(Double worstScore) {
        this.worstScore = worstScore;
    }

    // ── NEW getter/setter ────────────────────────────────────────────────
    public Map<LocalDate, Double> getDailyHumeurScores() {
        return dailyHumeurScores;
    }

    public void setDailyHumeurScores(Map<LocalDate, Double> dailyHumeurScores) {
        this.dailyHumeurScores = dailyHumeurScores;
    }
}