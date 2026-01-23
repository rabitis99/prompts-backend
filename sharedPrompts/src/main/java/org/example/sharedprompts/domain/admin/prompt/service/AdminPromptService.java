package org.example.sharedprompts.domain.admin.prompt.service;

import org.example.sharedprompts.dto.admin.request.PromptVisibilityRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminPromptResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminPromptService {

    Page<AdminPromptResponseDto> getPrompts(Pageable pageable);

    Page<AdminPromptResponseDto> searchPrompts(String keyword, Pageable pageable);

    AdminPromptResponseDto getPrompt(Long promptId);

    void deletePrompt(Long promptId, Long adminId);

    AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto);
}

