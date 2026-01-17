package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.redis.TokenVersionCacheService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtUtil jwtUtil;
    private final TokenVersionCacheService tokenVersionCacheService;

    public TokenResponseDto getToken(User user) {
        Long tokenVersion = getTokenVersion(user.getId());
        
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId(),
                tokenVersion
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId(),
                tokenVersion
        );

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public String generateAccessToken(User user) {
        Long tokenVersion = getTokenVersion(user.getId());
        return jwtUtil.generateAccessToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId(),
                tokenVersion
        );
    }

    public String generateRefreshToken(User user) {
        Long tokenVersion = getTokenVersion(user.getId());
        // TODO: RefreshToken 생성 시 사용자 요청 IP 포함 예정
        return jwtUtil.generateRefreshToken(
                user.getId(),
                user.getRole(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId(),
                tokenVersion
                // + clientIp (예정)
        );
    }

    /**
     * 사용자의 tokenVersion 조회 (캐시에서 가져오거나 없으면 0 반환)
     * 
     * @param userId 사용자 ID
     * @return tokenVersion
     */
    private Long getTokenVersion(Long userId) {
        return tokenVersionCacheService.getTokenVersion(userId);
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

    public Long getTokenVersionFromClaims(Claims claims) {
        Object version = claims.get("tokenVersion");
        if (version == null) {
            return 0L;
        }
        if (version instanceof Number) {
            return ((Number) version).longValue();
        }
        try {
            return Long.valueOf(version.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
