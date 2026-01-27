// AsyncExecutorConfig.java
package org.example.sharedprompts.global.config.async;

import org.example.sharedprompts.global.config.async.security.SecurityContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 다양한 용도의 전용 Executor 설정
 */
@Configuration
public class AsyncExecutorConfig {

    private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 60;

    /** SSE 배치 전송 전용 Executor */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        return createExecutorWithSecurityContext("sse-async-", 5, 20, 100,
                DEFAULT_AWAIT_TERMINATION_SECONDS, createCallerThreadRejectionHandler());
    }

    /** Rate Limit 로그 기록 전용 Executor */
    @Bean(name = "rateLimitLogTaskExecutor")
    public Executor rateLimitLogTaskExecutor() {
        return createExecutor("rate-limit-log-async-", 3, 10, 500, 30,
                createCallerThreadRejectionHandler());
    }

    /** AI 호출 전용 Executor (SecurityContext 없음) */
    @Bean(name = "aiCallTaskExecutor")
    public ExecutorService aiCallTaskExecutor() {
        return createExecutorService("ai-call-async-", 10, 50, 100,
                createRejectionHandler("AI call"));
    }

    /** AI 호출 전용 Executor (SecurityContext 포함) */
    @Bean(name = "aiCallTaskExecutorWithSecurityContext")
    public ExecutorService aiCallTaskExecutorWithSecurityContext() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) createExecutorWithSecurityContext(
                "ai-call-secure-async-", 10, 50, 100, DEFAULT_AWAIT_TERMINATION_SECONDS,
                createRejectionHandler("AI call"));
        return executor.getThreadPoolExecutor();
    }

    /** 태그 카운트 업데이트 전용 Executor */
    @Bean(name = "tagCountUpdateExecutor")
    public Executor tagCountUpdateExecutor() {
        return createExecutorWithSecurityContext("tag-count-update-", 2, 5, 100,
                DEFAULT_AWAIT_TERMINATION_SECONDS, createCallerThreadRejectionHandler());
    }

    /** 공통 Executor 설정 로직 */
    private void configureExecutor(ThreadPoolTaskExecutor executor, String prefix, int core, int max, int queue,
                                   int awaitSec, RejectedExecutionHandler rejectedHandler) {
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitSec);
        executor.setRejectedExecutionHandler(rejectedHandler);
    }

    /** SecurityContext 없는 일반 Executor */
    private Executor createExecutor(String prefix, int core, int max, int queue, int awaitSec,
                                    RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        configureExecutor(executor, prefix, core, max, queue, awaitSec, rejectedHandler);
        executor.initialize();
        return executor;
    }

    /** SecurityContext 포함 Executor */
    private Executor createExecutorWithSecurityContext(String prefix, int core, int max, int queue,
                                                       int awaitSec, RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        configureExecutor(executor, prefix, core, max, queue, awaitSec, rejectedHandler);
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /** 일반 ExecutorService 생성 */
    private ExecutorService createExecutorService(String prefix, int core, int max, int queue,
                                                  RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) createExecutor(
                prefix, core, max, queue, DEFAULT_AWAIT_TERMINATION_SECONDS, rejectedHandler);
        return executor.getThreadPoolExecutor();
    }

    /** CallerRunsPolicy 적용 */
    private RejectedExecutionHandler createCallerThreadRejectionHandler() {
        return (r, executor) -> {
            if (!executor.isShutdown()) {
                r.run();
            }
        };
    }

    /** 예외 던지는 정책 (Circuit Breaker 등에서 활용 가능) */
    private RejectedExecutionHandler createRejectionHandler(String executorName) {
        return (r, executor) -> {
            throw new RejectedExecutionException(
                    executorName + " executor pool saturated. Circuit Breaker will handle fallback.");
        };
    }
}
