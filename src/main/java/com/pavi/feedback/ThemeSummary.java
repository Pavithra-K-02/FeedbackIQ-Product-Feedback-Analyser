
package com.pavi.feedback;

public class ThemeSummary {
    private final String theme;
    private final int mentions;
    private final int negativeReviews;
    private final String priority;
    private final double priorityScore;
    private final String recommendation;
    private final String successMetric;

    public ThemeSummary(
            String theme,
            int mentions,
            int negativeReviews,
            String priority,
            double priorityScore,
            String recommendation,
            String successMetric) {

        this.theme = theme;
        this.mentions = mentions;
        this.negativeReviews = negativeReviews;
        this.priority = priority;
        this.priorityScore = priorityScore;
        this.recommendation = recommendation;
        this.successMetric = successMetric;
    }

    public String getTheme() {
        return theme;
    }

    public int getMentions() {
        return mentions;
    }

    public int getNegativeReviews() {
        return negativeReviews;
    }

    public String getPriority() {
        return priority;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getSuccessMetric() {
        return successMetric;
    }
}
