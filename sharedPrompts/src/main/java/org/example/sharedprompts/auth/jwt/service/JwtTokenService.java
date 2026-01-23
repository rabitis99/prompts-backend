package org.example.sharedprompts.auth.jwt.service;

import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.jwt.util.JwtTokenGenerator;
import org.example.sharedprompts.auth.jwt.util.JwtTokenParser;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.auth.redis.TokenVersionStore;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
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

    /**
     * Access Token과 Refresh Token을 모두 생성하여 반환
     * 
     * @param user 사용자 (null이 아니어야 함)
     * @return TokenResponseDto
     * @throws ApiException user가 null인 경우
     */
    public TokenResponseDto getToken(@NonNull User user) {
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        
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

    /**
     * Access Token 생성 (User 객체 기반)
     * 
     * @param user 사용자 (null이 아니어야 함)
     * @return Access Token
     * @throws ApiException user가 null인 경우
     */
    public String generateAccessToken(@NonNull User user) {
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return generateAccessToken(user.getId(), user.getRole());
    }

    /**
     * Refresh Token 생성 (User 객체 기반)
     * 
     * @param user 사용자 (null이 아니어야 함)
     * @return Refresh Token
     * @throws ApiException user가 null인 경우
     */
    public String generateRefreshToken(@NonNull User user) {
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return generateRefreshToken(user.getId(), user.getRole());
    }

    /**
     * Access Token 생성 (id, role 직접 전달)
     * PrincipalDetails 등에서 직접 호출할 때 사용
     */
    public String generateAccessToken(Long userId, Role role) {
        Long tokenVersion = getTokenVersion(userId);
        return tokenGenerator.generateAccessToken(userId, role, tokenVersion);
    }

    /**
     * Refresh Token 생성 (id, role 직접 전달)
     * PrincipalDetails 등에서 직접 호출할 때 사용
     */
    public String generateRefreshToken(Long userId, Role role) {
        Long tokenVersion = getTokenVersion(userId);
        return tokenGenerator.generateRefreshToken(userId, role, tokenVersion);
    }

    /**
     * 사용자의 tokenVersion 조회 (캐시에서 가져오거나 없으면 0 반환)
     * 
     * @param userId 사용자 ID
     * @return tokenVersion (값이 없으면 0L 반환)
     */
    private Long getTokenVersion(Long userId) {
        return tokenVersionStore.get(userId);
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
            // JWT 토큰 파싱 중 발생하는 오류이므로 ApiException으로 처리
            throw new ApiException(ErrorCode.UNAUTHORIZED, null, 
                    "유효하지 않은 JWT 토큰 형식입니다.", e);
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

