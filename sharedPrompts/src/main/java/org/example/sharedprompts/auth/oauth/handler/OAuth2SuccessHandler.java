package org.example.sharedprompts.auth.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.auth.jwt.service.JwtTokenService;
import org.example.sharedprompts.auth.oauth.state.OAuth2StateService;
import org.example.sharedprompts.auth.oauth.util.OAuth2RedirectUrlBuilder;
import org.example.sharedprompts.auth.oauth.util.PrincipalDetailsExtractor;
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
 * OAuth2 인증 성공 후 JWT 토큰 생성 및 임시 토큰 저장, 성공 로그 기록을 담당합니다.
 * 
 * 책임:
 * - JWT 토큰 생성 (JwtTokenService 위임)
 * - 임시 토큰 저장
 * - OAuth2 State 생성
 * - 성공 로그 기록
 * - 프론트엔드 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final TokenRedisService tokenRedisService;
    private final OAuth2StateService oAuth2StateService;
    private final AuthAuditPublisher authAuditPublisher;

    @Value("${oauth2.redirect.front-url}")
    private String frontRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // PrincipalDetails 추출 및 OAuth2 필수 필드 검증
        PrincipalDetails principal = PrincipalDetailsExtractor.extractForOAuth2(authentication);

        // JWT 토큰 생성 (PrincipalDetails의 정보 사용 - 중복 DB 조회 불필요)
        String accessToken = jwtTokenService.generateAccessToken(
                principal.getId(),
                principal.getRole()
        );
        String refreshToken = jwtTokenService.generateRefreshToken(
                principal.getId(),
                principal.getRole()
        );

        // OAuth2 State 및 임시 키 생성
        String state = oAuth2StateService.generateState();
        String tempKey = RandomGenerator.randomKey();
        
        String provider = principal.getProvider().name();
        String providerId = principal.getProviderId();

        tokenRedisService.saveTempToken(
                tempKey,
                accessToken,
                refreshToken,
                state,
                provider,
                providerId,
                Duration.ofMinutes(3)
        );

        // 성공 로그 기록
        authAuditPublisher.loginSuccess(
                principal.getProvider(),
                principal.getProviderId(),
                principal.getId(),
                request
        );

        // 프론트엔드로 리다이렉트
        String redirectUrl = OAuth2RedirectUrlBuilder.buildCallbackUrl(frontRedirectUrl, tempKey, state);
        response.sendRedirect(redirectUrl);
    }
}

