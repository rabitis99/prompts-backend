package org.example.sharedprompts.auth.jwt.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.auth.jwt.service.JwtTokenValidationService;

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

    private final JwtTokenValidationService jwtTokenValidationService;

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

        // JWT 토큰 검증
        JwtTokenValidationService.ValidationResult validationResult = jwtTokenValidationService.validateToken(token);

        if (!validationResult.isValid()) {
            jwtTokenValidationService.handleInvalidToken(token, response, validationResult.getErrorMessage());
            return;
        }

        // ==================== 인증 성공 ====================
        PrincipalDetails principal =
                PrincipalDetails.fromJwtClaims(
                        validationResult.getUserId(),
                        validationResult.getRole()
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

}