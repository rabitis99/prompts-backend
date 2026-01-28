package org.example.sharedprompts.auth.jwt.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.util.JwtErrorResponseWriter;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * JWT 토큰 검증 서비스
 * 
 * JWT 토큰의 검증 로직을 담당합니다.
 * - JWT 서명 검증
 * - 만료 시간 검증
 * - Claim 파싱 및 검증
 * - tokenVersion 검증
 * - Redis 토큰 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenValidationService {

    private final JwtTokenService jwtTokenService;
    private final TokenRedisService tokenRedisService;
    private final JwtErrorResponseWriter jwtErrorResponseWriter;

    /**
     * JWT 토큰 검증 결과
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Claims claims;
        private final Long userId;
        private final String role;
        private final String errorMessage;

        private ValidationResult(boolean valid, Claims claims, Long userId, String role, String errorMessage) {
            this.valid = valid;
            this.claims = claims;
            this.userId = userId;
            this.role = role;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success(Claims claims, Long userId, String role) {
            return new ValidationResult(true, claims, userId, role, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, null, null, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public Claims getClaims() {
            return claims;
        }

        public Long getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * JWT 토큰 검증
     * 
     * 2단계 토큰 검증 전략:
     * 1단계 (필수): JWT 서명 검증, 만료 시간 검증, Claim 파싱, tokenVersion 검증
     * 2단계 (선택적): Redis 토큰 검증 (Redis 장애 시 Fail-Open 정책 적용)
     * 
     * @param token JWT 토큰
     * @return 검증 결과
     */
    public ValidationResult validateToken(String token) {
        // ==================== 1단계: JWT 서명 및 만료 시간 검증 (필수) ====================
        Claims claims;
        try {
            claims = jwtTokenService.getClaims(token);
        } catch (Exception e) {
            // JWT 서명 검증 실패 또는 만료된 토큰
            log.warn("JWT 서명 검증 실패 또는 만료된 토큰: {}", e.getMessage());
            return ValidationResult.failure("JWT 서명 검증 실패 또는 만료된 토큰: " + e.getMessage());
        }

        Long userId = jwtTokenService.getUserIdFromClaims(claims);
        String role = jwtTokenService.getRoleFromClaims(claims);

        // 필수 필드 검증
        if (userId == null || role == null) {
            return ValidationResult.failure(String.format("필수 클레임 누락: userId=%s, role=%s", userId, role));
        }

        // tokenVersion 검증 (soft delete, 차단 등 상태 변경 검증)
        if (!jwtTokenService.isTokenVersionValid(claims, userId)) {
            return ValidationResult.failure(String.format("토큰 버전 불일치: userId=%s", userId));
        }

        // ==================== 2단계: Redis 토큰 검증 (선택적, Fail-Open) ====================
        // Redis는 선택 검증 단계로 처리: Redis 장애 시에도 JWT 서명 검증 통과 시 인증 허용
        // TokenRedisService의 Circuit Breaker fallback이 Fail-Open 정책을 적용하므로
        // 예외가 발생하지 않고 true를 반환합니다.
        boolean isRedisTokenValid = tokenRedisService.isAccessTokenValid(token);

        if (!isRedisTokenValid) {
            // Redis에서 토큰이 없거나 만료된 경우 (Redis가 정상 동작 중일 때만)
            // Circuit Breaker가 Open 상태이거나 Redis 장애 시에는 fallback에서 true를 반환하므로
            // 이 분기는 Redis가 정상 동작 중이고 토큰이 실제로 없거나 만료된 경우에만 실행됩니다.
            log.warn("Redis 토큰 검증 실패: userId={}, token={}, 토큰이 Redis에 없거나 만료됨",
                    userId, SensitiveDataMasker.maskToken(token));
            return ValidationResult.failure(String.format("Redis 토큰 검증 실패: userId=%s", userId));
        }

        return ValidationResult.success(claims, userId, role);
    }

    /**
     * 무효한 토큰 처리
     * 
     * 토큰 삭제 및 에러 응답을 일관된 방식으로 처리합니다.
     * 
     * @param token 무효한 토큰
     * @param response HTTP 응답
     * @param errorMessage 무효화 사유
     * @throws IOException 응답 작성 실패 시
     */
    public void handleInvalidToken(String token, HttpServletResponse response, String errorMessage) throws IOException {
        log.warn("JWT 토큰 무효화: token={}, reason={}",
                SensitiveDataMasker.maskToken(token), errorMessage);
        // Redis 삭제는 실패해도 무시 (Fail-Open 정책)
        try {
            tokenRedisService.deleteAccessToken(token);
        } catch (Exception e) {
            log.debug("Redis 토큰 삭제 실패 (무시): token={}", SensitiveDataMasker.maskToken(token));
        }
        jwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }
}

