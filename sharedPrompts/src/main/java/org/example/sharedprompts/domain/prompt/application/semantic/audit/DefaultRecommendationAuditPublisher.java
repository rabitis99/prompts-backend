package org.example.sharedprompts.domain.prompt.application.semantic.audit;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationAuditRecord;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds RecommendationAuditRecord from result and forwards to all configured sinks.
 */
public class DefaultRecommendationAuditPublisher implements RecommendationAuditPublisher {

    private final List<RecommendationAuditSink> sinks;

    public DefaultRecommendationAuditPublisher(List<RecommendationAuditSink> sinks) {
        this.sinks = sinks != null ? List.copyOf(sinks) : List.of();
    }

    @Override
    public void publish(RecommendPromptResult result) {
        if (result == null) {
            return;
        }
        RecommendationAuditRecord record = RecommendationAuditRecord.of(
                result.category(),
                result.recommendedIntent(),
                result.recommendedAction() != null ? result.recommendedAction().key() : null,
                result.recommendedRole() != null ? result.recommendedRole().key() : null,
                result.actionCandidates().stream().map(a -> a.key()).collect(Collectors.toList()),
                result.roleCandidates().stream().map(r -> r.key()).collect(Collectors.toList()),
                result.trace().orElse(null)
        );
        for (RecommendationAuditSink sink : sinks) {
            try {
                sink.accept(record);
            } catch (Exception ignored) {
                // Sink must not throw; handle internally
            }
        }
    }
}
