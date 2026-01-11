package org.example.sharedprompts.domain.prompt.service;

import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.response.PageResponse;
import reactor.core.publisher.Mono;

public interface PromptService {

    Mono<PromptResponseDto> createPrompt(PromptRequestDto request, Long userId);

    PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition);

    PromptResponseDto getPromptDetail(Long promptId);

    PromptResponseDto updatePrompt(Long promptId, PromptUpdateDto promptUpdateDto, Long userId);

    void deletePrompt(Long promptId, Long userId);

    PageResponse<PromptResponseDto> getMyPrompts(Long userId, PromptSearchCondition condition);
}
