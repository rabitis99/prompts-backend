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
                user.getEmail(),
                user.getRole(),
                user.getNickname()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getNickname()
        );

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public String generateAccessToken(User user) {
        return jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getNickname()
        );
    }

    public String generateRefreshToken(User user) {
        return jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getNickname()
        );
    }

    public Claims getClaims(String token) {
        return jwtUtil.getClaims(token);
    }

    public Long getUserIdFromClaims(Claims claims) {
        String sub = claims.getSubject();
        return sub == null ? null : Long.valueOf(sub);
    }

    public String getEmailFromClaims(Claims claims) {
        return claims.get("email", String.class);
    }

    public String getRoleFromClaims(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getNicknameFromClaims(Claims claims) {
        return claims.get("nickname", String.class);
    }
}
