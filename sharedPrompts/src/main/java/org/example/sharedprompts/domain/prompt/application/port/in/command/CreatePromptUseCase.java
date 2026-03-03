package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;

/**
 * 프롬프트 생성 유즈케이스 포트 (v1).
 *
 * <p>컨트롤러는 이 인터페이스만 의존하여 프롬프트 생성 플로우를 실행한다.</p>
 */
public interface CreatePromptUseCase {

    /**
     * 프롬프트를 생성한다.
     */
    PromptResponseDto create(PromptRequestDto request, Long userId);
}

