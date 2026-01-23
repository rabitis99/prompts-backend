package org.example.sharedprompts.auth.security.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.security.handler.AccessDeniedHandlerImpl;
import org.example.sharedprompts.auth.security.handler.JwtAuthenticationEntryPoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * Spring Security 예외 처리 설정
 *
 * <p>인증/인가 실패 시 처리할 핸들러를 설정합니다:
 * <ul>
 *   <li>AuthenticationEntryPoint: 인증 실패 시 처리 (401 Unauthorized)</li>
 *   <li>AccessDeniedHandler: 인가 실패 시 처리 (403 Forbidden)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandlers {

    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 예외 처리 핸들러 설정
     *
     * @param http HttpSecurity 객체
     * @throws Exception 설정 중 오류 발생 시
     */
    public void configure(HttpSecurity http) throws Exception {
        http.exceptionHandling(exception ->
                exception
                        // 인증 실패 시 처리 (401 Unauthorized)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 인가 실패 시 처리 (403 Forbidden)
                        .accessDeniedHandler(accessDeniedHandler)
        );
    }
}

