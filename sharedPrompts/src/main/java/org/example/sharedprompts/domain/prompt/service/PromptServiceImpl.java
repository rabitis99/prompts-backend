package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.google.gemini.GoogleGeminiService;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final GoogleGeminiService googleGeminiService;
    private final PromptGenerator promptGenerator;
    private final PromptTagService promptTagService;

    @Override
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {

        // 1. 유저 조회 (트랜잭션 필요 없음)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 2. 프롬프트 텍스트 생성
        InputRequestDto dto = request.toInputRequestDto();
        String promptText = promptGenerator.generatePrompt(dto);

        // 3. 외부 API 호출 (트랜잭션 밖)
        String aiGeneratedContent = googleGeminiService.chat(promptText)
                .timeout(Duration.ofSeconds(30))
                .switchIfEmpty(Mono.error(new ApiException(ErrorCode.AI_GENERATION_FAILED)))
                .onErrorResume(e -> {
                    log.error("AI 콘텐츠 생성 실패", e);
                    return Mono.error(new ApiException(ErrorCode.AI_GENERATION_FAILED));
                })
                .block();

        // 4. 저장 구간만 트랜잭션으로 분리
        Prompt promptEntity = savePrompt(request, user, aiGeneratedContent);

        // 5. 태그 처리
        List<Tag> tags = (request.getTags() != null)
                ? promptTagService.addTags(promptEntity, request.getTags())
                : List.of();

        return PromptResponseDto.from(promptEntity, tags);
    }

    @Transactional
    protected Prompt savePrompt(PromptRequestDto request, User user, String aiGeneratedContent) {
        Prompt prompt = request.toEntity(user, aiGeneratedContent);
        return promptRepository.save(prompt);
    }

    // ============ 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition) {
        Page<Prompt> page = promptRepository.searchPrompts(condition);
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
    @Transactional(readOnly = true)
    public PromptResponseDto getPromptDetail(Long promptId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
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
