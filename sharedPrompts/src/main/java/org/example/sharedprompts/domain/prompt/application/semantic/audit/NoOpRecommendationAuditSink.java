package org.example.sharedprompts.domain.prompt.application.semantic.audit;

import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationAuditRecord;

/**
 * No-op audit sink; replace with file/DB/event sink when needed.
 */
public class NoOpRecommendationAuditSink implements RecommendationAuditSink {

    @Override
    public void accept(RecommendationAuditRecord record) {
        // no-op
    }
}
