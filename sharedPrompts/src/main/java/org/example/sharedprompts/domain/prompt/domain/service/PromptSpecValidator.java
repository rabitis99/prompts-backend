package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.model.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PromptSpec 기준으로 생성된 프롬프트 초안을 검증하는 도메인 서비스.
 *
 * <p>Objective별로 Verify 강도가 다르게 적용된다:
 * <ul>
 *   <li>FACTUAL: Chain-of-Verification 강화 + CITE_OR_UNCERTAIN</li>
 *   <li>REASONING: 루브릭 Coverage + 모순 탐지 (표준)</li>
 *   <li>EXTRACTION: JSON Schema/OutputContract 우선, LLM Verify 생략 가능</li>
 *   <li>PLANNING: 단계 순서·선행조건 커버리지</li>
 *   <li>CREATIVE_WITH_CONSTRAINTS: Soft-verify (형식·금지어·명백한 모순만)</li>
 * </ul>
 */
@Component
public class PromptSpecValidator {

    private static final Pattern CONTRADICTION_PATTERN =
            Pattern.compile("(?i)(\\b\\w+\\b)\\s+.{0,50}\\b(아니다|아닙니다|not|isn't|is not)\\b");

    // String.matches()는 매 호출마다 Pattern.compile()을 실행하므로 static으로 캐시
    private static final Pattern QUANTITATIVE_CLAIM_PATTERN =
            Pattern.compile("\\d+%|\\d+배");

    /**
     * 초안(draft)을 PromptSpec 기준으로 검증한다.
     * Objective별 Verify 모드에 따라 검증 강도가 달라진다.
     */
    public VerifyResult verify(String draft, PromptSpec spec) {
        PromptObjective objective = spec.getObjective();
        PromptObjective.VerifyMode mode = objective.getVerifyMode();

        return switch (mode) {
            case CHAIN_OF_VERIFICATION -> verifyChainOfVerification(draft, spec);
            case SCHEMA_FIRST -> verifySchemaFirst(draft, spec);
            case SOFT -> verifySoft(draft, spec);
            case STANDARD -> verifyStandard(draft, spec);
        };
    }

    /**
     * FACTUAL — Chain-of-Verification: 사실/수치/인용 검증, CITE_OR_UNCERTAIN 강화.
     */
    private VerifyResult verifyChainOfVerification(String draft, PromptSpec spec) {
        Map<QualityRubric.RubricItem, Boolean> results = new EnumMap<>(QualityRubric.RubricItem.class);
        List<String> failures = new ArrayList<>();

        checkCoverage(draft, spec, results, failures);
        checkInputPreservation(draft, spec, results, failures);
        checkNoContradiction(draft, results, failures);
        checkUncertaintyHandling(draft, results, failures);
        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        return buildResult(results, failures);
    }

    /**
     * REASONING/PLANNING — 표준 검증.
     * <p>기본: Coverage + 모순 탐지.
     * <p>추가:
     * <ul>
     *     <li>REASONING: UNCERTAINTY_HANDLING</li>
     *     <li>PLANNING: INPUT_PRESERVATION</li>
     * </ul>
     */
    private VerifyResult verifyStandard(String draft, PromptSpec spec) {
        Map<QualityRubric.RubricItem, Boolean> results = new EnumMap<>(QualityRubric.RubricItem.class);
        List<String> failures = new ArrayList<>();

        PromptObjective objective = spec.getObjective();

        checkCoverage(draft, spec, results, failures);

        // Objective별 추가 루브릭 검증
        if (objective == PromptObjective.REASONING) {
            checkUncertaintyHandling(draft, results, failures);
        } else if (objective == PromptObjective.PLANNING) {
            checkInputPreservation(draft, spec, results, failures);
        }

        checkNoContradiction(draft, results, failures);
        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        return buildResult(results, failures);
    }

    /**
     * EXTRACTION — Schema 우선: JSON Schema 기반 검증, 통과 시 LLM Verify 생략 가능.
     */
    private VerifyResult verifySchemaFirst(String draft, PromptSpec spec) {
        Map<QualityRubric.RubricItem, Boolean> results = new EnumMap<>(QualityRubric.RubricItem.class);
        List<String> failures = new ArrayList<>();

        OutputContract contract = spec.getOutputContract();

        // FORMAT_COMPLIANCE: JSON Schema 검증
        boolean formatOk = checkFormatCompliance(draft, contract);
        results.put(QualityRubric.RubricItem.FORMAT_COMPLIANCE, formatOk);
        if (!formatOk) {
            failures.add("출력 형식이 지정된 JSON Schema를 준수하지 않습니다: " + contract.getFormat());
        }

        checkInputPreservation(draft, spec, results, failures);
        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        return buildResult(results, failures);
    }

