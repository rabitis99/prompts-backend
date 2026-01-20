package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.follow.policy.FollowBlockPolicy;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.event.PromptCreatedEvent;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.prompt.service.guideline.GuidelineBuilderFactory;
import org.example.sharedprompts.domain.prompt.service.guideline.PromptGuidelineBuilder;
import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.domain.prompt.repository.PromptSearchContext;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.google.gemini.GoogleGeminiService;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final FollowBlockPolicy followBlockPolicy;
    private final GoogleGeminiService googleGeminiService;
    private final PromptGenerator promptGenerator;
    private final PromptTagService promptTagService;
    private final GuidelineBuilderFactory guidelineBuilderFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<PromptResponseDto> createPrompt(PromptRequestDto request, Long userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND)))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(user -> {
                InputRequestDto dto = request.toInputRequestDto();
                String promptText = promptGenerator.generatePrompt(dto);
                
                return googleGeminiService.chat(promptText)
                    .flatMap(aiGeneratedContent -> {
                        PromptGuidelineBuilder builder = 
                            guidelineBuilderFactory.getBuilder(dto.getLanguage());
                        String prompt = builder.build(aiGeneratedContent, dto);
                        return Mono.fromCallable(() -> savePrompt(request, user, prompt))
                            .subscribeOn(Schedulers.boundedElastic());
                    });
            });
    }

    @Transactional
    protected PromptResponseDto savePrompt(PromptRequestDto request, User user, String aiGeneratedContent) {
        Prompt promptEntity = request.toEntity(user, aiGeneratedContent);
        promptEntity = promptRepository.save(promptEntity);

        List<Tag> tags = (request.getTags() != null)
                ? promptTagService.addTags(promptEntity, request.getTags())
                : List.of();

        // 프롬프트 생성 도메인 이벤트 발행 (트랜잭션 커밋 이후 SSE 알림 전송)
        eventPublisher.publishEvent(
                new PromptCreatedEvent(
                        promptEntity.getId(),
                        user.getId(),
                        user.getNickname(),
                        promptEntity.getDescription()
                )
        );

        return PromptResponseDto.from(promptEntity, tags);
    }

    // ============ 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition, Long viewerId) {
        // 검색 조건(condition)과 viewer 컨텍스트를 분리하여 전달
        PromptSearchContext context = PromptSearchContext.of(condition, viewerId);

        Page<Prompt> page = promptRepository.searchPrompts(context);
        return PageResponse.of(page.map(
                p -> PromptResponseDto.from(
                        p,
                        p.getPromptTags().stream()
                                .map(PromptTag::getTag)
                                .toList()
                )
        ));
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
        return PromptResponseDto.from(prompt, tags);
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

        promptUpdateDto.applyTo(prompt);

        if (promptUpdateDto.getTags() != null) {
            promptTagService.updateTags(prompt, promptUpdateDto.getTags());
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
