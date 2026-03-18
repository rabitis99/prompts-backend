package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Audit boundary: input + outcome + trace summary for later persistence (file/DB/event).
 * Not persisted in this phase; structure is ready for audit sink.
 */
public record RecommendationAuditRecord(
        Instant timestamp,
        String categoryKey,
        String intentKey,
        String selectedActionKey,
        String selectedRoleKey,
        List<String> recommendedActionKeys,
        List<String> recommendedRoleKeys,
        RecommendationTraceSnapshot traceSummary
) {
    public RecommendationAuditRecord {
        recommendedActionKeys = recommendedActionKeys != null ? List.copyOf(recommendedActionKeys) : List.of();
        recommendedRoleKeys = recommendedRoleKeys != null ? List.copyOf(recommendedRoleKeys) : List.of();
    }

    public static RecommendationAuditRecord of(
            PromptCategory category,
            ActionIntent intent,
            String selectedActionKey,
            String selectedRoleKey,
            List<String> recommendedActionKeys,
            List<String> recommendedRoleKeys,
            RecommendationTrace trace
    ) {
        RecommendationTraceSnapshot snapshot = trace != null ? RecommendationTraceSnapshot.from(trace) : null;
        return new RecommendationAuditRecord(
                Instant.now(),
                category != null ? category.name() : null,
                intent != null ? intent.name() : null,
                selectedActionKey,
                selectedRoleKey,
                recommendedActionKeys != null ? recommendedActionKeys : Collections.emptyList(),
                recommendedRoleKeys != null ? recommendedRoleKeys : Collections.emptyList(),
                snapshot
        );
    }
}
