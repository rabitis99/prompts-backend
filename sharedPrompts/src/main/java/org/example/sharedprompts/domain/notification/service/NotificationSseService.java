package org.example.sharedprompts.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE (Server-Sent Events)를 통한 실시간 알림 전송 서비스
 * - 로컬 메모리에 SseEmitter 저장 (HTTP 연결 객체이므로 필수)
 * - Redis Pub/Sub을 통한 분산 환경 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    private static final String SSE_CONNECTION_PREFIX = "sse:connection:";
    // 향후 Redis Pub/Sub을 통한 분산 환경 지원 시 사용 예정
    @SuppressWarnings("unused")
    private static final String SSE_PUBSUB_CHANNEL = "sse:notification";

    // 사용자 ID별로 SseEmitter를 관리 (로컬 메모리)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;

    @Value("${notification.sse.timeout:1800000}")
    private long sseTimeout;

    @PostConstruct
    public void init() {
        // Redis Pub/Sub은 필요시 구현
        // 현재는 로컬 메모리 기반으로 동작
        log.debug("NotificationSseService initialized");
    }

    @PreDestroy
    public void cleanup() {
        // 모든 연결 정리
        emitters.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Failed to complete emitter during cleanup: {}", e.getMessage());
            }
        });
        emitters.clear();
    }

    /**
     * SSE 구독
     * 
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    public SseEmitter subscribe(Long userId) {
        // 기존 연결이 있으면 제거
        SseEmitter existingEmitter = emitters.remove(userId);
        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.warn("Failed to complete existing emitter for user {}: {}", userId, e.getMessage());
            }
        }

        // 타임아웃 설정 (설정 파일에서 가져옴)
        SseEmitter emitter = new SseEmitter(sseTimeout);

        // 연결 완료 콜백
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for user: {}", userId);
            if (emitters.remove(userId, emitter)) {
                redisTemplate.delete(SSE_CONNECTION_PREFIX + userId);
            }
        });

        // 타임아웃 콜백
        emitter.onTimeout(() -> {
            log.debug("SSE connection timeout for user: {}", userId);
            if (emitters.remove(userId, emitter)) {
                redisTemplate.delete(SSE_CONNECTION_PREFIX + userId);
            }
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Failed to complete emitter on timeout for user {}: {}", userId, e.getMessage());
            }
        });

        // 에러 콜백
        emitter.onError((ex) -> {
            log.error("SSE connection error for user {}: {}", userId, ex.getMessage(), ex);
            if (emitters.remove(userId, emitter)) {
                redisTemplate.delete(SSE_CONNECTION_PREFIX + userId);
            }
            try {
                emitter.completeWithError(ex);
            } catch (Exception e) {
                log.warn("Failed to complete emitter with error for user {}: {}", userId, e.getMessage());
            }
        });

        emitters.put(userId, emitter);

        // Redis에 연결 정보 저장 (TTL: 타임아웃 시간과 동일)
        String connectionKey = SSE_CONNECTION_PREFIX + userId;
        redisTemplate.opsForValue().set(connectionKey, "connected", Duration.ofMillis(sseTimeout));

        // 연결 확인 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("알림 구독이 시작되었습니다."));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message to user {}: {}", userId, e.getMessage(), e);
            if (emitters.remove(userId, emitter)) {
                redisTemplate.delete(connectionKey);
            }
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.warn("Failed to complete emitter with error: {}", ex.getMessage());
            }
            return emitter;
        }

        log.info("SSE connection established for user: {}", userId);
        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     * 
     * @param userId 사용자 ID
     * @param notification 알림 DTO
     */
    public void sendNotification(Long userId, NotificationResponseDto notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
                log.debug("Notification sent to user {}: {}", userId, notification.getId());
            } catch (IOException e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage(), e);
                if (emitters.remove(userId, emitter)) {
                    redisTemplate.delete(SSE_CONNECTION_PREFIX + userId);
                }
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.warn("Failed to complete emitter with error: {}", ex.getMessage());
                }
            }
        } else {
            checkRedisConnection(userId);
        }
    }

    /**
     * 프롬프트 생성 SSE 알림 전송
     * 
     * @param userId 알림 대상 사용자 ID
     * @param payload 프롬프트 생성 알림 페이로드
     */
    public void sendPromptCreated(Long userId, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("prompt-created")
                        .data(payload));
                log.debug("Prompt-created SSE sent to user {}", userId);
            } catch (IOException e) {
                log.error("Failed to send prompt-created SSE to user {}: {}", userId, e.getMessage(), e);
                if (emitters.remove(userId, emitter)) {
                    redisTemplate.delete(SSE_CONNECTION_PREFIX + userId);
                }
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.warn("Failed to complete emitter with error: {}", ex.getMessage());
                }
            }
        } else {
            checkRedisConnection(userId);
        }
    }

    /**
     * Redis에서 다른 서버 인스턴스의 SSE 연결 상태 확인
     * 
     * @param userId 사용자 ID
     */
    private void checkRedisConnection(Long userId) {
        // 로컬에 연결이 없으면 Redis에서 다른 서버 인스턴스 연결 확인
        String connectionKey = SSE_CONNECTION_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(connectionKey))) {
            log.debug("SSE connection exists on another server instance for user: {}", userId);
            // 향후 Redis Pub/Sub을 통한 분산 환경 지원 가능
        } else {
            log.debug("No active SSE connection for user: {}", userId);
        }
    }

    /**
     * 특정 사용자의 SSE 연결 종료
     * 
     * @param userId 사용자 ID
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null && emitters.remove(userId, emitter)) {
            try {
                emitter.complete();
                redisTemplate.delete(SSE_CONNECTION_PREFIX + userId);
                log.info("SSE connection closed for user: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to complete emitter for user {}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * 활성 연결 수 조회
     * 
     * @return 활성 연결 수
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * 주기적으로 비활성 SSE 연결 정리
     * - 메모리 누수 방지
     * - 설정된 간격마다 실행
     */
    @Scheduled(fixedDelayString = "${notification.sse.cleanup-interval:300000}")
    public void cleanupInactiveConnections() {
        int cleanedCount = 0;
        Iterator<Map.Entry<Long, SseEmitter>> iterator = emitters.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, SseEmitter> entry = iterator.next();
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                // 연결이 이미 완료되었거나 타임아웃된 경우 정리
                // SseEmitter의 상태를 직접 확인할 수 없으므로, Redis 키 존재 여부로 확인
                String connectionKey = SSE_CONNECTION_PREFIX + userId;
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(connectionKey))) {
                    // Redis에 키가 없으면 연결이 끊어진 것으로 간주
                    iterator.remove();
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.debug("Emitter already completed for user: {}", userId);
                    }
                    cleanedCount++;
                }
            } catch (Exception e) {
                log.warn("Error during SSE connection cleanup for user {}: {}", userId, e.getMessage());
                // 에러 발생 시에도 제거하여 메모리 누수 방지
                iterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("Cleaned up {} inactive SSE connections", cleanedCount);
        }
    }
}

