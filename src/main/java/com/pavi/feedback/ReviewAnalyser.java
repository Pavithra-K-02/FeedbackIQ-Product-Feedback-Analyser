
package com.pavi.feedback;

import java.util.*;
import java.util.regex.Pattern;

public class ReviewAnalyser {

    private static final Map<String, List<String>> THEME_KEYWORDS =
            new LinkedHashMap<>();


    private static final List<String> POSITIVE_PHRASES = List.of(
            "not bad", "works well", "no complaints", "no problems",
            "no issues", "great", "excellent", "amazing", "wonderful",
            "love", "loved", "easy to use", "user friendly",
            "beautiful", "helpful", "fast", "quick", "on time",
            "good quality", "fresh", "delicious", "smooth experience",
            "very happy", "satisfied", "perfect", "reliable",
            "recommend", "best", "good", "great service"
    );


    private static final List<String> NEGATIVE_PHRASES = List.of(
            "terrible", "awful", "very bad", "poor quality",
            "not working", "does not work", "doesnt work",
            "did not work", "not responding", "did not respond",
            "no response", "no refund", "money deducted",
            "hidden charges", "too expensive", "too high",
            "very late", "arrived cold", "bad quality",
            "waste of money", "never arrived", "failed twice",
            "keeps crashing", "crashes", "crashed", "crash",
            "confusing", "frustrating", "disappointed",
            "disappointing", "unhelpful", "broken", "defective",
            "delayed", "delay", "failed", "failure", "problem",
            "problems", "issue", "issues", "unacceptable",
            "slow", "expensive", "poor", "bad", "worst",
            "error", "errors", "stale", "spoiled", "rude",
            "late", "missing", "wrong", "refund failed"
    );

    private static final List<String> NEGATED_NEGATIVE_PHRASES =
            List.of(
                    "not bad", "not terrible", "not awful",
                    "not late", "not delayed", "not expensive",
                    "not confusing", "not broken", "not slow",
                    "no complaints", "no problems", "no issues",
                    "nothing wrong"
            );

    static {
        THEME_KEYWORDS.put("Payments & refunds", List.of(
                "payment", "paid", "refund", "money deducted",
                "transaction", "billing", "charged twice",
                "payment failed", "payment declined", "upi",
                "card payment", "money missing"
        ));

        THEME_KEYWORDS.put("Delivery & fulfilment", List.of(
                "delivery", "delivered", "late", "delay", "delayed",
                "tracking", "courier", "shipment", "arrived",
                "order never arrived", "missing order", "not arrived",
                "arrived cold", "fast delivery", "on time delivery"
        ));

        THEME_KEYWORDS.put("App usability & performance", List.of(
                "app", "interface", "crash", "crashes", "crashed",
                "bug", "slow", "loading", "login", "log in",
                "freezes", "frozen", "checkout", "confusing",
                "too many steps", "difficult to use", "hard to use",
                "navigation", "screen", "website"
        ));

        THEME_KEYWORDS.put("Customer support", List.of(
                "customer support", "support", "customer service",
                "service", "complaint", "respond", "response",
                "agent", "helpline", "no reply", "not respond",
                "did not respond", "unhelpful staff"
        ));

        THEME_KEYWORDS.put("Pricing & offers", List.of(
                "price", "prices", "expensive", "cost",
                "hidden charges", "extra charges", "fee", "fees",
                "discount", "offer", "coupon", "overpriced",
                "too expensive", "too high", "surge pricing"
        ));

        THEME_KEYWORDS.put("Food / product quality", List.of(
                "food", "taste", "tasteless", "quality", "cold food",
                "stale", "spoiled", "rotten", "damaged product",
                "defective", "broken item", "fresh", "freshness",
                "packaging", "portion"
        ));

        THEME_KEYWORDS.put("Search & discovery", List.of(
                "search", "find", "filter", "restaurant",
                "recommendation", "menu", "browse", "vegetarian"
        ));
    }

    public List<Review> analyse(List<String> input) {
        List<Review> result = new ArrayList<>();
        int id = 1;

        if (input == null) {
            return result;
        }

        for (String text : input) {
            if (text == null || text.isBlank()) {
                continue;
            }

            String cleanedText = text.trim();
            List<String> themes = detectThemes(cleanedText);

            List<Review.ThemeFeedback> feedback = new ArrayList<>();

            for (String theme : themes) {
                feedback.add(new Review.ThemeFeedback(
                        theme,
                        detectSentimentForTheme(cleanedText, theme)
                ));
            }

            String primaryTheme = themes.get(0);
            String overallSentiment = detectSentiment(cleanedText);

            result.add(new Review(
                    id++,
                    cleanedText,
                    primaryTheme,
                    overallSentiment,
                    feedback
            ));
        }

        return result;
    }

