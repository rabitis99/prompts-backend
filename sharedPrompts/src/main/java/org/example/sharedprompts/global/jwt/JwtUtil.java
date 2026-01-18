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
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final TokenTtlProperties ttlConfig;

    public JwtUtil(@Value("${jwt.secret}") String secret, TokenTtlProperties ttlConfig) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlConfig = ttlConfig;
    }

    /** Access Token 생성 */
    public String generateAccessToken(
            Long userId,
            Role role,
            String nickname,
            Provider provider,
            String providerId,
            Long tokenVersion
    ) {
        return generateToken(
                userId,
                role,
                nickname,
                provider,
                providerId,
                tokenVersion,
                ttlConfig.getAccessTokenValidity()
        );
    }

    /** Refresh Token 생성 */
    public String generateRefreshToken(
            Long userId,
            Role role,
            String nickname,
            Provider provider,
            String providerId,
            Long tokenVersion
    ) {
        return generateToken(
                userId,
                role,
                nickname,
                provider,
                providerId,
                tokenVersion,
                ttlConfig.getRefreshTokenValidity()
        );
    }

    /** 실제 JWT 생성 로직 */
    private String generateToken(
            Long userId,
            Role role,
            String nickname,
            Provider provider,
            String providerId,
            Long tokenVersion,
            long validityMinutes
    ) {
        long expirationMillis = TimeUnit.MINUTES.toMillis(validityMinutes);

        return Jwts.builder()
                // 내부 사용자 식별자
                .setSubject(String.valueOf(userId))

                // 권한 및 인증 정보
                .claim("role", role.name())
                .claim("nickname", nickname)
                .claim("provider", provider.name())
                .claim("providerId", providerId)
                .claim("tokenVersion", tokenVersion != null ? tokenVersion : 0L)

                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** JWT Claims 파싱 */
    public Claims getClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
