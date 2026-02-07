package org.example.sharedprompts.scheduler.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.notification.DiscordNotificationService;
import org.example.sharedprompts.global.redis.RedisHealthService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthCheckScheduler {

    private final RedisHealthService redisHealthService;
    private final DiscordNotificationService discordNotificationService;

    private final AtomicBoolean previousHealthyStatus = new AtomicBoolean(true);
    private final AtomicLong lastNotificationTime = new AtomicLong(0);
    private static final long NOTIFICATION_COOLDOWN_MS = 60_000;
    private final AtomicLong downSinceTime = new AtomicLong(0);

    @Scheduled(
            fixedDelayString = "${redis.health-check.fixed-delay:5000}",
            initialDelayString = "${redis.health-check.initial-delay:10000}"
    )
    public void performHealthCheck() {
        try {
            boolean isHealthy = redisHealthService.isRedisHealthy();
            boolean previousHealthy = previousHealthyStatus.get();

            if (!isHealthy) {
                if (previousHealthy) {
                    downSinceTime.set(System.currentTimeMillis());
                    sendRedisDownNotification();
                }
            } else {
                if (!previousHealthy) {
                    sendRedisRecoveryNotification();
                }
            }

            previousHealthyStatus.set(isHealthy);
        } catch (Exception e) {
            log.error("Redis Health Check 예외 발생", e);

            if (previousHealthyStatus.get()) {
                downSinceTime.set(System.currentTimeMillis());
                sendRedisDownNotification();
                previousHealthyStatus.set(false);
            }
        }
    }

    private void sendRedisDownNotification() {
        long now = System.currentTimeMillis();
        long last = lastNotificationTime.get();

        if (last > 0 && now - last < NOTIFICATION_COOLDOWN_MS) {
            return;
        }

        try {
            redisHealthService.reportFailure();
            long consecutiveFailures = redisHealthService.getConsecutiveFailures();
            long failureCount = Math.max(1, consecutiveFailures);
            
            discordNotificationService.sendRedisDownNotification(
                    "Redis 서버에 연결할 수 없습니다. Health Check 실패.",
                    failureCount
            );
            lastNotificationTime.set(now);
            log.info("Redis 장애 알림 전송 (실패 횟수: {})", failureCount);
        } catch (Exception e) {
            log.error("Redis 장애 알림 전송 실패", e);
        }
    }

    private void sendRedisRecoveryNotification() {
        try {
            long downSince = downSinceTime.get();
            long downtimeDuration = 0;
            if (downSince > 0) {
                downtimeDuration = System.currentTimeMillis() - downSince;
                downSinceTime.set(0);
            }
            
            discordNotificationService.sendRedisRecoveryNotification(downtimeDuration);
            lastNotificationTime.set(0);
            log.info("Redis 복구 알림 전송 (장애 지속 시간: {}ms)", downtimeDuration);
        } catch (Exception e) {
            log.error("Redis 복구 알림 전송 실패", e);
        }
    }
}
