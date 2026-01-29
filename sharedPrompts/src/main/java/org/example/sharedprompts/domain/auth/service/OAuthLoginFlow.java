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

@Component
@RequiredArgsConstructor
public class OAuthLoginFlow {

    private final TokenRedisService tokenRedisService;
    private final UserRepository userRepository;
    private final AuthAuditPublisher authAuditPublisher;
    private final OAuth2StateService oAuth2StateService;

    public OAuthLoginPayload validateForConfirm(String tempKey, String state) {
        if (!oAuth2StateService.validateAndConsume(state)) {
            authAuditPublisher.loginFailWithoutPrincipal(AuthFailReason.OAUTH2_STATE_MISMATCH);
            throw new ApiException(ErrorCode.OAUTH2_STATE_MISMATCH);
        }

        Map<String, String> sessionData = tokenRedisService.getAndDeleteOAuth2TempSession(tempKey);

        if (sessionData == null || sessionData.isEmpty()) {
            authAuditPublisher.loginFailWithoutPrincipal(AuthFailReason.OAUTH2_INVALID_CODE);
            throw new ApiException(ErrorCode.OAUTH2_INVALID_CODE);
        }

        String providerStr = sessionData.get(Constant.PROVIDER_KEY);
        String providerId = sessionData.get(Constant.PROVIDER_ID_KEY);
        String sessionState = sessionData.get(Constant.STATE_KEY);

        if (providerStr == null || providerId == null) {
            Provider provider = providerStr != null ? Provider.valueOf(providerStr) : null;
            authAuditPublisher.loginFailByProviderId(provider, providerId, AuthFailReason.OAUTH2_TOKEN_INVALID);
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_INVALID);
        }

        if (sessionState == null || !sessionState.equals(state)) {
            authAuditPublisher.loginFailWithoutPrincipal(AuthFailReason.OAUTH2_STATE_MISMATCH);
            throw new ApiException(ErrorCode.OAUTH2_STATE_MISMATCH);
        }

        Provider provider = Provider.valueOf(providerStr);
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> {
                    authAuditPublisher.loginFailByProviderId(provider, providerId, AuthFailReason.USER_NOT_FOUND);
                    return new ApiException(ErrorCode.USER_NOT_FOUND);
                });

        return new OAuthLoginPayload(user);
    }

    record OAuthLoginPayload(User user) {
    }
}


