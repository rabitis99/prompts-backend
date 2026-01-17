package org.example.sharedprompts.global.jwt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.redis.TokenVersionCacheService;
import org.springframework.stereotype.Service;

/**
 * 사용자 상태 변경 시 JWT 토큰 무효화를 담당하는 서비스
 * 
 * 사용자 차단, 권한 변경, 삭제 등 상태 변경 시
 * tokenVersion을 증가시켜 기존 JWT 토큰을 무효화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTokenInvalidationService {

    private final TokenVersionCacheService tokenVersionCacheService;

    /**
     * 사용자의 모든 JWT 토큰을 무효화
     * 
     * 사용자 상태 변경(차단, 권한 변경, 삭제 등) 시 호출하여
     * 기존에 발급된 모든 JWT 토큰을 무효화합니다.
     * 
     * @param userId 사용자 ID
     */
    public void invalidateUserTokens(Long userId) {
        tokenVersionCacheService.incrementTokenVersion(userId);
        log.debug("사용자 토큰 무효화: userId={}", userId);
    }

    /**
     * 사용자 차단 시 토큰 무효화
     * 
     * @param userId 사용자 ID
     */
    public void invalidateTokensOnBlock(Long userId) {
        invalidateUserTokens(userId);
        log.info("사용자 차단으로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 사용자 차단 해제 시 토큰 무효화
     * 
     * @param userId 사용자 ID
     */
    public void invalidateTokensOnUnblock(Long userId) {
        invalidateUserTokens(userId);
        log.info("사용자 차단 해제로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 사용자 권한 변경 시 토큰 무효화
     * 
     * @param userId 사용자 ID
     */
    public void invalidateTokensOnRoleChange(Long userId) {
        invalidateUserTokens(userId);
        log.info("사용자 권한 변경으로 인한 토큰 무효화: userId={}", userId);
    }

    /**
     * 사용자 삭제 시 토큰 무효화
     * 
     * @param userId 사용자 ID
     */
    public void invalidateTokensOnDelete(Long userId) {
        invalidateUserTokens(userId);
        log.info("사용자 삭제로 인한 토큰 무효화: userId={}", userId);
    }
}

