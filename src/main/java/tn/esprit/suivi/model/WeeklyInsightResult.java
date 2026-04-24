package tn.esprit.suivi.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeeklyInsightResult {

    private int totalSubmittedDays;
    private List<CategorySummary> categorySummaries = new ArrayList<>();

    private LocalDate bestDay;
    private LocalDate worstDay;
    private Double bestScore;
    private Double worstScore;

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
}