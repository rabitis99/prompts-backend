package org.example.sharedprompts.auth.rate.filter.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.util.ValidationUtils;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Rate Limit 필터 컨텍스트
 * Rate Limit 필터 처리 중 사용되는 컨텍스트 정보를 담는 불변 객체입니다.
 * 확장 가능한 구조로 설계되었습니다.
 */
@Getter
@RequiredArgsConstructor
public class RateLimitFilterContext {
    
    private final HttpServletRequest request;
    private final Authentication authentication;
    private final Long userId;

    /**
     * IP 기반 필터용 컨텍스트 생성
     */
    public static RateLimitFilterContext forIp(HttpServletRequest request) {
        return new RateLimitFilterContext(request, null, null);
    }

    /**
     * 사용자 기반 필터용 컨텍스트 생성
     */
    public static RateLimitFilterContext forUser(
            HttpServletRequest request,
            Authentication authentication,
            Long userId
    ) {
        ValidationUtils.requireNonNull(userId, "userId must not be null for user-based context");
        return new RateLimitFilterContext(request, authentication, userId);
    }

    /**
     * 사용자 ID를 Optional로 반환합니다.
     */
    public Optional<Long> getUserIdOpt() {
        return Optional.ofNullable(userId);
    }

    /**
     * Authentication을 Optional로 반환합니다.
     */
    public Optional<Authentication> getAuthenticationOpt() {
        return Optional.ofNullable(authentication);
    }

    /**
     * IP 기반 컨텍스트인지 확인합니다.
     */
    public boolean isIpBased() {
        return userId == null;
    }

    /**
     * 사용자 기반 컨텍스트인지 확인합니다.
     */
    public boolean isUserBased() {
        return userId != null;
    }
}
