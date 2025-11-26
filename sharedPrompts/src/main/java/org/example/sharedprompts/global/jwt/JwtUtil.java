package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.global.config.TokenTtlConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final TokenTtlConfig ttlConfig;

    public JwtUtil(@Value("${jwt.secret}") String secret, TokenTtlConfig ttlConfig) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlConfig = ttlConfig;
    }

    public String generateAccessToken(Long userId, String email, Role role, String nickname) {
        return generateToken(userId, email, role, nickname, ttlConfig.getAccessTokenValidity());
    }

    public String generateRefreshToken(Long userId, String email, Role role, String nickname) {
        return generateToken(userId, email, role, nickname, ttlConfig.getRefreshTokenValidity());
    }

    private String generateToken(Long userId, String email, Role role, String nickname, long validity) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("nickname", nickname)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
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

