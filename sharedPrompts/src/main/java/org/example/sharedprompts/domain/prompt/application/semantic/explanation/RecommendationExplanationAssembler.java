package org.example.sharedprompts.domain.prompt.application.semantic.explanation;

import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;

import java.util.List;
import java.util.Optional;

/**
 * Application port: builds user-facing hints and explanation from internal trace.
 * Implementation lives in adapter; response assembler uses this interface only.
 */
public interface RecommendationExplanationAssembler {

    /**
     * Build summary hints from trace for recommendation metadata.
     * Returns empty when trace is empty.
     */
    List<String> toHints(Optional<RecommendationTrace> trace);

    /**
     * Build full explanation view from trace for response assembly.
     * Adapter maps this to DTO; no adapter type in application layer.
     */
    RecommendationExplanationView toExplanation(Optional<RecommendationTrace> trace);
}
