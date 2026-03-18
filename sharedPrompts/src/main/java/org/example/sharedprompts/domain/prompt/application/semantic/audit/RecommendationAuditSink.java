package org.example.sharedprompts.domain.prompt.application.semantic.audit;

import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationAuditRecord;

/**
 * Sink for recommendation audit records (file, DB, event bus).
 * Implementations must not throw; handle errors internally.
 */
public interface RecommendationAuditSink {

    void accept(RecommendationAuditRecord record);
}
