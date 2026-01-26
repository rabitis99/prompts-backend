package org.example.sharedprompts.global.config.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.config.async.metrics.AsyncMetricsService;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @EnableAsync의 기본 Executor 설정
 * 
 * <p>이 클래스는 @EnableAsync 어노테이션으로 활성화된
 * @Async 메서드의 기본 executor를 제공합니다.
 * 
 * <p>설정:
 * <ul>
 *   <li>Thread prefix: "async-"</li>
 *   <li>Core pool size: 5</li>
 *   <li>Max pool size: 20</li>
 *   <li>Queue capacity: 100</li>
 *   <li>Await termination: 60초</li>
 *   <li>SecurityContext 전파: 포함 (TaskDecorator 방식)</li>
 *   <li>AsyncUncaughtExceptionHandler: CustomAsyncUncaughtExceptionHandler</li>
 * </ul>
 * 
 * <p>참고: 전용 Executor가 필요한 경우 {@link AsyncExecutorConfig}를 참조하세요.
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {
    
    private final AsyncMetricsService asyncMetricsService;
    private final AsyncExceptionNotifier asyncExceptionNotifier;
    private final CriticalMethodChecker criticalMethodChecker;

    /**
     * @EnableAsync의 기본 Executor
     * - @Async 어노테이션이 명시된 메서드에서 사용
     * - SecurityContext 자동 전파 포함
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("async-");
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }
    
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncUncaughtExceptionHandler(
            asyncMetricsService, 
            asyncExceptionNotifier,
            criticalMethodChecker
        );
    }
}
