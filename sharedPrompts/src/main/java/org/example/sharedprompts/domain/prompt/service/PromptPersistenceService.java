package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.application.port.out.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.event.PromptEventPublisher;
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
    private final PromptCommandPort promptCommandPort;
    private final PromptTagService promptTagService;
    private final PromptNotificationService promptNotificationService;
    private final PromptEventPublisher promptEventPublisher;

    /**
     * 프롬프트 저장과 태그 처리, 트랜잭션 관리를 담당한다.
     * 
     * <p><strong>트랜잭션 전파 전략:</strong>
     * 기본 전략({@code REQUIRED})을 사용하여 상위 트랜잭션에 참여합니다.
     * 프롬프트 저장이 실패하면 상위 트랜잭션도 함께 롤백되어 데이터 일관성을 보장합니다.
     */
    @Transactional(timeout=30)
    public PromptResponseDto savePrompt(PromptRequestDto request, Long userId, String aiGeneratedContent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Prompt promptEntity = request.toEntity(user, aiGeneratedContent);
        promptEntity = promptCommandPort.save(promptEntity);

        List<Tag> tags = (request.getTags() != null)
                ? promptTagService.addTags(promptEntity, request.getTags())
                : List.of();

        // 트랜잭션 커밋 후 이벤트 발행 등 후처리
        promptNotificationService.publishPromptCreated(promptEntity, user);
        
        // 통계 캐시 무효화를 위한 이벤트 발행 (트랜잭션 내부에서 발행, 커밋 후 처리됨)
        promptEventPublisher.publishPromptCreated(promptEntity.getId(), userId);

        return PromptResponseDto.from(promptEntity, tags);
    }
}


