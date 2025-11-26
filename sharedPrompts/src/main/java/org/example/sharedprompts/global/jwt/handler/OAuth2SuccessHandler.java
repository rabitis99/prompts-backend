package org.example.sharedprompts.global.jwt.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.jwt.JwtUtil;
import org.example.sharedprompts.global.jwt.PrincipalDetails;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final TokenRedisService tokenRedisService;

    // 프론트 SPA 주소
    private static final String FRONT_REDIRECT_URL = "http://localhost:5173/oauth/success";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        // JWT 생성 (임시 저장용)
        String accessToken = jwtUtil.generateAccessToken(
                principal.getId(),
                principal.getEmail(),
                principal.getRole(),
                principal.getNickname(),
                principal.getProvider()
        );
        String refreshToken = jwtUtil.generateRefreshToken(
                principal.getId(),
                principal.getEmail(),
                principal.getRole(),
                principal.getNickname(),
                principal.getProvider()
        );

        // SecureRandom 기반 임시 key + state 생성
        String tempKey = RandomGenerator.randomKey();
        String state = RandomGenerator.randomState();

        // Redis에 임시 key + state 저장 (예: 3분 유효)
        tokenRedisService.saveTempToken(tempKey, accessToken, refreshToken, state, Duration.ofMinutes(3));

        // 프론트로 리다이렉트 시 key + state 전달
        String redirectUrl = FRONT_REDIRECT_URL + "?key=" + tempKey + "&state=" + state;
        response.sendRedirect(redirectUrl);
    }
}
