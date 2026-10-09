package com.pavi.feedback;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Main {
    private static final Scanner IN = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" PRODUCT FEEDBACK ANALYSER — JAVA + GEMINI AI");
        System.out.println("==============================================");
        System.out.print("Enter app/product name [Zomato]: ");
        String app = IN.nextLine().trim(); if (app.isBlank()) app = "Zomato";
        List<String> reviews = sampleReviews();
        System.out.println("\n1. Use 20 built-in sample reviews");
        System.out.println("2. Paste your own reviews (one per line; blank line finishes)");
        System.out.print("Choice [1]: ");
        if ("2".equals(IN.nextLine().trim())) {
            reviews = new ArrayList<>();
            System.out.println("Paste reviews, then press Enter on a blank line:");
            while (true) { String line = IN.nextLine(); if (line.isBlank()) break; reviews.add(line.trim()); }
        }
        if (reviews.size() < 3) { System.out.println("Enter at least 3 reviews."); return; }

        List<Review> analysed;
        Map<String, AIReviewAnalyser.Recommendation> recs = new HashMap<>();
        String mode, aiSummary = "";
        boolean useAI = !"false".equalsIgnoreCase(System.getenv().getOrDefault("USE_GEMINI_AI", "true"));
        if (useAI) {
            try {
                System.out.println("\nCalling Gemini AI API...");
                AIReviewAnalyser.AnalysisResult result = new AIReviewAnalyser().analyse(reviews);
                analysed = result.reviews(); recs = result.recommendations(); aiSummary = result.overallSummary(); mode = "Gemini AI API";
            } catch (Exception e) {
                System.out.println("Gemini request failed: " + e.getMessage());
                System.out.println("Using local keyword fallback instead.");
                analysed = new ReviewAnalyser().analyse(reviews); mode = "Local keyword fallback";
            }
        } else {
            analysed = new ReviewAnalyser().analyse(reviews); mode = "Local keyword analysis";
        }

        List<ThemeSummary> themes = new ReviewAnalyser().summarize(analysed, recs);
        print(app, analysed, themes, mode, aiSummary);
        export(app, analysed, themes, mode, aiSummary);
    }

    private static void print(String app, List<Review> reviews, List<ThemeSummary> themes, String mode, String summary) {
        long pos = reviews.stream().filter(r -> r.getSentiment().equals("Positive")).count();
        long neg = reviews.stream().filter(r -> r.getSentiment().equals("Negative")).count();
        System.out.println("\n--- SUMMARY: " + app + " ---");
        System.out.println("Analysis mode: " + mode);
        System.out.printf("Reviews: %d | Positive: %d | Negative: %d | Neutral: %d%n", reviews.size(), pos, neg, reviews.size()-pos-neg);
        if (!summary.isBlank()) System.out.println("\nAI summary: " + summary);
        System.out.println("\n--- REVIEW-LEVEL ANALYSIS ---");
        for (Review r : reviews) System.out.printf("%02d. [%s] [%s] %s%n", r.getId(), r.getSentiment(), r.getTheme(), r.getText());
        System.out.println("\n--- PRIORITIZED PRODUCT ROADMAP ---");
        for (ThemeSummary t : themes) {
            System.out.printf("%n%s | Mentions: %d | Negative: %d | Priority: %s%n", t.getTheme(), t.getMentions(), t.getNegativeReviews(), t.getPriority());
            System.out.println("  Recommendation: " + t.getRecommendation());
            System.out.println("  Success metric: " + t.getSuccessMetric());
        }
    }

    private static void export(String app, List<Review> reviews, List<ThemeSummary> themes, String mode, String summary) {
        try {
            Path out = Paths.get("output"); Files.createDirectories(out);
            CsvExporter.exportReviews(reviews, out.resolve("review_analysis.csv"));
            CsvExporter.exportThemes(themes, out.resolve("prioritized_roadmap.csv"));
            long pos = reviews.stream().filter(r -> r.getSentiment().equals("Positive")).count();
            long neg = reviews.stream().filter(r -> r.getSentiment().equals("Negative")).count();
            StringBuilder md = new StringBuilder("# Product Feedback Analysis — " + app + "\n\n");
            md.append("**Sample size:** ").append(reviews.size()).append(" reviews. **Analysis mode:** ").append(mode).append(".\n\n");
            if (!summary.isBlank()) md.append("## AI summary\n\n").append(summary).append("\n\n");
            md.append("## Sentiment snapshot\n\n- Positive: ").append(pos).append("\n- Negative: ").append(neg)
                    .append("\n- Neutral: ").append(reviews.size()-pos-neg).append("\n\n## Top themes\n\n");
            for (int i=0; i<Math.min(3, themes.size()); i++) {
                ThemeSummary t = themes.get(i);
                md.append("### ").append(t.getTheme()).append(" — ").append(t.getMentions()).append(" mentions\n")
                        .append("- Priority: ").append(t.getPriority()).append("\n- Recommendation: ").append(t.getRecommendation())
                        .append("\n- Success metric: ").append(t.getSuccessMetric()).append("\n\n");
            }
            md.append("## Limitations\n\nAI outputs can be wrong; sample bias and mention frequency do not establish root cause. Validate recommendations with real users.\n");
            Files.writeString(out.resolve("product_brief.md"), md.toString());
            System.out.println("\nReports saved to: " + out.toAbsolutePath());
            System.out.println("- review_analysis.csv\n- prioritized_roadmap.csv\n- product_brief.md");
        } catch (IOException e) { System.out.println("Report export failed: " + e.getMessage()); }
    }

    private static List<String> sampleReviews() {
        return List.of(
            "My order arrived 40 minutes late and the delivery tracking did not update.",
            "Payment failed but the amount was deducted. I am still waiting for a refund.",
            "The app is slow and keeps crashing when I open the restaurant menu.",
            "Customer support did not respond to my complaint for two days.",
            "Food was fresh and delivery was quick. Great experience.",
            "The delivery was late again and the food was cold.",
            "There are too many extra fees at checkout and the offer coupon did not work.",
            "Easy to use app and a smooth payment experience.",
            "I cannot find the refund status anywhere in the app.",
            "Search filters are confusing and I cannot find vegetarian options quickly.",
            "The restaurant sent the wrong order and support was not helpful.",
            "App loading takes too long, especially when I try to pay.",
            "Good discounts and the food quality was excellent.",
            "My card was charged twice and getting help is difficult.",
            "Order tracking showed the driver nearby but delivery took another 30 minutes.",
            "The menu photos look nice but the portion size was very small.",
            "Login keeps failing and I have to enter the OTP multiple times.",
            "Refund came quickly this time and the support agent was helpful.",
            "The prices are expensive compared with ordering directly.",
            "Great app, easy to reorder my favourite meal."
        );
    }
}
