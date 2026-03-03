package org.example.sharedprompts.domain.prompt.domain.verification.base;

import org.example.sharedprompts.domain.prompt.domain.model.spec.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 검증 전략 공통 헬퍼 — 도메인 검증 로직(패턴 매칭, 키워드 보존 등)을 상속 계층으로 공유한다.
 */
public abstract class BaseVerification implements org.example.sharedprompts.domain.prompt.domain.verification.VerificationStrategy {

    private static final Pattern CONTRADICTION_PATTERN =
            Pattern.compile("(?i)(\\b\\w+\\b)\\s+.{0,50}\\b(아니다|아닙니다|not|isn't|is not)\\b");

    private static final Pattern QUANTITATIVE_CLAIM_PATTERN =
            Pattern.compile("\\d+%|\\d+배");

    protected Map<QualityRubric.RubricItem, Boolean> newResults() {
        return new EnumMap<>(QualityRubric.RubricItem.class);
    }

    protected void checkCoverage(String draft,
                                  Map<QualityRubric.RubricItem, Boolean> r, List<String> f) {
        boolean ok = draft != null && !draft.isBlank() && draft.length() >= 50;
        r.put(QualityRubric.RubricItem.COVERAGE, ok);
        if (!ok) f.add("프롬프트 내용이 충분하지 않습니다 (최소 50자 미만).");
    }

    protected void checkInputPreservation(String draft, String clarifiedInput,
                                           Map<QualityRubric.RubricItem, Boolean> r, List<String> f) {
        boolean ok = clarifiedInput == null || clarifiedInput.isBlank()
                || draftPreservesKeywords(draft, clarifiedInput);
        r.put(QualityRubric.RubricItem.INPUT_PRESERVATION, ok);
        if (!ok) f.add("입력 조건의 핵심 키워드가 생성 결과에 반영되지 않았습니다.");
    }

    protected void checkNoContradiction(String draft,
                                         Map<QualityRubric.RubricItem, Boolean> r, List<String> f) {
        boolean ok = hasObviousContradiction(draft);
        r.put(QualityRubric.RubricItem.NO_CONTRADICTION, ok);
        if (!ok) f.add("논리 모순(A이다 + A가 아니다)이 감지되었습니다.");
    }

    protected void checkUncertaintyHandling(String draft,
                                             Map<QualityRubric.RubricItem, Boolean> r, List<String> f) {
        boolean ok = !hasUnsubstantiatedClaim(draft);
        r.put(QualityRubric.RubricItem.UNCERTAINTY_HANDLING, ok);
        if (!ok) f.add("검증 불가능한 수치·인용 주장에 근거 또는 불확실성 표시가 없습니다.");
    }

    protected void checkNoProhibitedContent(String draft, ContentSandbox sandbox,
                                             Map<QualityRubric.RubricItem, Boolean> r, List<String> f) {
        boolean ok = sandbox == null || !sandbox.containsViolation(draft);
        r.put(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, ok);
        if (!ok) f.add("금지어 또는 내용 정책 위반 내용이 포함되어 있습니다.");
    }

    protected void checkFormatCompliance(String draft, OutputContract contract,
                                          Map<QualityRubric.RubricItem, Boolean> r, List<String> f) {
        boolean ok = isFormatCompliant(draft, contract);
        r.put(QualityRubric.RubricItem.FORMAT_COMPLIANCE, ok);
        if (!ok) f.add("출력 형식이 지정된 JSON Schema를 준수하지 않습니다: " + contract.getFormat());
    }

    protected boolean isFormatCompliant(String draft, OutputContract contract) {
        if (contract == null || !contract.hasJsonSchema()) return true;
        if (draft == null || draft.isBlank()) return false;
        String trimmed = draft.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    protected boolean hasObviousContradiction(String draft) {
        if (draft == null) return true;
        return !CONTRADICTION_PATTERN.matcher(draft).find();
    }

    protected boolean hasUnsubstantiatedClaim(String draft) {
        if (draft == null) return false;
        boolean hasQuantitativeClaim = QUANTITATIVE_CLAIM_PATTERN.matcher(draft).find();
        boolean hasHedging = draft.contains("추정") || draft.contains("참고")
                || draft.contains("approximately") || draft.contains("likely")
                || draft.contains("약 ") || draft.contains("출처:");
        return hasQuantitativeClaim && !hasHedging;
    }

    protected boolean draftPreservesKeywords(String draft, String input) {
        if (input == null) return true;
        if (draft == null || draft.isBlank()) return false;
        List<String> keywords = Arrays.stream(input.split("\\s+"))
                .filter(w -> w.length() >= 2)
                .limit(5)
                .toList();
        if (keywords.isEmpty()) return true;
        long matched = keywords.stream().filter(draft::contains).count();
        return (double) matched / keywords.size() >= 0.4;
    }

    protected VerifyResult buildResult(Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        return failures.isEmpty() ? VerifyResult.pass(results) : VerifyResult.fail(results, failures);
    }
}
