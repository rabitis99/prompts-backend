package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.Tag.Tag;
import org.example.sharedprompts.domain.Tag.service.PromptTagService;
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

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final GoogleGeminiService googleGeminiService;
    private final PromptGenerator promptGenerator;
    private final PromptTagService promptTagService;

    @Override
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        InputRequestDto dto = request.toInputRequestDto();
        String promptText = promptGenerator.generatePrompt(dto);

        String aiGeneratedContent = googleGeminiService.chat(promptText).block();
        if (aiGeneratedContent == null) aiGeneratedContent = "";

        Prompt promptEntity = request.toEntity(user, aiGeneratedContent);
        promptRepository.save(promptEntity);

        List<Tag> tags = promptTagService.addTags(promptEntity, request.getTags());

        return PromptResponseDto.from(promptEntity, tags);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromptResponseDto> getPrompts(PromptSearchCondition condition) {
        Page<Prompt> page = promptRepository.searchPrompts(condition);
        return PageResponse.of(page.map(p -> PromptResponseDto.from(p, promptTagService.getTags(p))));
    }

    @Override
    @Transactional(readOnly = true)
    public PromptResponseDto getPromptDetail(Long promptId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
        List<Tag> tags = promptTagService.getTags(prompt);
        return PromptResponseDto.from(prompt, tags);
    }

    @Override
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

    @Override
    public void deletePrompt(Long promptId, Long userId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));

        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROMPT_FORBIDDEN);
        }

        promptRepository.delete(prompt);
    }

    @Override
    public PageResponse<PromptResponseDto> getMyPrompts(Long userId, PromptSearchCondition condition) {
        Page<Prompt> page = promptRepository.searchMyPrompts(userId, condition);
        return PageResponse.of(page.map(p -> PromptResponseDto.from(p, promptTagService.getTags(p))));
    }
}
