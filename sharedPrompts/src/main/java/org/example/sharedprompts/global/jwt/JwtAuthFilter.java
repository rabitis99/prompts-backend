package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.jwt.util.JwtErrorResponseWriter;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.redis.TokenVersionCacheService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * JWT 인증 필터
 * 
 * 책임:
 * - 토큰 서명 검증
 * - 만료 시간 검증
 * - Claim 파싱
 * - tokenVersion 검증 (soft delete, 차단 등 상태 변경 검증)
 * 
 * 금지:
 * - Repository/Service 호출
 * - DB 접근
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;
    private final TokenVersionCacheService tokenVersionCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!tokenRedisService.isAccessTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtProvider.getClaims(token);

            Long userId = jwtProvider.getUserIdFromClaims(claims);
            String role = jwtProvider.getRoleFromClaims(claims);
            String nickname = jwtProvider.getNicknameFromClaims(claims);
            String provider = jwtProvider.getProviderFromClaims(claims);
            String providerId = jwtProvider.getProviderIdFromClaims(claims);

            if (userId == null || role == null) {
                tokenRedisService.deleteAccessToken(token);
                filterChain.doFilter(request, response);
                return;
            }

            // tokenVersion 검증 (soft delete, 차단 등 상태 변경 검증)
            Long tokenVersionInJwt = jwtProvider.getTokenVersionFromClaims(claims);
            Long currentTokenVersion = tokenVersionCacheService.getTokenVersion(userId);

            // null-safe 비교: tokenVersion이 null이거나 불일치하면 토큰 무효화
            if (!Objects.equals(tokenVersionInJwt, currentTokenVersion)) {
                log.warn("토큰 버전 불일치: userId={}, jwtVersion={}, currentVersion={}", 
                        userId, tokenVersionInJwt, currentTokenVersion);
                tokenRedisService.deleteAccessToken(token);
                // 일관된 형식으로 에러 응답 작성
                JwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            PrincipalDetails principal =
                    PrincipalDetails.fromJwtClaims(
                            userId,
                            nickname,
                            role,
                            provider,
                            providerId
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("JWT 처리 중 오류 발생: {}", e.getMessage());
            tokenRedisService.deleteAccessToken(token);
        }

        filterChain.doFilter(request, response);
    }
}
