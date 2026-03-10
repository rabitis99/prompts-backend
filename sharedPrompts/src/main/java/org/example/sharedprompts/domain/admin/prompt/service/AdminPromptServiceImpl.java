package org.example.sharedprompts.domain.admin.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.util.AdminAuditLogger;
import org.example.sharedprompts.domain.admin.util.AdminEntityFinder;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.prompt.application.port.out.like.LikeCountPort;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
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
    private final LikeCountPort likeCountPort;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPromptResponseDto> getPrompts(Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        Page<Prompt> page = promptRepository.findAll(pageable);
        return mapToAdminDtos(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPromptResponseDto> searchPrompts(String keyword, Pageable pageable) {
        adminValidator.validatePageSize(pageable, 100);
        String trimmedKeyword = normalizeKeyword(keyword);
        if (trimmedKeyword == null || trimmedKeyword.isEmpty()) {
            return getPrompts(pageable);
        }
        Page<Prompt> page = promptRepository.searchPromptsForAdmin(trimmedKeyword, pageable);
        return mapToAdminDtos(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPromptResponseDto getPrompt(Long promptId) {
        Prompt prompt = entityFinder.findPromptById(promptId);
        long likeCount = likeCountPort.getPromptLikeCounts(java.util.List.of(promptId))
                .getOrDefault(promptId, 0L);
        return AdminPromptResponseDto.from(prompt, likeCount);
    }

    @Override
    @Transactional
    public void deletePrompt(Long promptId, Long adminId) {
        User admin = entityFinder.findUserById(adminId);
        Prompt prompt = entityFinder.findPromptById(promptId);

        promptRepository.delete(prompt);
        log.info("프롬프트 삭제: promptId={}, adminId={}", promptId, adminId);

        adminAuditLogger.logPromptDelete(admin, promptId);
    }

    @Override
    @Transactional
    public AdminPromptResponseDto togglePromptVisibility(Long promptId, Long adminId, PromptVisibilityRequestDto requestDto) {
        User admin = entityFinder.findUserById(adminId);
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

        adminAuditLogger.logPromptVisibilityChange(admin, promptId, isPublic);

        long likeCount = likeCountPort.getPromptLikeCounts(java.util.List.of(promptId))
                .getOrDefault(promptId, 0L);
        return AdminPromptResponseDto.from(prompt, likeCount);
    }

    private Page<AdminPromptResponseDto> mapToAdminDtos(Page<Prompt> page) {
        var promptIds = page.getContent().stream().map(Prompt::getId).toList();
        var likeCountByPromptId = likeCountPort.getPromptLikeCounts(promptIds);
        return page.map(p -> AdminPromptResponseDto.from(p, likeCountByPromptId.getOrDefault(p.getId(), 0L)));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}

