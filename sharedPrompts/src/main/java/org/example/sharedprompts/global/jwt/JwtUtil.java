package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final TokenTtlProperties ttlConfig;

    public JwtUtil(@Value("${jwt.secret}") String secret, TokenTtlProperties ttlConfig) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlConfig = ttlConfig;
    }

    // AccessToken 생성
    public String generateAccessToken(Long userId, String email, Role role, String nickname, Provider provider) {
        return generateToken(userId, email, role, nickname, provider, ttlConfig.getAccessTokenValidity());
    }

    // RefreshToken 생성
    public String generateRefreshToken(Long userId, String email, Role role, String nickname, Provider provider) {
        return generateToken(userId, email, role, nickname, provider, ttlConfig.getRefreshTokenValidity());
    }

    // 실제 토큰 생성
    private String generateToken(Long userId, String email, Role role, String nickname, Provider provider, long validity) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .claim("nickname", nickname)
                .claim("provider", provider.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Claims 조회
    public Claims getClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
