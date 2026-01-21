package org.example.sharedprompts.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.util.AdminAuditLogger;
import org.example.sharedprompts.domain.admin.util.AdminEntityFinder;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.admin.request.PromptVisibilityRequestDto;
import org.example.sharedprompts.dto.admin.response.AdminPromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPromptServiceImpl implements AdminPromptService {

    private final PromptRepository promptRepository;
    private final AdminValidator adminValidator;
    private final AdminEntityFinder entityFinder;
    private final AdminAuditLogger adminAuditLogger;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPromptResponseDto> getPrompts(Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        return promptRepository.findAll(pageable)
                .map(AdminPromptResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPromptResponseDto> searchPrompts(String keyword, Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        String trimmedKeyword = normalizeKeyword(keyword);
        if (trimmedKeyword == null || trimmedKeyword.isEmpty()) {
            return getPrompts(pageable);
        }

        return promptRepository.searchPromptsForAdmin(trimmedKeyword, pageable)
                .map(AdminPromptResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPromptResponseDto getPrompt(Long promptId) {
        Prompt prompt = entityFinder.findPromptById(promptId);
        return AdminPromptResponseDto.from(prompt);
    }

    @Override
    @Transactional
    public void deletePrompt(Long promptId, Long adminId) {
        Prompt prompt = entityFinder.findPromptById(promptId);

        promptRepository.delete(prompt);
        log.info("프롬프트 삭제: promptId={}, adminId={}", promptId, adminId);

        User admin = entityFinder.findUserById(adminId);
        adminAuditLogger.logPromptDelete(admin, promptId);
    }

    @Override
    @Transactional
    public AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto) {
        Prompt prompt = entityFinder.findPromptById(promptId);

        Boolean isPublic = requestDto.getIsPublic();
        if (isPublic == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        boolean oldVisibility = prompt.isPublic();
        adminValidator.validateNotSameVisibility(oldVisibility, isPublic);

        prompt.updateIsPublic(isPublic);

        log.info("프롬프트 공개 상태 변경: promptId={}, oldVisibility={}, newVisibility={}, adminId={}",
                promptId, oldVisibility, isPublic, adminId);

        User admin = entityFinder.findUserById(adminId);
        adminAuditLogger.logPromptVisibilityChange(admin, promptId, isPublic);

        return AdminPromptResponseDto.from(prompt);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}


