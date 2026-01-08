package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtUtil jwtUtil;

    public TokenResponseDto getToken(User user) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId()
        );

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public String generateAccessToken(User user) {
        return jwtUtil.generateAccessToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId()
        );
    }

    public String generateRefreshToken(User user) {
        // TODO: RefreshToken 생성 시 사용자 요청 IP 포함 예정
        return jwtUtil.generateRefreshToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId()
                // + clientIp (예정)
        );
    }

    public Claims getClaims(String token) {
        return jwtUtil.getClaims(token);
    }

    public Long getUserIdFromClaims(Claims claims) {
        String sub = claims.getSubject();
        if (sub == null) return null;

        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 userId 형식: " + sub, e);
        }
    }

    public String getRoleFromClaims(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getNicknameFromClaims(Claims claims) {
        return claims.get("nickname", String.class);
    }

    public String getProviderFromClaims(Claims claims) {
        return claims.get("provider", String.class);
    }

    public String getProviderIdFromClaims(Claims claims) {
        return claims.get("providerId", String.class);
    }
}
