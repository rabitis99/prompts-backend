package org.example.sharedprompts.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;

@Configuration
public class AsyncExecutorConfig {

    private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 60;

    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        return createExecutor("sse-async-", 5, 20, 100, DEFAULT_AWAIT_TERMINATION_SECONDS,
                createCallerThreadRejectionHandler("SSE"));
    }

    @Bean(name = "rateLimitLogTaskExecutor")
    public Executor rateLimitLogTaskExecutor() {
        return createExecutor("rate-limit-log-async-", 3, 10, 500, 30,
                createCallerThreadRejectionHandler("RateLimit log"));
    }

    @Bean(name = "aiCallTaskExecutor")
    public ExecutorService aiCallTaskExecutor() {
        return createExecutorService("ai-call-async-", 10, 50, 100,
                createRejectionHandler("AI call"));
    }

    @Bean(name = "aiCallTaskExecutorWithSecurityContext")
    public ExecutorService aiCallTaskExecutorWithSecurityContext() {
        ExecutorService delegate = createExecutorService("ai-call-secure-async-", 10, 50, 100,
                createRejectionHandler("AI call"));
        return new DelegatingSecurityContextExecutorService(delegate);
    }

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

    private ExecutorService createExecutorService(String prefix, int core, int max, int queue,
                                                  RejectedExecutionHandler rejectedHandler) {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) createExecutor(
                prefix, core, max, queue, DEFAULT_AWAIT_TERMINATION_SECONDS, rejectedHandler);
        return executor.getThreadPoolExecutor();
    }

    private RejectedExecutionHandler createCallerThreadRejectionHandler(String executorName) {
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
