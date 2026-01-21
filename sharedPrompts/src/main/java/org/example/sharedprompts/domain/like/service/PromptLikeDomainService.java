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
import org.springframework.dao.DataIntegrityViolationException;
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
        validateUserExists(userId);
        validatePromptExists(promptId);

        PromptLikeId id = new PromptLikeId(promptId, userId);

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
        PromptLikeId id = new PromptLikeId(promptId, userId);

        if (!promptLikeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.PROMPT_LIKE_NOT_FOUND);
        }

        promptLikeRepository.deleteById(id);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validatePromptExists(Long promptId) {
        if (!promptRepository.existsById(promptId)) {
            throw new ApiException(ErrorCode.PROMPT_NOT_FOUND);
        }
    }

    private PromptLike savePromptLikeOrThrow(PromptLike promptLike) {
        try {
            return promptLikeRepository.save(promptLike);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.PROMPT_ALREADY_LIKED);
        }
    }
}


