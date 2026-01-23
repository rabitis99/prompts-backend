package org.example.sharedprompts.auth.jwt.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.auth.jwt.service.JwtTokenService;
import org.example.sharedprompts.auth.jwt.util.JwtErrorResponseWriter;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
@Order(-100)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final TokenRedisService tokenRedisService;

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
            Claims claims = jwtTokenService.getClaims(token);

            Long userId = jwtTokenService.getUserIdFromClaims(claims);
            String role = jwtTokenService.getRoleFromClaims(claims);

            // 필수 필드 검증
            if (userId == null || role == null) {
                handleInvalidToken(token, response, "필수 클레임 누락: userId=%s, role=%s", userId, role);
                return;
            }

            // tokenVersion 검증 (soft delete, 차단 등 상태 변경 검증)
            if (!jwtTokenService.isTokenVersionValid(claims, userId)) {
                handleInvalidToken(token, response, "토큰 버전 불일치: userId=%s", userId);
                return;
            }

            PrincipalDetails principal =
                    PrincipalDetails.fromJwtClaims(
                            userId,
                            role
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handleInvalidToken(token, response, "JWT 처리 중 오류 발생: %s", e.getMessage());
        }
    }

    /**
     * 무효한 토큰 처리
     * 
     * 토큰 삭제 및 에러 응답을 일관된 방식으로 처리합니다.
     * 
     * @param token 무효한 토큰
     * @param response HTTP 응답
     * @param reason 무효화 사유 (포맷 문자열)
     * @param args reason의 인자
     * @throws IOException 응답 작성 실패 시
     */
    private void handleInvalidToken(String token, HttpServletResponse response, String reason, Object... args) throws IOException {
        log.warn("JWT 토큰 무효화: {}", String.format(reason, args));
        tokenRedisService.deleteAccessToken(token);
        JwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }
}

