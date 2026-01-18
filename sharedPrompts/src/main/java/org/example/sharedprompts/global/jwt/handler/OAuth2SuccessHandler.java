package org.example.sharedprompts.global.jwt.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.jwt.JwtUtil;
import org.example.sharedprompts.global.jwt.PrincipalDetails;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.redis.TokenVersionCacheService;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final TokenRedisService tokenRedisService;
    private final TokenVersionCacheService tokenVersionCacheService;

    @Value("${oauth2.redirect.front-url}")
    private String frontRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        // tokenVersion 조회 및 초기화 (없으면 0)
        Long tokenVersion = tokenVersionCacheService.getTokenVersion(principal.getId());
        if (tokenVersion.equals(0L)) {
            tokenVersionCacheService.initializeTokenVersion(principal.getId());
            tokenVersion = 0L; // initializeTokenVersion은 0으로 초기화하므로 재조회 불필요
        }

        // JWT 생성 (email 제거)
        String accessToken = jwtUtil.generateAccessToken(
                principal.getId(),
                principal.getRole(),
                principal.getNickname(),
                principal.getProvider(),
                principal.getProviderId(),
                tokenVersion
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                principal.getId(),
                principal.getRole(),
                principal.getNickname(),
                principal.getProvider(),
                principal.getProviderId(),
                tokenVersion
        );

        // SecureRandom 기반 임시 key + state 생성
        String tempKey = RandomGenerator.randomKey();
        String state = RandomGenerator.randomState();

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

        // 프론트로 리다이렉트 (key + state 전달)
        String redirectUrl = frontRedirectUrl
                + "?key=" + URLEncoder.encode(tempKey, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
