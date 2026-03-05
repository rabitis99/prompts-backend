package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;

/**
 * 통합 프롬프트 생성 유즈케이스.
 *
 * <p>외부에서는 이 포트만 의존하고, 내부에서는 V2 품질 파이프라인과
 * V3 Intent 메타 해석을 조합하여 동작한다.</p>
 */
public interface GenerateUnifiedPromptUseCase {

    UnifiedGeneratePromptResult generate(UnifiedGeneratePromptCommand command);
}

