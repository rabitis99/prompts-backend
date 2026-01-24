package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptPersistenceService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final PromptTagService promptTagService;
    private final PromptNotificationService promptNotificationService;

    /**
     * 프롬프트 저장과 태그 처리, 트랜잭션 관리를 담당한다.
     */
    @Transactional
    public PromptResponseDto savePrompt(PromptRequestDto request, Long userId, String aiGeneratedContent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Prompt promptEntity = request.toEntity(user, aiGeneratedContent);
        promptEntity = promptRepository.save(promptEntity);

        List<Tag> tags = (request.getTags() != null)
                ? promptTagService.addTags(promptEntity, request.getTags())
                : List.of();

        // 트랜잭션 커밋 후 이벤트 발행 등 후처리
        promptNotificationService.publishPromptCreated(promptEntity, user);

        return PromptResponseDto.from(promptEntity, tags);
    }
}


