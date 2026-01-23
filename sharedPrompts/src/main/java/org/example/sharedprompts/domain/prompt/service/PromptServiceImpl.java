package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.policy.FollowBlockPolicy;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.domain.prompt.repository.PromptSearchContext;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.dto.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프롬프트 조회 / 수정 / 삭제를 담당하는 도메인 서비스 구현체입니다.
 * <p>
 * 프롬프트 생성(create) 플로우는 {@link org.example.sharedprompts.domain.prompt.facade.PromptFacade}
 * 및 하위 서비스(PromptSanitizationService, PromptAIService, PromptPersistenceService)가 담당합니다.
 * 이 구현체는 생성 이외의 CRUD 책임만 가집니다.
 */
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final PromptRepository promptRepository;
    private final FollowBlockPolicy followBlockPolicy;
    private final PromptTagService promptTagService;
    private final LikeCountService likeCountService;
    private final PromptSanitizationService promptSanitizationService;

    // ============ 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition, Long viewerId) {
        // 검색 조건(condition)과 viewer 컨텍스트를 분리하여 전달
        PromptSearchContext context = PromptSearchContext.of(condition, viewerId);

        Page<Prompt> page = promptRepository.searchPrompts(context);
        return mapToPromptResponsePage(page);
    }

    @Override
    @Transactional
    public PromptResponseDto getPromptDetail(Long promptId, Long viewerId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));

        // viewer와 author 간 BLOCKED 관계가 존재하면 접근 차단
        if (viewerId != null &&
                followBlockPolicy.isBlocked(viewerId, prompt.getAuthor().getId())) {
            throw new ApiException(ErrorCode.PROMPT_BLOCKED_VIEW);
        }

        promptRepository.incrementUsageCount(promptId);

        List<Tag> tags = promptTagService.getTags(prompt);
        Long likeCount = likeCountService
                .getPromptLikeCounts(List.of(promptId))
                .getOrDefault(promptId, prompt.getLikeCount());
        return PromptResponseDto.from(prompt, tags, likeCount);
    }

    // ============ 수정 ===============
    @Override
    @Transactional
    public PromptResponseDto updatePrompt(Long promptId, PromptUpdateDto promptUpdateDto, Long userId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));

        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROMPT_FORBIDDEN);
        }

        PromptUpdateDto sanitizedDto = promptSanitizationService.sanitize(promptUpdateDto);
        sanitizedDto.applyTo(prompt);

        if (sanitizedDto.getTags() != null) {
            promptTagService.updateTags(prompt, sanitizedDto.getTags());
        }

        List<Tag> tags = promptTagService.getTags(prompt);
        return PromptResponseDto.from(prompt, tags);
    }

    // ============ 삭제 ===============
    @Override
    @Transactional
    public void deletePrompt(Long promptId, Long userId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));

        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROMPT_FORBIDDEN);
        }

        promptTagService.updateTags(prompt, List.of());
        promptRepository.delete(prompt);
    }

    // ============ 내 프롬프트 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getMyPrompts(Long userId, PromptSearchCondition condition) {
        Page<Prompt> page = promptRepository.searchMyPrompts(userId, condition);
        return mapToPromptResponsePage(page);
    }

    // ============ 다른 사용자의 프롬프트 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId) {
        Page<Prompt> page = promptRepository.searchUserPrompts(userId, condition, viewerId);
        return mapToPromptResponsePage(page);
    }

    /**
     * Prompt 페이지를 PromptResponseDto 페이지로 변환하는 공통 메서드
     */
    private PageResponse<PromptResponseDto> mapToPromptResponsePage(Page<Prompt> page) {
        return PageResponse.of(page.map(
                p -> PromptResponseDto.from(
                        p,
                        p.getPromptTags().stream()
                                .map(PromptTag::getTag)
                                .toList()
                )
        ));
    }

}
