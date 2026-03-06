package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;

/**
 * 통합 프롬프트 생성 유즈케이스.
 *
 * <p>단일 엔진 경로로 프롬프트를 생성하며, 커맨드의 옵션에 따라
 * 품질 파이프라인·엔진 모드 등을 적용한다.</p>
 */
public interface GenerateUnifiedPromptUseCase {

    /**
     * 통합 설정으로 프롬프트를 생성한다.
     */
    UnifiedGeneratePromptResult generate(UnifiedGeneratePromptCommand command);
}

