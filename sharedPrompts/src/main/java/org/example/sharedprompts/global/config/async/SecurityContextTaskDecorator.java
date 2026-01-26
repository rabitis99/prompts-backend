package org.example.sharedprompts.global.config.async;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext를 비동기 작업에 전파하는 TaskDecorator
 * 
 * <p>비동기 Executor에서 SecurityContext를 자동으로 전파하기 위해 사용됩니다.
 * 이 클래스는 모든 비동기 Executor 설정에서 일관되게 사용되어
 * SecurityContext 전파 방식을 통일합니다.
 * 
 * <p>사용 예시:
 * <ul>
 *   <li>AsyncConfig.taskExecutor: @EnableAsync의 기본 executor</li>
 *   <li>AsyncExecutorConfig.sseTaskExecutor: SSE 배치 전송 전용</li>
 *   <li>AsyncExecutorConfig.aiCallTaskExecutorWithSecurityContext: AI 호출 전용 (SecurityContext 필요)</li>
 * </ul>
 */
public class SecurityContextTaskDecorator implements TaskDecorator {
    
    @NotNull
    @Override
    public Runnable decorate(@NotNull Runnable runnable) {
        SecurityContext context = SecurityContextHolder.getContext();
        return () -> {
            SecurityContext previousContext = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(context);
                runnable.run();
            } finally {
                SecurityContextHolder.setContext(previousContext);
            }
        };
    }
}

