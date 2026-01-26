package org.example.sharedprompts.auth.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.auth.security.constant.SecurityConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 비동기 요청 처리 시 SecurityContext를 복원하는 필터
 * 
 * <p>비동기 디스패치(ASYNC dispatch) 단계에서 SecurityContext가 손실되는 것을 방지하기 위해
 * HttpServletRequest에 저장된 SecurityContext를 복원합니다.
 * 
 * <p>SecurityConfig의 HttpServletRequestAttributeSecurityContextRepository와
 * WebAsyncSecurityConfig의 인터셉터와 협력하여 SecurityContext를 유지합니다.
 * 
 * <p>필터 순서: @Order(-300)로 설정되어 가장 먼저 실행됩니다.
 */
@Component
@Order(-300)
public class AsyncSecurityContextRestoreFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // 비동기 디스패치에서도 필터를 실행하여 SecurityContext를 복원합니다
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        // HttpServletRequest에 저장된 SecurityContext를 복원
        SecurityContext savedContext = (SecurityContext) request.getAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME);

        if (savedContext != null) {
            SecurityContextHolder.setContext(savedContext);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 필터 체인 실행 중 SecurityContext가 clear되었을 수 있으므로
            // 저장된 SecurityContext가 있고 현재 SecurityContext에 인증 정보가 없다면 복원
            // (비동기 응답 처리 시 SecurityContext가 필요할 수 있음)
            if (savedContext != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.setContext(savedContext);
            }
        }
    }
}
