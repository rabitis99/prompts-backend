package org.example.sharedprompts.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 작업 처리를 위한 설정
 * - SSE 전송 등 긴 작업을 별도 스레드 풀에서 처리
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * SSE 전송용 TaskExecutor
     * - 여러 사용자에게 동시에 SSE 알림 전송 시 사용
     * - 스레드 풀 포화 시 호출 스레드에서 실행하여 알림 유실 방지
     */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(createSseRejectedExecutionHandler());
        executor.initialize();
        return executor;
    }

    /**
     * Rate Limit 로그 저장용 TaskExecutor
     * - Rate Limit 로그를 비동기로 저장하여 요청 응답 시간에 영향 없도록 함
     * - 풀 포화 시 호출 스레드에서 실행하여 로그 유실 방지
     */
    @Bean(name = "rateLimitLogTaskExecutor")
    public Executor rateLimitLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("rate-limit-log-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(createRateLimitRejectedHandler());
        executor.initialize();
        return executor;
    }

    /**
     * SSE TaskExecutor용 거부 정책
     * - 로깅 후 호출 스레드에서 실행하여 알림 유실 방지
     */
    private RejectedExecutionHandler createSseRejectedExecutionHandler() {
        return (r, executor) -> {
            log.warn(
                    "SSE task executor pool is saturated. Active threads: {}, Queue size: {}, Pool size: {}. " +
                    "Executing task in caller thread to prevent notification loss.",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getPoolSize()
            );

            if (!executor.isShutdown()) {
                r.run();
            } else {
                log.error("SSE task rejected because executor is shutdown");
            }
        };
    }

    /**
     * Rate Limit 로그 TaskExecutor용 거부 정책
     * - 로깅 후 호출 스레드에서 실행하여 로그 유실 방지
     */
    private RejectedExecutionHandler createRateLimitRejectedHandler() {
        return (r, executor) -> {
            log.warn(
                    "Rate limit log task executor pool is saturated. Active threads: {}, Queue size: {}, Pool size: {}. " +
                    "Executing task in caller thread to prevent log loss.",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getPoolSize()
            );

            if (!executor.isShutdown()) {
                r.run();
            } else {
                log.error("Rate limit log task rejected because executor is shutdown");
            }
        };
    }
}
