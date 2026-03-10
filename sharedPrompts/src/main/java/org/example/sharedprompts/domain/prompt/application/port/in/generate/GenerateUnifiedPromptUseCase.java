package org.example.sharedprompts.domain.prompt.application.port.in.generate;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;

/** 통합 프롬프트 생성 유즈케이스 */
public interface GenerateUnifiedPromptUseCase {

    UnifiedGeneratePromptResult generate(UnifiedGeneratePromptCommand command);
}
