package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.verification.VerificationContext;

/**
 * PromptSpec 기준으로 생성된 프롬프트 초안을 검증하는 도메인 서비스.
 *
 * <p>Objective별 검증 강도 분기는 {@link ObjectiveRegistry}를 통해 조회한
 * {@link org.example.sharedprompts.domain.prompt.domain.verification.VerificationStrategy}에 위임한다.
 * switch/case 없음, Spring 의존 없음.
 *
 * <p>새 Objective가 추가되어도 이 클래스는 수정 불필요(OCP).
 */
public class PromptSpecValidator {

    private final ObjectiveRegistry objectiveRegistry;

    public PromptSpecValidator(ObjectiveRegistry objectiveRegistry) {
        this.objectiveRegistry = objectiveRegistry;
    }

    /**
     * 초안(draft)을 PromptSpec 기준으로 검증한다.
     * Objective별 전략은 ObjectiveRegistry가 결정한다.
     */
    public VerifyResult verify(String draft, PromptSpec spec) {
        return objectiveRegistry
                .get(spec.getObjective())
                .verificationStrategy()
                .verify(new VerificationContext(spec, draft));
    }
}
