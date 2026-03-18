package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationEvaluationRecord;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;

import java.util.Optional;

/**
 * Assembles observability artifacts from recommendation result and trace.
 * Extracts only the dimensions needed for metrics/evaluation; does not embed full trace.
 */
public interface RecommendationMetricsAssembler {

    /**
     * Build online metrics event from result (and its trace).
     * Uses trace for policyVersion, experimentId, variantId, fallback; result for category, intent, actions, roles.
     */
    RecommendationMetricsEvent toMetricsEvent(RecommendPromptResult result);

    /**
     * Build online metrics event with optional linkage ids.
     */
    RecommendationMetricsEvent toMetricsEvent(
            RecommendPromptResult result,
            Optional<String> traceId,
            Optional<String> auditId,
            Optional<String> recommendationId
    );

    /**
     * Build offline evaluation record from result (no expected labels).
     * Same axes as metrics but for evaluation pipeline; separate from online event.
     */
    RecommendationEvaluationRecord toEvaluationRecord(RecommendPromptResult result);

    /**
     * Build offline evaluation record with optional linkage ids.
     */
    RecommendationEvaluationRecord toEvaluationRecord(
            RecommendPromptResult result,
            Optional<String> traceId,
            Optional<String> auditId,
            Optional<String> recommendationId
    );
}
