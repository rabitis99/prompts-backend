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
                user.getNickname(),
                user.getProvider()       // provider 추가
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getNickname(),
                user.getProvider()       // provider 추가
        );

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public String generateAccessToken(User user) {
        return jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getNickname(),
                user.getProvider()       // provider 추가
        );
    }

    public String generateRefreshToken(User user) {
        // TODO: RefreshToken 생성 시 사용자 요청 IP도 포함하도록 확장 필요
        //  - 향후 보안 강화를 위해 RefreshToken 페이로드에 clientIp 포함 예정
        //  - jwtUtil.generateRefreshToken(...) 시그니처 변경 필요
        //  - 프록시/로드밸런서 환경 고려하여 실제 클라이언트 IP 추출 로직도 함께 설계할 것
        return jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getNickname(),
                user.getProvider()   // provider 포함
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

    public String getEmailFromClaims(Claims claims) {
        return claims.get("email", String.class);
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
}
