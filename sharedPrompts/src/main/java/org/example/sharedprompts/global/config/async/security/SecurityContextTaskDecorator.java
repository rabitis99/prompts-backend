package org.example.sharedprompts.global.config.async.security;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext와 TenantContext를 비동기 작업에 전파하는 TaskDecorator
 *
 * <p>비동기 Executor에서 SecurityContext와 TenantContext를 자동으로 전파하기 위해 사용됩니다.
 * 이 클래스는 모든 비동기 Executor 설정에서 일관되게 사용되어
 * 컨텍스트 전파 방식을 통일합니다.
 *
 * <p>사용 예시:
 * <ul>
 *   <li>AsyncConfig.taskExecutor: @EnableAsync의 기본 executor</li>
 *   <li>AsyncExecutorConfig.sseTaskExecutor: SSE 배치 전송 전용</li>
 *   <li>AsyncExecutorConfig.aiCallTaskExecutorWithSecurityContext: AI 호출 전용 (SecurityContext 필요)</li>
 * </ul>
 */
@Slf4j
public class SecurityContextTaskDecorator implements TaskDecorator {

    @NotNull
    @Override
    public Runnable decorate(@NotNull Runnable runnable) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String tenantId = TenantContext.getCurrentTenantId();
        
        // Log captured context - use INFO level if tenantId is null to help diagnose issues
        if (tenantId != null) {
            if (log.isDebugEnabled()) {
                log.debug("SecurityContextTaskDecorator: Capturing context - thread: {}, tenantId: {}", 
                        Thread.currentThread().getName(), tenantId);
            }
        } else {
            // Warn if tenant context is null when it should be set (e.g., for job processing)
            log.warn("SecurityContextTaskDecorator: Tenant context is null when capturing - thread: {}. " +
                    "This may cause issues if the async task requires tenant context.", 
                    Thread.currentThread().getName());
        }
        
        return () -> {
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
            String previousTenantId = TenantContext.getCurrentTenantId();
            try {
                SecurityContextHolder.setContext(securityContext);
                if (tenantId != null) {
                    TenantContext.setCurrentTenantId(tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug("SecurityContextTaskDecorator: Setting tenant context in async thread - thread: {}, tenantId: {}", 
                                Thread.currentThread().getName(), tenantId);
                    }
                } else {
                    TenantContext.clear();
                    if (log.isDebugEnabled()) {
                        log.debug("SecurityContextTaskDecorator: Clearing tenant context in async thread - thread: {}", 
                                Thread.currentThread().getName());
                    }
                }
                runnable.run();
            } finally {
                SecurityContextHolder.setContext(previousSecurityContext);
                if (previousTenantId != null) {
                    TenantContext.setCurrentTenantId(previousTenantId);
                } else {
                    TenantContext.clear();
                }
            }
        };
    }
}

