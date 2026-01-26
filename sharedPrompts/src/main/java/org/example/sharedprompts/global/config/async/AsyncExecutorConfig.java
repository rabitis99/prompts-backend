package org.example.sharedprompts.global.config.async;

import org.example.sharedprompts.global.config.async.config.AsyncConfig;
import org.example.sharedprompts.global.config.async.security.SecurityContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 비동기 Executor 설정 클래스
 * 
 * <p>다양한 용도의 전용 Executor를 제공합니다:
 * <ul>
 *   <li>sseTaskExecutor: SSE 배치 전송 전용</li>
 *   <li>rateLimitLogTaskExecutor: Rate Limit 로그 기록 전용</li>
 *   <li>aiCallTaskExecutor: AI 호출 전용 (SecurityContext 전파 없음)</li>
 *   <li>aiCallTaskExecutorWithSecurityContext: AI 호출 전용 (SecurityContext 전파 포함)</li>
 * </ul>
 * 
 * <p>참고: @EnableAsync의 기본 executor는 {@link AsyncConfig#taskExecutor()}를 참조하세요.
 */
@Configuration
public class AsyncExecutorConfig {

    private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 60;

    /**
     * SSE 배치 전송 전용 Executor
     * - SecurityContext 전파 포함 (사용자별 SSE 전송 시 인증 정보 필요)
     * - taskExecutor와 동일한 설정이지만 용도가 다르므로 별도 관리
     */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        return createExecutorWithSecurityContext("sse-async-", 5, 20, 100, 
                DEFAULT_AWAIT_TERMINATION_SECONDS, createCallerThreadRejectionHandler());
    }

    /**
     * Rate Limit 로그 기록 전용 Executor
     * - SecurityContext 전파 불필요 (로그 기록만 수행)
     */
    @Bean(name = "rateLimitLogTaskExecutor")
    public Executor rateLimitLogTaskExecutor() {
        return createExecutor("rate-limit-log-async-", 3, 10, 500, 30,
                createCallerThreadRejectionHandler());
    }

    /**
     * AI 호출 전용 Executor (SecurityContext 전파 없음)
     * - 외부 API 호출만 수행하므로 SecurityContext 불필요
     */
    @Bean(name = "aiCallTaskExecutor")
    public ExecutorService aiCallTaskExecutor() {
        return createExecutorService("ai-call-async-", 10, 50, 100,
                createRejectionHandler("AI call"));
    }

    /**
     * AI 호출 전용 Executor (SecurityContext 전파 포함)
     * - SecurityContext가 필요한 AI 호출 시나리오용
     * - TaskDecorator 방식으로 통일 (DelegatingSecurityContextExecutorService 대신)
     */
    @Bean(name = "aiCallTaskExecutorWithSecurityContext")
    public ExecutorService aiCallTaskExecutorWithSecurityContext() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) createExecutorWithSecurityContext(
                "ai-call-secure-async-", 10, 50, 100, DEFAULT_AWAIT_TERMINATION_SECONDS,
                createRejectionHandler("AI call"));
        return executor.getThreadPoolExecutor();
    }

    /**
     * SecurityContext 전파 없는 Executor 생성
     */
    private Executor createExecutor(String prefix, int core, int max, int queue, int awaitSec,
                                    RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitSec);
        executor.setRejectedExecutionHandler(rejectedHandler);
        executor.initialize();
        return executor;
    }

    /**
     * SecurityContext 전파가 포함된 Executor 생성
     * - TaskDecorator 방식 사용 (일관성 유지)
     */
    private Executor createExecutorWithSecurityContext(String prefix, int core, int max, int queue, 
                                                        int awaitSec, RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitSec);
        executor.setRejectedExecutionHandler(rejectedHandler);
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    private ExecutorService createExecutorService(String prefix, int core, int max, int queue,
                                                  RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) createExecutor(
                prefix, core, max, queue, DEFAULT_AWAIT_TERMINATION_SECONDS, rejectedHandler);
        return executor.getThreadPoolExecutor();
    }

    private RejectedExecutionHandler createCallerThreadRejectionHandler() {
        return (r, executor) -> {
            if (!executor.isShutdown()) {
                r.run();
            }
        };
    }

    private RejectedExecutionHandler createRejectionHandler(String executorName) {
        return (r, executor) -> {
            throw new RejectedExecutionException(
                    executorName + " executor pool saturated. Circuit Breaker will handle fallback.");
        };
    }

}
