package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.PolicyMetricsDimensions;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationEvaluationRecord;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.PolicyTraceInfo;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds metrics event and evaluation record from RecommendPromptResult.
 * Extracts dimensions from trace (policy version, experiment, variant, fallback) and result (category, intent, actions, roles).
 */
@Component
public class DefaultRecommendationMetricsAssembler implements RecommendationMetricsAssembler {

    @Override
    public RecommendationMetricsEvent toMetricsEvent(RecommendPromptResult result) {
        return toMetricsEvent(result, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Override
    public RecommendationMetricsEvent toMetricsEvent(
            RecommendPromptResult result,
            Optional<String> traceId,
            Optional<String> auditId,
            Optional<String> recommendationId
    ) {
        if (result == null) {
            return RecommendationMetricsEvent.builder().build();
        }

        String categoryKey = result.category() != null ? result.category().name() : "";
        String intentKey = result.recommendedIntent() != null ? result.recommendedIntent().name() : "";
        List<String> actionKeys = result.actionCandidates() != null
                ? result.actionCandidates().stream().map(a -> a.key()).collect(Collectors.toList())
                : List.of();
        String topActionKey = result.recommendedAction() != null ? result.recommendedAction().key() : (actionKeys.isEmpty() ? "" : actionKeys.get(0));
        List<String> roleKeys = result.roleCandidates() != null
                ? result.roleCandidates().stream().map(r -> r.key()).collect(Collectors.toList())
                : List.of();
        String topRoleKey = result.recommendedRole() != null ? result.recommendedRole().key() : (roleKeys.isEmpty() ? "" : roleKeys.get(0));

        boolean fallbackApplied = result.fallbackApplied() != null && !result.fallbackApplied().isEmpty();
        String policyVersion = "";
        String experimentId = "";
        String variantId = "";
        String sourceKind = "recommendation";

        Optional<RecommendationTrace> traceOpt = result.trace();
        if (traceOpt != null && traceOpt.isPresent()) {
            RecommendationTrace trace = traceOpt.get();
            fallbackApplied = fallbackApplied || trace.fallbackIntentUsed();
            if (trace.policyTraceInfo() != null && trace.policyTraceInfo().isPresent()) {
                PolicyTraceInfo info = trace.policyTraceInfo().get();
                policyVersion = info.policyVersionId() != null ? info.policyVersionId() : "";
                sourceKind = info.policySourceId() != null ? info.policySourceId() : "recommendation";
                experimentId = info.experimentId().orElse("");
                variantId = info.variantId().orElse("");
            }
            if (trace.actionOrderingTrace() != null && trace.actionOrderingTrace().isPresent()) {
                fallbackApplied = fallbackApplied || trace.actionOrderingTrace().get().fallbackOrderingApplied();
            }
        }

        PolicyMetricsDimensions dimensions = new PolicyMetricsDimensions(
                categoryKey, intentKey, topActionKey, topRoleKey,
                policyVersion, experimentId, variantId, sourceKind, fallbackApplied
        );

        return RecommendationMetricsEvent.builder()
                .categoryKey(categoryKey)
                .intentKey(intentKey)
                .recommendedActionKeys(actionKeys)
                .topActionKey(topActionKey)
                .recommendedRoleKeys(roleKeys)
                .topRoleKey(topRoleKey)
                .policyVersion(policyVersion)
                .experimentId(experimentId)
                .variantId(variantId)
                .fallbackApplied(fallbackApplied)
                .candidateActionCount(actionKeys.size())
                .candidateRoleCount(roleKeys.size())
                .dimensions(dimensions)
                .traceId(traceId.orElse(null))
                .auditId(auditId.orElse(null))
                .recommendationId(recommendationId.orElse(null))
                .build();
    }

    @Override
    public RecommendationEvaluationRecord toEvaluationRecord(RecommendPromptResult result) {
        return toEvaluationRecord(result, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Override
    public RecommendationEvaluationRecord toEvaluationRecord(
            RecommendPromptResult result,
            Optional<String> traceId,
            Optional<String> auditId,
            Optional<String> recommendationId
    ) {
        if (result == null) {
            return RecommendationEvaluationRecord.builder().build();
        }

        String categoryKey = result.category() != null ? result.category().name() : "";
        String intentKey = result.recommendedIntent() != null ? result.recommendedIntent().name() : "";
        List<String> actionKeys = result.actionCandidates() != null
                ? result.actionCandidates().stream().map(a -> a.key()).collect(Collectors.toList())
                : List.of();
        String topActionKey = result.recommendedAction() != null ? result.recommendedAction().key() : (actionKeys.isEmpty() ? "" : actionKeys.get(0));
        List<String> roleKeys = result.roleCandidates() != null
                ? result.roleCandidates().stream().map(r -> r.key()).collect(Collectors.toList())
                : List.of();
        String topRoleKey = result.recommendedRole() != null ? result.recommendedRole().key() : (roleKeys.isEmpty() ? "" : roleKeys.get(0));

        String policyVersion = "";
        String experimentId = "";
        String variantId = "";

        Optional<RecommendationTrace> traceOpt = result.trace();
        if (traceOpt != null && traceOpt.isPresent() && traceOpt.get().policyTraceInfo() != null && traceOpt.get().policyTraceInfo().isPresent()) {
            PolicyTraceInfo info = traceOpt.get().policyTraceInfo().get();
            policyVersion = info.policyVersionId() != null ? info.policyVersionId() : "";
            experimentId = info.experimentId().orElse("");
            variantId = info.variantId().orElse("");
        }

        return RecommendationEvaluationRecord.builder()
                .categoryKey(categoryKey)
                .intentKey(intentKey)
                .recommendedActionKeys(actionKeys)
                .topActionKey(topActionKey)
                .recommendedRoleKeys(roleKeys)
                .topRoleKey(topRoleKey)
                .policyVersion(policyVersion)
                .experimentId(experimentId)
                .variantId(variantId)
                .traceId(traceId.orElse(null))
                .auditId(auditId.orElse(null))
                .recommendationId(recommendationId.orElse(null))
                .build();
    }
}
