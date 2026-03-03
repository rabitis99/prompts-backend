package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;

/**
 * 프롬프트 조회 관련 유즈케이스 포트.
 */
public interface PromptQueryUseCase {

    PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition, Long viewerId);

    PromptResponseDto getPromptDetail(Long promptId, Long viewerId);

    PageResponse<PromptResponseDto> getMyPrompts(Long userId, PromptSearchCondition condition);

    PageResponse<PromptResponseDto> getUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId);
}

