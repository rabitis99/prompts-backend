package org.example.sharedprompts.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.like.PromptLike;
import org.example.sharedprompts.domain.like.PromptLikeId;
import org.example.sharedprompts.domain.like.repository.PromptLikeRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromptLikeDomainService {

    private final PromptLikeRepository promptLikeRepository;
    private final UserRepository userRepository;
    private final PromptRepository promptRepository;

    @Transactional
    public PromptLike like(Long userId, Long promptId) {
        validateIdsNotNull(userId, promptId);

        PromptLikeId id = new PromptLikeId(promptId, userId);

        // getReferenceById()는 프록시를 반환하므로 불필요한 existsById() 호출 제거
        // 엔티티가 존재하지 않으면 나중에 예외가 발생하므로 별도 존재 확인 불필요
        User user = userRepository.getReferenceById(userId);
        Prompt prompt = promptRepository.getReferenceById(promptId);

        PromptLike promptLike = PromptLike.builder()
                .id(id)
                .user(user)
                .prompt(prompt)
                .build();

        return savePromptLikeOrThrow(promptLike);
    }

    @Transactional
    public void unlike(Long userId, Long promptId) {
        validateIdsNotNull(userId, promptId);
        PromptLikeId id = new PromptLikeId(promptId, userId);

        if (!promptLikeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.PROMPT_LIKE_NOT_FOUND);
        }

        promptLikeRepository.deleteById(id);
    }

    private void validateIdsNotNull(Long userId, Long promptId) {
        if (userId == null || promptId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private PromptLike savePromptLikeOrThrow(PromptLike promptLike) {
        // 중복 좋아요 등 DB 제약 조건 위반은 GlobalExceptionHandler에서
        // DataIntegrityViolationException과 제약 조건 이름을 기준으로 매핑한다.
        return promptLikeRepository.save(promptLike);
    }
}


