package org.example.sharedprompts.domain.prompt.application.port.in.generate;

import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.UnifiedGeneratePromptResult;

/** 확정된 시맨틱 축 기반 프롬프트 생성 유즈케이스 */
public interface GeneratePromptFromConfirmedAxesUseCase {

    UnifiedGeneratePromptResult generate(ConfirmedGeneratePromptCommand command);
}
