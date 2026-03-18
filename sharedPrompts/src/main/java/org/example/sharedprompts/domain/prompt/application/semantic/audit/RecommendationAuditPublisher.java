package org.example.sharedprompts.domain.prompt.application.semantic.audit;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationAuditRecord;

/**
 * Publishes recommendation audit records to configured sink(s).
 * Separate from observability/metrics; audit is for persistence and compliance.
 */
public interface RecommendationAuditPublisher {

    /**
     * Build audit record from result and publish to sink(s).
     * Must not throw; sink failures are handled by the sink.
     */
    void publish(RecommendPromptResult result);
}
