package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.springframework.lang.NonNull;

/**
 * 생성된 프롬프트를 저장하는 포트.
 *
 * <p>4단계 파이프라인(Clarify→Solve→Verify→Repair) 결과를 버전으로 저장한다.</p>
 * <p>save(…)의 spec은 호출자가 non-null로 보장해야 한다 (예: PromptSpecFactory.createFromConfirmedAxes).</p>
 */
public interface SavePromptVersionPort {

    /**
     * 생성 결과를 저장하고 저장된 프롬프트 ID를 반환한다.
     *
     * @param spec null이 아니어야 함
     */
    Long save(GeneratePromptCommand command, @NonNull PromptSpec spec,
              String finalContent, int repairCount, boolean finallyPassed);
}
