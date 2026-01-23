package org.example.sharedprompts.auth.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.auth.jwt.service.JwtTokenService;
import org.example.sharedprompts.auth.oauth.state.OAuth2StateService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final TokenRedisService tokenRedisService;
    private final OAuth2StateService oAuth2StateService;
    private final UserRepository userRepository;
    private final AuthAuditPublisher authAuditPublisher;

    @Value("${oauth2.redirect.front-url}")
    private String frontRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        // 사용자 조회
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "OAuth2 인증 성공 후 사용자를 찾을 수 없습니다: userId=" + principal.getId()
                ));

        // JWT 토큰 생성 (JwtTokenService가 tokenVersion 관리)
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        // OAuth2StateService를 사용하여 State 생성 (HMAC 검증 포함)
        String state = oAuth2StateService.generateState();
        
        // SecureRandom 기반 임시 key 생성
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
        authAuditPublisher.loginSuccessByUser(user);

        // 프론트로 리다이렉트 (key + state 전달)
        String redirectUrl = frontRedirectUrl
                + "?key=" + URLEncoder.encode(tempKey, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}

