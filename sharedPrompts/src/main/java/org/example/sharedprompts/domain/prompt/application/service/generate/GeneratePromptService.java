package org.example.sharedprompts.domain.prompt.application.service.generate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.LLMClientPort;
import org.example.sharedprompts.domain.prompt.application.port.out.render.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.application.port.out.identity.ValidateUserPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;

import org.example.sharedprompts.domain.prompt.domain.service.badge.BadgeResolver;
import org.example.sharedprompts.domain.prompt.domain.service.spec.InputNormalizer;
import org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecFactory;
import org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecValidator;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.springframework.stereotype.Service;

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
    private final DomainResolverPort domainResolver;
    private final LLMClientPort llmClientPort;
    private final ConstrainedDecodingPort constrainedDecodingPort;
    private final ValidateUserPort validateUserPort;
    private final SavePromptVersionPort savePromptVersionPort;
    private final PromptSpecRendererPort promptSpecRenderer;
    private final ObjectiveRegistry objectiveRegistry;
    private final BadgeResolver badgeResolver;

    @Override
    public GeneratePromptResult generate(GeneratePromptCommand command) {
        log.info("[GeneratePrompt] 시작: objective 결정 중");

        // ─── 유저 사전 검증 (LLM 호출 전): 탈퇴·삭제 유저로 인한 비용 낭비 방지 ──
        validateUserPort.validateUserExists(command.userId());

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

                if (attempt == MAX_REPAIR_ATTEMPTS) {
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

        // ─── 배지 결정 ────────────────────────────────────────────────────────
        List<QualityBadge> badges = badgeResolver.resolve(lastVerifyResult, firstPassSuccess, repairCount, finallyPassed);

        boolean formatValid = Boolean.TRUE.equals(
                lastVerifyResult.getItemResults().get(QualityRubric.RubricItem.FORMAT_COMPLIANCE));

        return new GeneratePromptResult(
                promptId,
                command.title(),
                draft,
                badges,
                spec.getObjective(),
                formatValid,
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
        ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(
                command.actionType(), command.promptCategory());
        if (resolved.fallback()) {
            log.warn("[GeneratePrompt] Domain fallback used — actionType={}, category={}, fallbackDomain={}. Consider adding mapping.",
                    command.actionType(), command.promptCategory(), resolved.domain());
        }
        TaskDomain taskDomain = resolved.domain();

        PromptSpec spec = promptSpecFactory.create(
                command.input(),
                taskDomain,
                command.actionType(),
                command.roleType(),
                command.tone(),
                command.style(),
                command.language(),
                command.experimentalEnabled(),
                command.experienceLevel(),
                command.jsonSchema()
        );

        // 입력 정규화: 과도한 공백·특수문자 제거
        String clarifiedInput = InputNormalizer.normalize(command.input());
        return spec.withClarifiedInput(clarifiedInput);
    }

    /**
     * Solve 단계 — PromptSpec 기반 초안 생성.
     * supportsConstrainedDecoding()이 true인 Objective는 ConstrainedDecodingPort 우선 사용.
     * ObjectiveRegistry에서 판단 — 직접 enum 비교 없음.
     */
    private String solve(PromptSpec spec) {
        boolean useConstrainedDecoding = objectiveRegistry
                .get(spec.getObjective())
                .supportsConstrainedDecoding();

        if (useConstrainedDecoding && spec.getOutputContract().hasJsonSchema()) {
            try {
                String metaPrompt = promptSpecRenderer.render(spec);
                String constrained = constrainedDecodingPort.generateConstrained(
                        metaPrompt, spec.getOutputContract());
                if (constrained != null && !constrained.isBlank()) {
                    return constrained;
                }
                log.warn("[GeneratePrompt] ConstrainedDecoding 결과가 비어 일반 LLM 호출로 fallback");
            } catch (Exception e) {
                log.warn("[GeneratePrompt] ConstrainedDecoding 실패, 일반 LLM 호출로 fallback: {}", e.getMessage());
                log.debug("[GeneratePrompt] ConstrainedDecoding 예외 상세", e);
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
}
