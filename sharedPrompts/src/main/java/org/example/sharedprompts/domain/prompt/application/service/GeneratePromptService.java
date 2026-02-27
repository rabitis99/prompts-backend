package org.example.sharedprompts.domain.prompt.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.QualityBadge;
import org.example.sharedprompts.domain.prompt.application.port.out.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.application.port.out.LLMClientPort;
import org.example.sharedprompts.domain.prompt.application.port.out.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.application.port.out.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;
import org.example.sharedprompts.domain.prompt.service.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.service.PromptSpecFactory;
import org.example.sharedprompts.domain.prompt.domain.service.PromptSpecValidator;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 프롬프트 생성 4단계 파이프라인 구현체.
 *
 * <pre>
 * 1) Clarify  : 입력 정규화, 누락 조건 자동 보강
 * 2) Solve    : PromptSpec 기반 LLM 초안 생성
 * 3) Verify   : Objective별 정책으로 검증
 * 4) Repair   : 실패 항목만 지목하여 수정 요청, 최대 2회
 *               2회 실패 시 현재 결과 반환 + 실패 로깅
 * </pre>
 *
 * <p><b>무한 루프 방지 보장:</b>
 * MAX_REPAIR_ATTEMPTS(2) 상수를 초과하면 즉시 루프를 종료하고
 * 현재 최선의 초안을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePromptService implements GeneratePromptUseCase {

    private static final int MAX_REPAIR_ATTEMPTS = 2;

    private final PromptSpecFactory promptSpecFactory;
    private final PromptSpecValidator promptSpecValidator;
    private final DomainResolver domainResolver;
    private final LLMClientPort llmClientPort;
    private final ConstrainedDecodingPort constrainedDecodingPort;
    private final SavePromptVersionPort savePromptVersionPort;
    private final PromptSpecRendererPort promptSpecRenderer;

    @Override
    public GeneratePromptResult generate(GeneratePromptCommand command) {
        log.info("[GeneratePrompt] 시작: objective 결정 중");

        // ─── 유저 사전 검증 (LLM 호출 전): 탈퇴·삭제 유저로 인한 비용 낭비 방지 ──
        savePromptVersionPort.validateUserExists(command.userId());

        // ─── 1. Clarify ──────────────────────────────────────────────────────
        PromptSpec spec = clarify(command);
        log.debug("[GeneratePrompt] Clarify 완료: objective={}, strategy={}",
                spec.getObjective(), spec.getStrategyBundle().getName());

        // ─── 2. Solve ─────────────────────────────────────────────────────────
        String draft = solve(spec);
        log.debug("[GeneratePrompt] Solve 완료: draftLength={}", draft.length());

        // ─── 3. Verify ────────────────────────────────────────────────────────
        VerifyResult verifyResult = verify(draft, spec);
        boolean firstPassSuccess = verifyResult.isPassed();
        log.debug("[GeneratePrompt] Verify 완료: passed={}", firstPassSuccess);

        // ─── 4. Repair (최대 2회) ─────────────────────────────────────────────
        int repairCount = 0;
        VerifyResult lastVerifyResult = verifyResult;

        if (!firstPassSuccess) {
            for (int attempt = 1; attempt <= MAX_REPAIR_ATTEMPTS; attempt++) {
                log.info("[GeneratePrompt] Repair 시도 {}/{}: failedItems={}",
                        attempt, MAX_REPAIR_ATTEMPTS,
                        lastVerifyResult.getFailedItems());

                draft = repair(draft, spec, lastVerifyResult);
                repairCount = attempt;

                lastVerifyResult = verify(draft, spec);
                if (lastVerifyResult.isPassed()) {
                    log.info("[GeneratePrompt] Repair {}회 후 Verify 통과", attempt);
                    break;
                }

                if (attempt == MAX_REPAIR_ATTEMPTS && !lastVerifyResult.isPassed()) {
                    // 무한 루프 방지: 상한 도달 시 현재 결과 반환 + 로깅
                    log.warn("[GeneratePrompt] Repair {}회 실패 — 현재 결과 반환: failureReasons={}",
                            MAX_REPAIR_ATTEMPTS, lastVerifyResult.getFailureReasons());
                }
            }
        }

        boolean finallyPassed = lastVerifyResult.isPassed();

        // ─── 저장 ─────────────────────────────────────────────────────────────
        Long promptId = savePromptVersionPort.save(command, spec, draft, repairCount, finallyPassed);
        log.info("[GeneratePrompt] 완료: promptId={}, repairCount={}, finallyPassed={}",
                promptId, repairCount, finallyPassed);

        // ─── 배지 결정 (내부 지표 → 배지 변환은 여기서 수행, 수치는 Result에서 분리) ──
        List<QualityBadge> badges = resolveBadges(lastVerifyResult, firstPassSuccess, repairCount, finallyPassed);

        return new GeneratePromptResult(
                promptId,
                command.title(),
                draft,
                badges,
                spec.getObjective(),
                firstPassSuccess,
                repairCount,
                finallyPassed
        );
    }

    // ─── 단계별 구현 ──────────────────────────────────────────────────────────

    /**
     * Clarify 단계 — 사용자 입력을 정규화하고 PromptSpec을 생성한다.
     * 누락된 필수 조건은 질문 대신 자동 보강 우선.
     */
    private PromptSpec clarify(GeneratePromptCommand command) {
        TaskDomain taskDomain = domainResolver.resolveDomain(
                command.actionType(), command.promptCategory());

        PromptSpec spec = promptSpecFactory.create(
                command.input(),
                taskDomain,
                command.actionType(),
                command.roleType(),
                command.tone(),
                command.style(),
                command.language(),
                command.experimentalEnabled(),
                ExperienceLevel.INTERMEDIATE,
                command.jsonSchema()
        );

        // 입력 정규화: 과도한 공백·특수문자 제거
        String clarifiedInput = normalizeInput(command.input());
        return spec.withClarifiedInput(clarifiedInput);
    }

    /**
     * Solve 단계 — PromptSpec 기반 초안 생성.
     * EXTRACTION Objective는 ConstrainedDecodingPort 우선 사용.
     */
    private String solve(PromptSpec spec) {
        if (spec.getObjective() == PromptObjective.EXTRACTION
                && spec.getOutputContract().hasJsonSchema()) {
            try {
                // 렌더링된 메타프롬프트를 constrainedDecoding에 전달 (LLM draft가 아님)
                String metaPrompt = promptSpecRenderer.render(spec);
                String constrained = constrainedDecodingPort.generateConstrained(
                        metaPrompt, spec.getOutputContract());
                // null/blank 결과는 verify의 FORMAT_COMPLIANCE로 처리 (LLM 예산 초과 방지)
                return constrained != null ? constrained : "";
            } catch (Exception e) {
                // 예외 시만 일반 LLM으로 fallback (1회 예산 내 대체 경로)
                log.warn("[GeneratePrompt] ConstrainedDecoding 실패, 일반 LLM 호출로 fallback: {}", e.getMessage());
            }
        }
        return llmClientPort.solve(spec);
    }

    /**
     * Verify 단계 — Objective별 정책으로 검증.
     */
    private VerifyResult verify(String draft, PromptSpec spec) {
        return promptSpecValidator.verify(draft, spec);
    }

    /**
     * Repair 단계 — 실패 항목만 지목하여 수정 요청.
     */
    private String repair(String draft, PromptSpec spec, VerifyResult verifyResult) {
        List<QualityRubric.RubricItem> failedItems = verifyResult.getFailedItems();
        List<String> failureReasons = verifyResult.getFailureReasons();

        String repairedDraft = llmClientPort.repair(draft, spec, failedItems, failureReasons);

        // Repair 결과가 null/blank면 원본 초안 유지
        if (repairedDraft == null || repairedDraft.isBlank()) {
            log.warn("[GeneratePrompt] Repair 결과가 비어있어 이전 초안 유지");
            return draft;
        }
        return repairedDraft;
    }

    /**
     * 배지 결정 — 내부 지표를 배지 목록으로 변환.
     * UX에는 수치가 아닌 배지만 노출한다.
     */
    private List<QualityBadge> resolveBadges(VerifyResult lastResult,
                                              boolean firstPassSuccess,
                                              int repairCount,
                                              boolean finallyPassed) {
        List<QualityBadge> badges = new ArrayList<>();

        if (finallyPassed) {
            badges.add(QualityBadge.CONDITIONS_MET);

            Boolean formatOk = lastResult.getItemResults().get(QualityRubric.RubricItem.FORMAT_COMPLIANCE);
            if (Boolean.TRUE.equals(formatOk)) {
                badges.add(QualityBadge.FORMAT_VERIFIED);
            }

            Boolean noProhibited = lastResult.getItemResults().get(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
            if (Boolean.TRUE.equals(noProhibited)) {
                badges.add(QualityBadge.NO_PROHIBITED_CONTENT);
            }

            if (firstPassSuccess && repairCount == 0) {
                badges.add(QualityBadge.FAST_GENERATION);
            } else if (repairCount > 0) {
                badges.add(QualityBadge.REVERIFIED);
            }
        } else {
            // 최종 실패 시에도 개별 통과 항목의 배지는 부여
            Boolean noProhibited = lastResult.getItemResults().get(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
            if (Boolean.TRUE.equals(noProhibited)) {
                badges.add(QualityBadge.NO_PROHIBITED_CONTENT);
            }
        }

        return badges;
    }

    private String normalizeInput(String input) {
        if (input == null) return "";
        return input.strip().replaceAll("\\s{3,}", "  ");
    }
}
