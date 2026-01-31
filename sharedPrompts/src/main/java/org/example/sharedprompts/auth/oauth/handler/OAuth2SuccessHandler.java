package org.example.sharedprompts.auth.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.state.OAuth2StateService;
import org.example.sharedprompts.auth.oauth.util.OAuth2RedirectUrlBuilder;
import org.example.sharedprompts.auth.oauth.util.PrincipalDetailsExtractor;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * OAuth2 인증 성공 핸들러
 * 
 * JWT 발급 없이 임시 인증 세션만 저장합니다.
 * 로그인 확정은 POST /auth/confirm에서만 수행됩니다 (RateLimit 적용).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenRedisService tokenRedisService;
    private final OAuth2StateService oAuth2StateService;

    @Value("${oauth2.redirect.front-url}")
    private String frontRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principal = PrincipalDetailsExtractor.extractForOAuth2(authentication);

        String state = oAuth2StateService.generateState();
        String tempKey = RandomGenerator.randomKey();
        String provider = principal.getProvider().name();
        String providerId = principal.getProviderId();

        tokenRedisService.saveOAuth2TempSession(
                tempKey,
                provider,
                providerId,
                state,
                Duration.ofMinutes(2)
        );

        log.debug("OAuth2 임시 인증 세션 생성 완료: tempKey={}, provider={}", 
                tempKey, provider);

        String redirectUrl = OAuth2RedirectUrlBuilder.buildCallbackUrl(frontRedirectUrl, tempKey, state);
        response.sendRedirect(redirectUrl);
    }
}

