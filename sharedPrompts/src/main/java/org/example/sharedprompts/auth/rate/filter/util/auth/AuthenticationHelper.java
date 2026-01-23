package org.example.sharedprompts.auth.rate.filter.util.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 인증 관련 헬퍼 클래스
 * 
 * Rate Limit 필터에서 사용하는 인증 관련 유틸리티를 통합 제공합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthenticationHelper {

    /**
     * 인증된 사용자인지 확인합니다.
     * 
     * @return 인증된 사용자 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * 현재 인증된 Authentication을 조회합니다.
     * 
     * @return Optional<Authentication> (인증되지 않았으면 empty)
     */
    public static Optional<Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    /**
     * Authentication에서 사용자 ID를 추출합니다.
     * 
     * @param authentication Authentication 객체
     * @return Optional<Long> 사용자 ID (추출 불가능한 경우 empty)
     */
    public static Optional<Long> extractUserId(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof PrincipalDetails principalDetails) {
            return Optional.of(principalDetails.getId());
        }

        return Optional.empty();
    }

    /**
     * 현재 인증된 사용자의 ID를 조회합니다.
     * 
     * @return Optional<Long> 사용자 ID (인증되지 않았거나 추출 불가능한 경우 empty)
     */
    public static Optional<Long> getCurrentUserId() {
        return getAuthentication()
                .flatMap(AuthenticationHelper::extractUserId);
    }
}




