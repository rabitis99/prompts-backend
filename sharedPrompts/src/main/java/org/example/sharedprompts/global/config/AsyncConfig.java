package org.example.sharedprompts.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 비동기 작업 처리를 위한 설정
 * 
 * 스레드 풀 분리 전략:
 * - SSE 전송: 별도 스레드 풀 (sseTaskExecutor)
 * - Rate Limit 로그: 별도 스레드 풀 (rateLimitLogTaskExecutor)
 * - AI 호출: 별도 스레드 풀 (aiCallTaskExecutor) - Tomcat 스레드 풀 보호
 * 
 * 운영 원칙:
 * - 외부 API 호출은 Tomcat 요청 스레드를 절대 block 하지 않음
 * - 각 작업 유형별로 독립적인 스레드 풀 사용으로 격리 보장
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
     * AI 호출 전용 TaskExecutor
     * 
     * 목적:
     * - 외부 AI API 호출을 별도 스레드 풀에서 실행하여 Tomcat 요청 스레드 보호
     * - AI 호출 지연/타임아웃이 다른 요청 처리에 영향을 주지 않도록 격리
     * 
     * 운영 원칙:
     * - AI 호출은 최대 60초까지 소요될 수 있으므로 충분한 스레드 수 확보
     * - 풀 포화 시 작업을 즉시 거부하여 Tomcat 스레드 풀 보호
     * - AI 호출 실패는 Circuit Breaker로 격리되지만, 스레드 풀은 독립적으로 관리
     * 
     * 설정 근거:
     * - CorePoolSize: 10 (동시 AI 호출 기본 처리량)
     * - MaxPoolSize: 50 (피크 트래픽 대응)
     * - QueueCapacity: 100 (대기 중인 AI 호출 수 제한)
     * - 타임아웃: 60초 (GoogleGeminiProperties.timeoutSeconds + 여유)
     */
    /**
     * AI 호출 전용 TaskExecutor
     * 
     * 반환 타입: ExecutorService
     * - ExecutorService는 Executor를 상속하므로 기존 코드와 호환됨
     * - Future.cancel() 등 ExecutorService의 고급 기능 사용 가능
     */
    @Bean(name = "aiCallTaskExecutor")
    public ExecutorService aiCallTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-call-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(createAiCallRejectedHandler());
        executor.initialize();
        return executor.getThreadPoolExecutor();
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

    /**
     * AI 호출 TaskExecutor용 거부 정책
     * 
     * 운영 전략:
     * - AI 호출은 긴 작업이므로 호출 스레드에서 실행하면 Tomcat 스레드 풀 고갈 위험
     * - 풀 포화 시 예외를 던져서 요청을 거부하고, Circuit Breaker가 처리하도록 함
     * - AI 호출 실패는 Fallback으로 처리되므로, 스레드 풀 보호가 우선
     * 
     * 장애 시나리오:
     * - AI 호출이 급증하여 스레드 풀 포화
     * - 새로운 AI 호출은 즉시 거부 (예외 발생)
     * - Circuit Breaker가 Fallback 메시지 반환
     * - Tomcat 스레드 풀은 안전하게 유지됨
     */
    private RejectedExecutionHandler createAiCallRejectedHandler() {
        return (r, executor) -> {
            log.error(
                    "AI call task executor pool is saturated. Active threads: {}, Queue size: {}, Pool size: {}. " +
                    "Rejecting task to protect Tomcat thread pool. Circuit Breaker will handle fallback.",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getPoolSize()
            );

            if (executor.isShutdown()) {
                log.error("AI call task rejected because executor is shutdown");
            }
            
            // AI 호출은 긴 작업이므로 호출 스레드에서 실행하지 않음
            // 예외를 던져서 Circuit Breaker가 Fallback 처리하도록 함
            throw new java.util.concurrent.RejectedExecutionException(
                    "AI call task executor pool is saturated. Circuit Breaker will provide fallback."
            );
        };
    }
}
