package org.example.sharedprompts.auth.oauth.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;

/**
 * PrincipalDetails 추출 및 검증 유틸리티
 * 
 * <p>Authentication에서 PrincipalDetails를 안전하게 추출하고 검증합니다.
 * 타입 검사 및 OAuth2 관련 필수 필드 검증을 수행합니다.
 * 
 * <p>모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PrincipalDetailsExtractor {

    /**
     * Authentication에서 PrincipalDetails를 추출합니다.
     * 
     * <p>타입 검사를 수행하여 ClassCastException을 방지합니다.
     * PrincipalDetails가 아닌 경우 적절한 예외를 발생시킵니다.
     * 
     * @param authentication Authentication 객체
     * @return PrincipalDetails
     * @throws ApiException PrincipalDetails가 아닌 경우
     */
    public static PrincipalDetails extract(Authentication authentication) {
        if (authentication == null) {
            log.error("Authentication이 null입니다.");
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, 
                    "인증 정보가 없습니다.");
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof PrincipalDetails principal)) {
            log.error("PrincipalDetails가 아닌 타입이 반환됨: type={}", 
                    principalObj != null ? principalObj.getClass().getName() : "null");
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, 
                    "인증 정보 형식이 올바르지 않습니다.");
        }

        return principal;
    }

    /**
     * OAuth2 인증을 위한 PrincipalDetails를 추출하고 검증합니다.
     * 
     * <p>PrincipalDetails를 추출한 후, OAuth2 인증에 필수인 provider와 providerId를 검증합니다.
     * 
     * @param authentication Authentication 객체
     * @return PrincipalDetails (provider와 providerId가 검증된 상태)
     * @throws ApiException PrincipalDetails가 아니거나 provider/providerId가 없는 경우
     */
    public static PrincipalDetails extractForOAuth2(Authentication authentication) {
        PrincipalDetails principal = extract(authentication);
        
        // provider와 providerId null 검증 (OAuth2 인증 성공 시점에서는 필수)
        if (principal.getProvider() == null) {
            log.error("OAuth2 인증에서 provider가 null입니다. userId={}", principal.getId());
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_REQUIRED);
        }
        if (principal.getProviderId() == null || principal.getProviderId().isBlank()) {
            log.error("OAuth2 인증에서 providerId가 null이거나 비어있습니다. userId={}, provider={}", 
                    principal.getId(), principal.getProvider().name());
            throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ID_MISSING);
        }
        
        return principal;
    }
}

