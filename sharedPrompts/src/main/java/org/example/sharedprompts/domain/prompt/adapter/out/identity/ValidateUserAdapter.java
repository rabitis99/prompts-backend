package org.example.sharedprompts.domain.prompt.adapter.out.identity;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.identity.ValidateUserPort;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 사용자 존재 여부 검증을 DB(UserRepository)에 위임하는 어댑터.
 *
 * <p>프롬프트 생성 플로우에서 LLM 호출 전에 호출되며,
 * 탈퇴·삭제된 유저로 인한 불필요한 비용을 방지한다.
 */
@Component
@RequiredArgsConstructor
public class ValidateUserAdapter implements ValidateUserPort {

    private final UserRepository userRepository;

    @Override
    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
