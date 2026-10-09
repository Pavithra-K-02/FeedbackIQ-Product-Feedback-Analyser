
package com.pavi.feedback;

import java.util.List;

public class Review {

    private final int id;
    private final String text;
    private final String theme;
    private final String sentiment;
    private final List<ThemeFeedback> themeFeedback;

    public record ThemeFeedback(String theme, String sentiment) {}

    // Existing constructor retained for compatibility with Gemini
    // and other code that creates a review with one theme.
    public Review(int id, String text, String theme, String sentiment) {
        this(id, text, theme, sentiment, List.of(
                new ThemeFeedback(theme, sentiment)
        ));
    }

    public Review(int id, String text, String theme, String sentiment,
                  List<ThemeFeedback> themeFeedback) {
        this.id = id;
        this.text = text;
        this.theme = theme;
        this.sentiment = sentiment;
        this.themeFeedback = themeFeedback == null
                ? List.of()
                : List.copyOf(themeFeedback);
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getTheme() {
        return theme;
    }

    public String getSentiment() {
        return sentiment;
    }

    public List<ThemeFeedback> getThemeFeedback() {
        return themeFeedback;
    }
}
