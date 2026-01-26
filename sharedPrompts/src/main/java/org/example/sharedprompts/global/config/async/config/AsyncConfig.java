package org.example.sharedprompts.global.config.async.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.config.async.*;
import org.example.sharedprompts.global.config.async.metrics.AsyncMetricsService;
import org.example.sharedprompts.global.config.async.security.SecurityContextTaskDecorator;
import org.example.sharedprompts.global.config.async.util.CriticalMethodChecker;
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
     * Defines the default Executor for @Async-annotated methods and propagates the SecurityContext to async tasks.
     *
     * @return the configured ThreadPoolTaskExecutor used for asynchronous method execution
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
    
    /**
     * Provides the Executor that Spring will use for executing methods annotated with @Async.
     *
     * @return the Executor used to execute @Async-annotated methods
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
    
    /**
     * Provide the handler for uncaught exceptions thrown by @Async methods with a void return type.
     *
     * @return the AsyncUncaughtExceptionHandler that records async metrics and notifies on uncaught exceptions from @Async void methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncUncaughtExceptionHandler(
            asyncMetricsService, 
            asyncExceptionNotifier,
            criticalMethodChecker
        );
    }
}