package org.example.sharedprompts.global.config.async.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.config.async.*;
import org.example.sharedprompts.global.config.async.metrics.AsyncMetricsService;
import org.example.sharedprompts.global.config.async.util.CriticalMethodChecker;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * @EnableAsync의 기본 Executor 설정
 * Executor 정의는 AsyncExecutorConfig에서 관리하고, 여기서는 선택만 함
 */
@RequiredArgsConstructor
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Qualifier("taskExecutor")
    private final Executor taskExecutor;
    private final AsyncMetricsService asyncMetricsService;
    private final AsyncExceptionNotifier asyncExceptionNotifier;
    private final CriticalMethodChecker criticalMethodChecker;

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


