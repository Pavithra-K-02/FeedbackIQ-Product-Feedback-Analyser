
package com.pavi.feedback;

import org.springframework.web.bind.annotation.*;
import com.google.genai.errors.ServerException;
import com.google.genai.errors.ClientException;
import java.util.*;

@RestController
public class FeedbackController {

    private final AIReviewAnalyser aiAnalyser = new AIReviewAnalyser();
    private final ReviewAnalyser reviewAnalyser = new ReviewAnalyser();

    public record AnalyseRequest(List<String> reviews) {}

    @GetMapping("/api/status")
    public Map<String, String> status() {
        return Map.of(
                "status", "running",
                "message", "Product Feedback Analyser API is ready"
        );
    }

    @PostMapping("/api/analyse")
    public Map<String, Object> analyse(@RequestBody AnalyseRequest request)
            throws Exception {

        if (request == null || request.reviews() == null
                || request.reviews().isEmpty()) {
            throw new IllegalArgumentException(
                    "Please provide at least one review.");
        }

        List<String> input = request.reviews().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(200)
                .toList();

        if (input.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please provide valid review text.");
        }

        AIReviewAnalyser.AnalysisResult result;
        String analysisMode;

        try {
            result = aiAnalyser.analyse(input);
            analysisMode = "Gemini AI API";
        } catch (ServerException e) {
            // Gemini service errors, including temporary 503 responses.
            result = fallbackAnalysis(input);
            analysisMode = "Rule-based fallback (Gemini unavailable)";
        } catch (ClientException e) {
            // Do not hide configuration, authentication, or invalid-request errors.
            throw e;
        }

        List<ThemeSummary> summaries = reviewAnalyser.summarize(
                result.reviews(), result.recommendations());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analysisMode", analysisMode);
        response.put("overallSummary", result.overallSummary());
        response.put("reviews", result.reviews());
        response.put("themes", summaries);

        response.put("recommendations", summaries.stream().map(t -> {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("theme", t.getTheme());
            rec.put("priority", t.getPriority());
            rec.put("priorityScore", t.getPriorityScore());
            rec.put("action", t.getRecommendation());
            rec.put("successMetric", t.getSuccessMetric());
            return rec;
        }).toList());

        return response;
    }

    private AIReviewAnalyser.AnalysisResult fallbackAnalysis(
            List<String> input) {

        List<Review> reviews = reviewAnalyser.analyse(input);

        return new AIReviewAnalyser.AnalysisResult(
                reviews,
                "Rule-based analysis was used because Gemini AI was temporarily unavailable.",
                Collections.emptyMap()
        );
    }
}
