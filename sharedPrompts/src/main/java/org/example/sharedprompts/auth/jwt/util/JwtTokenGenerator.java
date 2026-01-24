package org.example.sharedprompts.auth.jwt.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.domain.user.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 유틸리티
 * 
 * 저수준 토큰 생성 로직만 담당합니다.
 * - Access Token 생성
 * - Refresh Token 생성
 */
@Component
public class JwtTokenGenerator {

    private final SecretKey secretKey;
    private final TokenTtlProperties ttlConfig;

    public JwtTokenGenerator(@Value("${jwt.secret}") String secret, TokenTtlProperties ttlConfig) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlConfig = ttlConfig;
    }

    /** Access Token 생성 */
    public String generateAccessToken(
            Long userId,
            Role role,
            Long tokenVersion
    ) {
        return generateToken(
                userId,
                role,
                tokenVersion,
                ttlConfig.getAccessTokenValidityMillis()
        );
    }

    /** Refresh Token 생성 */
    public String generateRefreshToken(
            Long userId,
            Role role,
            Long tokenVersion
    ) {
        return generateToken(
                userId,
                role,
                tokenVersion,
                ttlConfig.getRefreshTokenValidityMillis()
        );
    }

    /** 실제 JWT 생성 로직 */
    private String generateToken(
            Long userId,
            Role role,
            Long tokenVersion,
            long validityMillis
    ) {
        Date expiration = new Date(System.currentTimeMillis() + validityMillis);

        return Jwts.builder()
                // 내부 사용자 식별자
                .setSubject(String.valueOf(userId))

                // 권한 및 인증 정보
                .claim("role", role.name())
                .claim("tokenVersion", tokenVersion != null ? tokenVersion : 0L)

                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}

