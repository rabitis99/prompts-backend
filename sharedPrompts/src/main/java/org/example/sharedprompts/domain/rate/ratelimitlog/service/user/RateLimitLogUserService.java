package org.example.sharedprompts.domain.rate.ratelimitlog.service.user;

import org.example.sharedprompts.domain.user.User;

/**
 * Rate Limit 로그용 User 조회 서비스 인터페이스
 * 
 * Rate Limit 로그 저장 시 필요한 User 조회를 담당합니다.
 * 조회 실패 시에도 로그 저장은 계속 진행됩니다.
 */
public interface RateLimitLogUserService {

    /**
     * User를 조회합니다.
     * userId가 null이면 null을 반환합니다.
     * 조회 실패 시에도 null을 반환하여 로그 저장을 계속 진행합니다.
     * 
     * @param userId 사용자 ID
     * @return User 엔티티 (조회 실패 시 null)
     */
    User findUserSafely(Long userId);
}
