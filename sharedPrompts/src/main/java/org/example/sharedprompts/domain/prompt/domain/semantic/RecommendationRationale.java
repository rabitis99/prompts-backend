package org.example.sharedprompts.domain.prompt.domain.semantic;

/**
 * Structured reason for a recommendation (intent, role, action, or fallback).
 * Used to produce domain-meaningful hints for UI and explainability.
 */
public record RecommendationRationale(
        RecommendationRationaleKind kind,
        String summary,
        String detail
) {
    public static RecommendationRationale intentPreferred(String summary, String detail) {
        return new RecommendationRationale(RecommendationRationaleKind.INTENT_PREFERRED, summary, detail);
    }

    public static RecommendationRationale roleRecommended(String summary, String detail) {
        return new RecommendationRationale(RecommendationRationaleKind.ROLE_RECOMMENDED, summary, detail);
    }

    public static RecommendationRationale actionFits(String summary, String detail) {
        return new RecommendationRationale(RecommendationRationaleKind.ACTION_FITS, summary, detail);
    }

    public static RecommendationRationale fallbackUsed(String summary, String detail) {
        return new RecommendationRationale(RecommendationRationaleKind.FALLBACK_USED, summary, detail);
    }

    public static RecommendationRationale choiceSuboptimal(String summary, String detail) {
        return new RecommendationRationale(RecommendationRationaleKind.CHOICE_SUBOPTIMAL, summary, detail);
    }

    public static RecommendationRationale toneOrStyleDiscouraged(String summary, String detail) {
        return new RecommendationRationale(RecommendationRationaleKind.TONE_STYLE_DISCOURAGED, summary, detail);
    }

    public enum RecommendationRationaleKind {
        INTENT_PREFERRED,
        ROLE_RECOMMENDED,
        ACTION_FITS,
        FALLBACK_USED,
        CHOICE_SUBOPTIMAL,
        TONE_STYLE_DISCOURAGED
    }
}
