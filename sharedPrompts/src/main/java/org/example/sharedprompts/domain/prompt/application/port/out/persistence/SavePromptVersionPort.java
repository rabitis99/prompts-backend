package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * 생성된 프롬프트를 저장하는 포트.
 *
 * <p>4단계 파이프라인(Clarify→Solve→Verify→Repair) 결과를 버전으로 저장한다.</p>
 */
public interface SavePromptVersionPort {

    /**
     * 생성 결과를 저장하고 저장된 프롬프트 ID를 반환한다.
     */
    Long save(GeneratePromptCommand command, PromptSpec spec,
              String finalContent, int repairCount, boolean finallyPassed);
}
