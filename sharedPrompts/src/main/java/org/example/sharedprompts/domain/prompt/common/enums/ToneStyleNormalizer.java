package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * Normalizes tone and style enum values for engine use.
 * Canonical mappings live here so enums remain identity-only; normalization is policy.
 */
public final class ToneStyleNormalizer {

    private ToneStyleNormalizer() {}

    /**
     * Canonical tone for engine-level use. Maps semantically similar tones to a representative.
     * POSITIVE/ENTHUSIASTIC/INSPIRATIONAL → FRIENDLY; PROFESSIONAL → FORMAL; else identity.
     */
    public static ToneType toCanonicalTone(ToneType tone) {
        if (tone == null) return null;
        return switch (tone) {
            case POSITIVE, ENTHUSIASTIC, INSPIRATIONAL -> ToneType.FRIENDLY;
            case PROFESSIONAL -> ToneType.FORMAL;
            default -> tone;
        };
    }

    /**
     * Canonical style for engine-level use. Maps structurally similar styles to a representative.
     * STORYTELLING → NARRATIVE; ANALYTICAL/COMPARATIVE → DESCRIPTIVE; FORMATTED → CONCISE; else identity.
     */
    public static StyleType toCanonicalStyle(StyleType style) {
        if (style == null) return null;
        return switch (style) {
            case STORYTELLING -> StyleType.NARRATIVE;
            case ANALYTICAL, COMPARATIVE -> StyleType.DESCRIPTIVE;
            case FORMATTED -> StyleType.CONCISE;
            default -> style;
        };
    }
}
