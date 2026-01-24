package org.example.sharedprompts.domain.user.event;

import org.example.sharedprompts.domain.user.enums.Provider;

/**
 * 사용자 등록 완료 이벤트
 * 
 * 트랜잭션 커밋 후 후속 작업(알림, 추천인 처리 등)을 위해 사용됩니다.
 * JPA Entity를 직접 포함하지 않고 스냅샷 값만 포함하여
 * AFTER_COMMIT 단계에서 안전하게 사용할 수 있도록 합니다.
 */
public record UserRegisteredEvent(
        Long userId,
        Provider provider,
        String email,
        String nickname
) {
    public static UserRegisteredEvent of(Long userId, Provider provider, 
                                         String email, String nickname) {
        return new UserRegisteredEvent(userId, provider, email, nickname);
    }
}





