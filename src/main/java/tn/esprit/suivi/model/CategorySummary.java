package tn.esprit.suivi.model;

public class CategorySummary {
    private String category;
    private int countAnswers;
    private Double avgNumericScore;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCountAnswers() {
        return countAnswers;
    }

    public void setCountAnswers(int countAnswers) {
        this.countAnswers = countAnswers;
    }

    public Double getAvgNumericScore() {
        return avgNumericScore;
    }

    public void setAvgNumericScore(Double avgNumericScore) {
        this.avgNumericScore = avgNumericScore;
    }

    @Override
    public String toString() {
        return "CategorySummary{category='" + category + "', countAnswers=" + countAnswers + ", avgNumericScore=" + avgNumericScore + "}";
    }
}