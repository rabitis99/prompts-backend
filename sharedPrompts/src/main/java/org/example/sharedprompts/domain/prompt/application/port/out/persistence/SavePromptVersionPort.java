package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.springframework.lang.NonNull;

/** 생성된 프롬프트 버전 저장 포트. spec은 호출자가 non-null 보장 */
public interface SavePromptVersionPort {

    Long save(GeneratePromptCommand command, @NonNull PromptSpec spec,
              String finalContent, int repairCount, boolean finallyPassed);
}
