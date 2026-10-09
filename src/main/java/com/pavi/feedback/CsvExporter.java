
package com.pavi.feedback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class CsvExporter {

    public static void exportReviews(List<Review> reviews, Path path)
            throws IOException {

        StringBuilder csv =
                new StringBuilder("Review ID,Review,Theme,Sentiment\n");

        for (Review r : reviews) {
            csv.append(r.getId())
                    .append(',')
                    .append(escape(r.getText()))
                    .append(',')
                    .append(escape(r.getTheme()))
                    .append(',')
                    .append(escape(r.getSentiment()))
                    .append('\n');
        }

        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }

    public static void exportThemes(List<ThemeSummary> themes, Path path)
            throws IOException {

        StringBuilder csv = new StringBuilder(
                "Theme,Mentions,Negative Reviews,Priority,Recommendation,Success Metric\n");

        for (ThemeSummary t : themes) {
            csv.append(escape(t.getTheme()))
                    .append(',')
                    .append(t.getMentions())
                    .append(',')
                    .append(t.getNegativeReviews())
                    .append(',')
                    .append(escape(t.getPriority()))
                    .append(',')
                    .append(escape(t.getRecommendation()))
                    .append(',')
                    .append(escape(t.getSuccessMetric()))
                    .append('\n');
        }

        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }

    private static String escape(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
