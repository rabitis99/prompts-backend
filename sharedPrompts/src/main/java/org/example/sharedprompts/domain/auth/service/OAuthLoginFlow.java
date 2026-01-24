package org.example.sharedprompts.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.auth.oauth.state.OAuth2StateService;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.constant.Constant;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OAuth2 로그인 전체 플로우 중, callback 검증 + User 조회를 담당하는 컴포넌트
 *
 * - Redis에 저장된 임시 토큰 조회
 * - state, provider, providerId 유효성 검증
 * - User 조회 및 실패 케이스별 Audit 이벤트 발행
 */
@Component
@RequiredArgsConstructor
public class OAuthLoginFlow {

    private final TokenRedisService tokenRedisService;
    private final UserRepository userRepository;
    private final AuthAuditPublisher authAuditPublisher;
    private final OAuth2StateService oAuth2StateService;

    public OAuthLoginPayload validate(String key, String state) {
        // State 검증 (HMAC + Redis 1회용 보장)
        if (!oAuth2StateService.validateAndConsume(state)) {
            authAuditPublisher.loginFailWithoutPrincipal(AuthFailReason.OAUTH2_STATE_MISMATCH);
            throw new ApiException(ErrorCode.OAUTH2_STATE_MISMATCH);
        }

        Map<String, String> tokens = tokenRedisService.getAndDeleteTempToken(key);

        if (tokens == null) {
            authAuditPublisher.loginFailWithoutPrincipal(AuthFailReason.OAUTH2_INVALID_CODE);
            throw new ApiException(ErrorCode.OAUTH2_INVALID_CODE);
        }

        String providerStr = tokens.get(Constant.PROVIDER_KEY);
        String providerId = tokens.get(Constant.PROVIDER_ID_KEY);

        if (providerStr == null || providerId == null) {
            Provider provider = providerStr != null ? Provider.valueOf(providerStr) : null;

            authAuditPublisher.loginFailByProviderId(provider, providerId, AuthFailReason.OAUTH2_TOKEN_INVALID);
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_INVALID);
        }

        Provider provider = Provider.valueOf(providerStr);

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> {
                    authAuditPublisher.loginFailByProviderId(provider, providerId, AuthFailReason.USER_NOT_FOUND);
                    return new ApiException(ErrorCode.USER_NOT_FOUND);
                });

        return new OAuthLoginPayload(user);
    }

    /**
     * OAuth2 callback 검증 결과 Payload
     */
    record OAuthLoginPayload(User user) {
    }
}