    public List<ThemeSummary> summarize(
            List<Review> reviews,
            Map<String, AIReviewAnalyser.Recommendation> aiRecs) {

        Map<String, List<ThemeObservation>> groups =
                new LinkedHashMap<>();

        if (reviews == null || reviews.isEmpty()) {
            return new ArrayList<>();
        }

        for (Review review : reviews) {
            if (review == null) {
                continue;
            }

            List<Review.ThemeFeedback> feedback =
                    review.getThemeFeedback();

            // Backward compatibility with reviews created using
            // the original four-argument Review constructor.
            if (feedback == null || feedback.isEmpty()) {
                addObservation(
                        groups,
                        review.getTheme(),
                        review.getSentiment(),
                        review.getText()
                );
                continue;
            }

            // Count each detected theme with its own sentiment.
            Set<String> countedThemes = new HashSet<>();

            for (Review.ThemeFeedback item : feedback) {
                if (item == null || item.theme() == null
                        || item.theme().isBlank()) {
                    continue;
                }

                String theme = item.theme();

                // Avoid counting the same theme twice in one review.
                if (!countedThemes.add(theme)) {
                    continue;
                }

                addObservation(
                        groups,
                        theme,
                        item.sentiment(),
                        review.getText()
                );
            }
        }

        if (groups.isEmpty()) {
            return new ArrayList<>();
        }

        int maxMentions = groups.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);

        Map<String, Double> scores = new HashMap<>();
        List<ThemeSummary> result = new ArrayList<>();

        for (Map.Entry<String, List<ThemeObservation>> entry
                : groups.entrySet()) {

            String theme = entry.getKey();
            List<ThemeObservation> group = entry.getValue();

            int count = group.size();

            List<ThemeObservation> negativeObservations =
                    group.stream()
                            .filter(o -> "Negative".equals(o.sentiment()))
                            .toList();

            int negative = negativeObservations.size();

            double frequencyScore =
                    100.0 * count / maxMentions;

            double negativeRate =
                    100.0 * negative / count;

            double severityScore = negativeObservations.stream()
                    .mapToInt(o -> estimateSeverity(o.text()))
                    .average()
                    .orElse(0.0);

            double score = 0.40 * frequencyScore
                    + 0.40 * negativeRate
                    + 0.20 * severityScore;

            score = Math.max(0, Math.min(100, score));
            scores.put(theme, score);

            String priority;

            if (negative == 0) {
                priority = "Low";
            } else if (score >= 70) {
                priority = "High";
            } else if (score >= 45) {
                priority = "Medium";
            } else {
                priority = "Low";
            }

            String[] fallback = fallback(theme);

            AIReviewAnalyser.Recommendation rec =
                    aiRecs == null ? null : aiRecs.get(theme);

            String action = rec != null
                    && rec.action() != null
                    && !rec.action().isBlank()
                    ? rec.action()
                    : fallback[0];

            String metric = rec != null
                    && rec.successMetric() != null
                    && !rec.successMetric().isBlank()
                    ? rec.successMetric()
                    : fallback[1];

            result.add(new ThemeSummary(
                    theme,
                    count,
                    negative,
                    priority,
                    score,
                    action,
                    metric
            ));
        }

        result.sort(
                Comparator.<ThemeSummary>comparingDouble(
                                t -> scores.getOrDefault(
                                        t.getTheme(), 0.0))
                        .reversed()
                        .thenComparing(ThemeSummary::getTheme)
        );

