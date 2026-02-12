package org.example.sharedprompts.auth.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청에서 Tenant ID를 추출하여 TenantContext ThreadLocal에 설정하는 필터
 *
 * <p>JWT 인증 후에 실행되어 인증된 요청에서 X-Tenant-Id 헤더를 추출합니다.
 * 요청 처리가 완료되면 ThreadLocal을 정리하여 메모리 누수를 방지합니다.
 *
 * <p>필터 순서: @Order(-40)로 설정되어 UserRateLimitFilter(-50) 다음에 실행됩니다.
 */
@Component
@Order(-40)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setCurrentTenantId(tenantId.trim());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
