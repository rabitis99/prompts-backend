package org.example.sharedprompts.domain.prompt.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Verify 단계 결과 — 루브릭 항목별 pass/fail과 실패 이유를 담는다.
 * Repair 단계에서는 실패 항목만 지목하여 수정 요청한다.
 */
public final class VerifyResult {

    private final boolean passed;
    private final Map<QualityRubric.RubricItem, Boolean> itemResults;
    private final List<String> failureReasons;

    private VerifyResult(
            boolean passed,
            Map<QualityRubric.RubricItem, Boolean> itemResults,
            List<String> failureReasons
    ) {
        this.passed = passed;
        this.itemResults = Map.copyOf(itemResults);
        this.failureReasons = List.copyOf(failureReasons);
    }

    public static VerifyResult pass(Map<QualityRubric.RubricItem, Boolean> itemResults) {
        return new VerifyResult(true, itemResults, List.of());
    }

    public static VerifyResult fail(
            Map<QualityRubric.RubricItem, Boolean> itemResults,
            List<String> failureReasons
    ) {
        return new VerifyResult(false, itemResults, failureReasons);
    }

    public boolean isPassed() { return passed; }
    public Map<QualityRubric.RubricItem, Boolean> getItemResults() { return itemResults; }
    public List<String> getFailureReasons() { return failureReasons; }

    /** Repair 요청을 위한 실패 루브릭 항목 목록 */
    public List<QualityRubric.RubricItem> getFailedItems() {
        return itemResults.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public String toString() {
        return "VerifyResult{passed=" + passed + ", failedItems=" + getFailedItems() + "}";
    }
}
