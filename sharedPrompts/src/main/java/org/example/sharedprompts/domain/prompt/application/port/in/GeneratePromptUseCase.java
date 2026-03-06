package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;

/**
 * 프롬프트 생성 유즈케이스 포트 — 4단계 파이프라인(Clarify → Solve → Verify → Repair)의 진입점.
 */
public interface GeneratePromptUseCase {

    /**
     * 프롬프트를 생성한다.
     */
    GeneratePromptResult generate(GeneratePromptCommand command);
}
