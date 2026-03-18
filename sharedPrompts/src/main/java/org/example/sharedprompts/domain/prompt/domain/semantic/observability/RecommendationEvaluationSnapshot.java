package org.example.sharedprompts.domain.prompt.domain.semantic.observability;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Snapshot boundary for offline evaluation pipeline.
 * Wraps one or more evaluation records with optional batch metadata.
 * Expected labels may be empty; structure is ready for human review or labeled dataset attachment.
 */
public record RecommendationEvaluationSnapshot(
        Instant snapshotAt,
        String snapshotId,
        List<RecommendationEvaluationRecord> records,
        Optional<String> runId
) {
    public RecommendationEvaluationSnapshot {
        records = records != null ? List.copyOf(records) : List.of();
        runId = runId != null ? runId : Optional.empty();
    }

    public static RecommendationEvaluationSnapshot of(
            String snapshotId,
            List<RecommendationEvaluationRecord> records
    ) {
        return new RecommendationEvaluationSnapshot(
                Instant.now(),
                snapshotId != null ? snapshotId : "",
                records,
                Optional.empty()
        );
    }

    public static RecommendationEvaluationSnapshot single(
            String snapshotId,
            RecommendationEvaluationRecord record
    ) {
        return of(snapshotId, record != null ? List.of(record) : List.of());
    }
}