    /**
     * CREATIVE_WITH_CONSTRAINTS — Soft-verify: 형식·금지어·명백한 모순만 체크.
     * 의미적 창의성 판단, 스타일·톤 평가는 제외한다.
     */
    private VerifyResult verifySoft(String draft, PromptSpec spec) {
        Map<QualityRubric.RubricItem, Boolean> results = new EnumMap<>(QualityRubric.RubricItem.class);
        List<String> failures = new ArrayList<>();

        if (draft == null || draft.isBlank()) {
            failures.add("생성 결과가 비어 있습니다.");
            return buildResult(results, failures);
        }

        OutputContract contract = spec.getOutputContract();
        if (contract.hasJsonSchema()) {
            boolean formatOk = checkFormatCompliance(draft, contract);
            results.put(QualityRubric.RubricItem.FORMAT_COMPLIANCE, formatOk);
            if (!formatOk) failures.add("지정 형식을 준수하지 않습니다.");
        }

        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        // 명백한 모순만 체크 (A이다 + A가 아니다 패턴)
        boolean noObviousContradiction = !hasObviousContradiction(draft);
        results.put(QualityRubric.RubricItem.NO_CONTRADICTION, noObviousContradiction);
        if (!noObviousContradiction) {
            failures.add("명백한 논리 모순이 감지되었습니다.");
        }

        return buildResult(results, failures);
    }

    // ─── 개별 검증 메서드 ───────────────────────────────

    private void checkCoverage(String draft, PromptSpec spec,
                                Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        boolean ok = draft != null && !draft.isBlank() && draft.length() >= 50;
        results.put(QualityRubric.RubricItem.COVERAGE, ok);
        if (!ok) failures.add("프롬프트 내용이 충분하지 않습니다 (최소 50자 미만).");
    }

    private void checkInputPreservation(String draft, PromptSpec spec,
                                         Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        String input = spec.getClarifiedInput();
        // 간단한 핵심 키워드 보존 검증: 원본 입력의 주요 단어가 초안에 포함되어 있는지
        boolean ok = input == null || input.isBlank() || draftPreservesKeywords(draft, input);
        results.put(QualityRubric.RubricItem.INPUT_PRESERVATION, ok);
        if (!ok) failures.add("입력 조건의 핵심 키워드가 생성 결과에 반영되지 않았습니다.");
    }

    private void checkNoContradiction(String draft,
                                       Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        boolean ok = !hasObviousContradiction(draft);
        results.put(QualityRubric.RubricItem.NO_CONTRADICTION, ok);
        if (!ok) failures.add("논리 모순(A이다 + A가 아니다)이 감지되었습니다.");
    }

    private void checkUncertaintyHandling(String draft,
                                           Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        // 수치/인용 주장이 있으면 근거 표현(probably, approximately, 추정, 참고)이 있어야 함
        boolean ok = !hasUnsubstantiatedClaim(draft);
        results.put(QualityRubric.RubricItem.UNCERTAINTY_HANDLING, ok);
        if (!ok) failures.add("검증 불가능한 수치·인용 주장에 근거 또는 불확실성 표시가 없습니다.");
    }

    private void checkNoProhibitedContent(String draft, ContentSandbox sandbox,
                                           Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        boolean ok = sandbox == null || !sandbox.containsViolation(draft);
        results.put(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, ok);
        if (!ok) failures.add("금지어 또는 내용 정책 위반 내용이 포함되어 있습니다.");
    }

    private boolean checkFormatCompliance(String draft, OutputContract contract) {
        if (contract == null || !contract.hasJsonSchema()) return true;
        if (draft == null || draft.isBlank()) return false;
        // JSON 형식 기본 검증 (실제 schema 검증은 ConstrainedDecodingPort에 위임)
        String trimmed = draft.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean hasObviousContradiction(String draft) {
        if (draft == null) return false;
        return CONTRADICTION_PATTERN.matcher(draft).find();
    }

    private boolean hasUnsubstantiatedClaim(String draft) {
        if (draft == null) return false;
        boolean hasQuantitativeClaim = QUANTITATIVE_CLAIM_PATTERN.matcher(draft).find();
        boolean hasHedging = draft.contains("추정") || draft.contains("참고")
                || draft.contains("approximately") || draft.contains("likely")
                || draft.contains("약 ") || draft.contains("출처:");
        return hasQuantitativeClaim && !hasHedging;
    }

    private boolean draftPreservesKeywords(String draft, String input) {
        if (input == null) return true;
        if (draft == null || draft.isBlank()) return false;
        // 최소 2자 이상: 1자짜리 조사·접속사는 키워드에서 제외
        List<String> keywords = java.util.Arrays.stream(input.split("\\s+"))
                .filter(w -> w.length() >= 2)
                .limit(5)
                .toList();
        if (keywords.isEmpty()) return true;
        long matched = keywords.stream().filter(draft::contains).count();
        return (double) matched / keywords.size() >= 0.4;
    }

    private VerifyResult buildResult(Map<QualityRubric.RubricItem, Boolean> results, List<String> failures) {
        boolean passed = failures.isEmpty();
        return passed ? VerifyResult.pass(results) : VerifyResult.fail(results, failures);
    }
}