        return result;
    }

    private void addObservation(
            Map<String, List<ThemeObservation>> groups,
            String theme,
            String sentiment,
            String text) {

        if (theme == null || theme.isBlank()) {
            theme = "Other / general";
        }

        if (sentiment == null || sentiment.isBlank()) {
            sentiment = "Neutral";
        }

        groups.computeIfAbsent(theme, k -> new ArrayList<>())
                .add(new ThemeObservation(
                        sentiment,
                        text == null ? "" : text
                ));
    }

    private record ThemeObservation(String sentiment, String text) {
    }

    private List<String> detectThemes(String review) {
        String text = normalizeText(review);
        List<String> matches = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry
                : THEME_KEYWORDS.entrySet()) {

            for (String keyword : entry.getValue()) {
                if (containsPhrase(text, keyword)) {
                    matches.add(entry.getKey());
                    break;
                }
            }
        }

        if (matches.isEmpty()) {
            matches.add("Other / general");
        }

        return matches;
    }

    private String detectSentimentForTheme(
            String review, String theme) {

        // Split contrasting statements so each theme can receive
        // a sentiment based on the part discussing that theme.
        String[] parts = review.split(
                "(?i)\\bbut\\b|[.!?;]"
        );

        List<String> relevantParts = new ArrayList<>();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (partMatchesTheme(part, theme)) {
                relevantParts.add(part.trim());
            }
        }

        if (relevantParts.isEmpty()) {
            return detectSentiment(review);
        }

        return detectSentiment(String.join(". ", relevantParts));
    }

    private boolean partMatchesTheme(String part, String theme) {
        String normalized = normalizeText(part);

        if ("Other / general".equals(theme)) {
            return true;
        }

        List<String> keywords = THEME_KEYWORDS.get(theme);

        if (keywords == null) {
            return true;
        }

        for (String keyword : keywords) {
            if (containsPhrase(normalized, keyword)) {
                return true;
            }
        }

        return false;
    }

    private String detectSentiment(String review) {
        String text = normalizeText(review);

        if (text.isBlank()) {
            return "Neutral";
        }

        // Remove known negated negative phrases before counting
        // negative expressions, e.g. "not bad" and "not late".
        String negativeText = text;

        for (String phrase : NEGATED_NEGATIVE_PHRASES) {
            negativeText = negativeText.replace(
                    normalizeText(phrase), " "
            );
        }

        int positiveCount = countMatches(text, POSITIVE_PHRASES);
        int negativeCount = countMatches(
                negativeText, NEGATIVE_PHRASES
        );

        if (positiveCount > negativeCount) {
            return "Positive";
        }

        if (negativeCount > positiveCount) {
            return "Negative";
        }

        return "Neutral";
    }

    private int countMatches(String text, List<String> phrases) {
        int count = 0;

        for (String phrase : phrases) {
            if (containsPhrase(text, phrase)) {
                count++;
            }
        }

        return count;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private boolean containsPhrase(String text, String phrase) {
        String normalizedText = normalizeText(text);
        String normalizedPhrase = normalizeText(phrase);

        if (normalizedText.isBlank() || normalizedPhrase.isBlank()) {
            return false;
        }

        return Pattern.compile(
                "\\b" + Pattern.quote(normalizedPhrase) + "\\b"
        ).matcher(normalizedText).find();
    }

    private int estimateSeverity(String text) {
        String lower = normalizeText(text);

        if (containsAny(lower,
                "money deducted", "charged twice", "account hacked",
                "fraud", "unsafe", "security breach")) {
            return 100;
        }

        if (containsAny(lower,
                "crash", "crashes", "crashing",
                "failed repeatedly", "money missing",
                "never received", "completely broken")) {
            return 75;
        }

        if (containsAny(lower,
                "late", "delay", "slow", "confusing",
                "refund", "not responding", "wrong",
                "missing", "poor")) {
            return 50;
        }

        return 25;
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (containsPhrase(text, phrase)) {
                return true;
            }
        }

        return false;
    }

    private String[] fallback(String theme) {
        return switch (theme) {
            case "Delivery & fulfilment" -> new String[]{
                    "Improve ETA accuracy and send delay alerts with clearer tracking.",
                    "On-time delivery rate and ETA error"
            };
            case "Payments & refunds" -> new String[]{
                    "Improve payment retry states and add a refund-status tracker.",
                    "Payment success rate and refund resolution time"
            };
            case "App usability & performance" -> new String[]{
                    "Investigate crashes and slow screens; simplify the highest-friction flow.",
                    "Crash-free sessions and screen load time"
            };
            case "Customer support" -> new String[]{
                    "Add issue-specific self-service and show expected resolution times.",
                    "First-response and resolution time"
            };
            case "Pricing & offers" -> new String[]{
                    "Show fees and offer eligibility clearly before checkout.",
                    "Checkout abandonment and offer redemption rate"
            };
            case "Food / product quality" -> new String[]{
                    "Add a simple issue-reporting flow and share quality feedback with partners.",
                    "Quality complaints and repeat purchase rate"
            };
            case "Search & discovery" -> new String[]{
                    "Improve filters and search relevance; add quick access to favourites.",
                    "Search-to-order conversion"
            };
            default -> new String[]{
                    "Manually review examples and interview affected users before choosing a fix.",
                    "Frequency of this issue in the next review sample"
            };
        };
    }
}
