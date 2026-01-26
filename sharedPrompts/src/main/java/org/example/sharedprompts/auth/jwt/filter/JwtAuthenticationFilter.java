package org.example.sharedprompts.auth.jwt.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.auth.jwt.service.JwtTokenService;
import org.example.sharedprompts.auth.jwt.util.JwtErrorResponseWriter;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.util.SensitiveDataMasker;

/**
 * JWT 인증 필터
 * 
 * 2단계 토큰 검증 전략:
 * 1단계 (필수): JWT 서명 검증, 만료 시간 검증, Claim 파싱, tokenVersion 검증
 * 2단계 (선택적): Redis 토큰 검증 (Redis 장애 시 Fail-Open 정책 적용)
 * 
 * 책임:
 * - 토큰 서명 검증
 * - 만료 시간 검증
 * - Claim 파싱
 * - tokenVersion 검증 (soft delete, 차단 등 상태 변경 검증)
 * - Redis 토큰 검증 (Redis 장애 시 무시)
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
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

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

        // ==================== 1단계: JWT 서명 및 만료 시간 검증 (필수) ====================
        Claims claims;
        try {
            claims = jwtTokenService.getClaims(token);
        } catch (Exception e) {
            // JWT 서명 검증 실패 또는 만료된 토큰
            log.warn("JWT 서명 검증 실패 또는 만료된 토큰: {}", e.getMessage());
            handleInvalidToken(token, response, "JWT 서명 검증 실패 또는 만료된 토큰: %s", e.getMessage());
            return;
        }

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

        // ==================== 2단계: Redis 토큰 검증 (선택적, Fail-Open) ====================
        // Redis는 선택 검증 단계로 처리: Redis 장애 시에도 JWT 서명 검증 통과 시 인증 허용
        // TokenRedisService의 Circuit Breaker fallback이 Fail-Open 정책을 적용하므로
        // 예외가 발생하지 않고 true를 반환합니다.
        boolean isRedisTokenValid = tokenRedisService.isAccessTokenValid(token);

        if (!isRedisTokenValid) {
            // Redis에서 토큰이 없거나 만료된 경우 (Redis가 정상 동작 중일 때만)
            // Circuit Breaker가 Open 상태이거나 Redis 장애 시에는 fallback에서 true를 반환하므로
            // 이 분기는 Redis가 정상 동작 중이고 토큰이 실제로 없거나 만료된 경우에만 실행됩니다.
            log.warn("Redis 토큰 검증 실패: userId={}, token={}, 토큰이 Redis에 없거나 만료됨", 
                    userId, SensitiveDataMasker.maskToken(token));
            handleInvalidToken(token, response, "Redis 토큰 검증 실패: userId=%s", userId);
            return;
        }

        // ==================== 인증 성공 ====================
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
        log.warn("JWT 토큰 무효화: token={}, reason={}", 
                SensitiveDataMasker.maskToken(token), String.format(reason, args));
        // Redis 삭제는 실패해도 무시 (Fail-Open 정책)
        try {
            tokenRedisService.deleteAccessToken(token);
        } catch (Exception e) {
            log.debug("Redis 토큰 삭제 실패 (무시): token={}", SensitiveDataMasker.maskToken(token));
        }
        JwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }
}

