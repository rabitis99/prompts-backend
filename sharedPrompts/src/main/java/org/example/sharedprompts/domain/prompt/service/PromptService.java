package org.example.sharedprompts.domain.prompt.service;

import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.dto.common.PageResponse;

public interface PromptService {

    PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition, Long viewerId);

    PromptResponseDto getPromptDetail(Long promptId, Long viewerId);

    PromptResponseDto updatePrompt(Long promptId, PromptUpdateDto promptUpdateDto, Long userId);

    void deletePrompt(Long promptId, Long userId);

    PageResponse<PromptResponseDto> getMyPrompts(Long userId, PromptSearchCondition condition);

    PageResponse<PromptResponseDto> getUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId);
}
