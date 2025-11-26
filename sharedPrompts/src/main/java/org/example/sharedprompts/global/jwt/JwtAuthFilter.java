package org.example.sharedprompts.global.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            // Redis에서 AccessToken 확인
            if (tokenRedisService.isAccessTokenValid(token)) {
                Claims claims = jwtProvider.getClaims(token);

                Long userId = jwtProvider.getUserIdFromClaims(claims);
                String email = jwtProvider.getEmailFromClaims(claims);
                String role = jwtProvider.getRoleFromClaims(claims);
                String nickname = jwtProvider.getNicknameFromClaims(claims); // optional

                // PrincipalDetails 생성
                PrincipalDetails principal = PrincipalDetails.fromJwtClaims(userId, email, nickname, role);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
