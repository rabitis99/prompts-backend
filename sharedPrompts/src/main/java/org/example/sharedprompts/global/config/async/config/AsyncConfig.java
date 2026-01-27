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
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @EnableAsync의 기본 Executor 설정
 */
@RequiredArgsConstructor
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final Executor taskExecutor; // Bean 주입
    private final AsyncMetricsService asyncMetricsService;
    private final AsyncExceptionNotifier asyncExceptionNotifier;
    private final CriticalMethodChecker criticalMethodChecker;

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
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor;
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

