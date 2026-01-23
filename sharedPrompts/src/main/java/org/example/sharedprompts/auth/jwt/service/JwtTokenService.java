package org.example.sharedprompts.auth.jwt.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.jwt.util.JwtTokenGenerator;
import org.example.sharedprompts.auth.jwt.util.JwtTokenParser;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.auth.redis.TokenVersionStore;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰 서비스
 * 
 * 고수준 토큰 관련 비즈니스 로직을 담당합니다.
 * - User 객체 기반 토큰 생성
 * - Claims에서 값 추출
 * - TokenVersion 관리
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtTokenGenerator tokenGenerator;
    private final JwtTokenParser tokenParser;
    private final TokenVersionStore tokenVersionStore;

    public TokenResponseDto getToken(User user) {
        Long tokenVersion = getTokenVersion(user.getId());
        
        String accessToken = tokenGenerator.generateAccessToken(
                user.getId(),
                user.getRole(),
                tokenVersion
        );

        String refreshToken = tokenGenerator.generateRefreshToken(
                user.getId(),
                user.getRole(),
                tokenVersion
        );

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public String generateAccessToken(User user) {
        Long tokenVersion = getTokenVersion(user.getId());
        return tokenGenerator.generateAccessToken(
                user.getId(),
                user.getRole(),
                tokenVersion
        );
    }

    public String generateRefreshToken(User user) {
        Long tokenVersion = getTokenVersion(user.getId());
        return tokenGenerator.generateRefreshToken(
                user.getId(),
                user.getRole(),
                tokenVersion
        );
    }

    /**
     * 사용자의 tokenVersion 조회 (캐시에서 가져오거나 없으면 0 반환)
     * 
     * @param userId 사용자 ID
     * @return tokenVersion
     */
    private Long getTokenVersion(Long userId) {
        Long version = tokenVersionStore.get(userId);
        return version != null ? version : 0L;
    }

    public Claims getClaims(String token) {
        return tokenParser.getClaims(token);
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

    /**
     * tokenVersion 검증
     * 
     * JWT에 포함된 tokenVersion과 현재 저장된 tokenVersion을 비교하여
     * 토큰이 유효한지 검증합니다.
     * 
     * @param claims JWT Claims
     * @param userId 사용자 ID
     * @return tokenVersion이 일치하면 true, 불일치하면 false
     */
    public boolean isTokenVersionValid(Claims claims, Long userId) {
        Long tokenVersionInJwt = getTokenVersionFromClaims(claims);
        Long currentTokenVersion = getTokenVersion(userId);
        return java.util.Objects.equals(tokenVersionInJwt, currentTokenVersion);
    }
}

