package org.example.sharedprompts.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.ValidationUtils;
import org.springframework.stereotype.Service;

/**
 * 사용자 검증 서비스
 * 
 * 사용자 상태 검증 정책을 집중 관리합니다.
 * OAuth, JWT, Refresh, Admin 등 모든 인증 흐름에서 재사용됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    
    /**
     * 사용자 검증 (확장 가능한 구조)
     * 
     * 모든 검증 규칙을 통합하여 실행합니다.
     * 검증 순서: 삭제 상태 우선 검증 후 차단 상태 검증
     * 
     * @param user 검증할 사용자
     * @throws ApiException 검증 실패 시
     */
    public void validate(User user) {
        ValidationUtils.requireNonNull(user, "user");
        
        // 삭제 상태를 먼저 검증 (USER_NOT_FOUND 우선 반환)
        validateNotDeleted(user);
        // 차단 상태 검증 (FORBIDDEN 반환)
        validateNotBlocked(user);
        // validateDormant(user);  // 미래 확장
    }
    
    /**
     * 차단 상태 검증
     */
    public void validateNotBlocked(User user) {
        if (user.isBlocked()) {
            log.warn("차단된 사용자 접근 시도: userId={}", user.getId());
            throw new ApiException(ErrorCode.FORBIDDEN, "차단된 사용자입니다.");
        }
    }
    
    /**
     * 삭제 상태 검증
     */
    public void validateNotDeleted(User user) {
        if (user.isDeleted()) {
            log.warn("삭제된 사용자 접근 시도: userId={}", user.getId());
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "삭제된 사용자입니다.");
        }
    }
}





