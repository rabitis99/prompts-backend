package org.example.sharedprompts.domain.admin.util;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 관리자 작업에서 사용하는 엔티티 조회 로직을 담당하는 헬퍼 클래스
 */
@Component
@RequiredArgsConstructor
public class AdminEntityFinder {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;

    /**
     * 사용자 조회 (관리자는 삭제된 사용자도 조회 가능)
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws ApiException 사용자를 찾을 수 없는 경우
     */
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 프롬프트 조회
     * @param promptId 프롬프트 ID
     * @return Prompt 엔티티
     * @throws ApiException 프롬프트를 찾을 수 없는 경우
     */
    public Prompt findPromptById(Long promptId) {
        return promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
    }
}

