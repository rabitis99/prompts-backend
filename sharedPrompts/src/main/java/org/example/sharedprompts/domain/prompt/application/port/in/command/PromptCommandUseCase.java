package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;

/**
 * 프롬프트 수정/삭제 유즈케이스 포트.
 */
public interface PromptCommandUseCase {

    PromptResponseDto updatePrompt(Long promptId, PromptUpdateDto promptUpdateDto, Long userId);

    void deletePrompt(Long promptId, Long userId);
}

