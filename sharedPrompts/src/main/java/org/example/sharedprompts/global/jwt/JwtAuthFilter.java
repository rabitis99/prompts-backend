package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (tokenRedisService.isAccessTokenValid(token)) {
                try {
                    Claims claims = jwtProvider.getClaims(token);

                    Long userId = jwtProvider.getUserIdFromClaims(claims);
                    String role = jwtProvider.getRoleFromClaims(claims);
                    String nickname = jwtProvider.getNicknameFromClaims(claims);
                    String provider = jwtProvider.getProviderFromClaims(claims);
                    String providerId = jwtProvider.getProviderIdFromClaims(claims);

                    if (userId != null && role != null) {
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
                    } else {
                        tokenRedisService.deleteAccessToken(token);
                    }
                } catch (Exception e) {
                    log.warn("JWT 처리 중 오류 발생: {}", e.getMessage());
                    tokenRedisService.deleteAccessToken(token);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
