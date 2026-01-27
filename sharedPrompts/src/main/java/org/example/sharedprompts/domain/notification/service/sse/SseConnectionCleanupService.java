package org.example.sharedprompts.domain.notification.service.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * SSE 연결 정리 서비스
 * - 메모리와 Redis 양방향 동기화
 * - 주기적 비활성 연결 정리
 * - 서버 시작/종료 시 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseConnectionCleanupService {

    private final SseConnectionRedisService redisService;
    private final NotificationSseService sseService;

    @PostConstruct
    public void init() {
        // 서버 시작 시 Redis에 남아있는 모든 SSE 연결 키 제거
        // 서버 재시작 시 stale 키 정리로 불일치 방지
        try {
            int deletedCount = redisService.deleteAllConnections();
            if (deletedCount > 0) {
                log.info("Cleaned up {} stale SSE connection keys on startup", deletedCount);
            }
        } catch (Exception e) {
            log.warn("Failed to clean up stale SSE connection keys on startup: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        // 모든 연결 정리
        sseService.cleanupAllConnections();

        // Redis 키도 정리
        try {
            int deletedCount = redisService.deleteAllConnections();
            if (deletedCount > 0) {
                log.info("Cleaned up {} SSE connection keys on shutdown", deletedCount);
            }
        } catch (Exception e) {
            log.warn("Failed to clean up SSE connection keys on shutdown: {}", e.getMessage(), e);
        }
    }

    /**
     * 주기적으로 비활성 SSE 연결 정리
     * - 메모리 누수 방지
     * - 메모리와 Redis 양방향 동기화
     * - 설정된 간격마다 실행
     */
    @Scheduled(fixedDelayString = "${notification.sse.cleanup-interval:300000}")
    public void cleanupInactiveConnections() {
        Map<Long, SseEmitter> emitters = sseService.getEmitters();
        int cleanedCount = 0;
        Iterator<Map.Entry<Long, SseEmitter>> iterator = emitters.entrySet().iterator();

        // 1. 메모리에 있지만 Redis에 없는 연결 정리
        cleanedCount += cleanupMemoryOnlyConnections(iterator, emitters);

        // 2. Redis에만 남아있는 키 정리 (서버 재시작 등으로 메모리에는 없지만 Redis에는 남아있는 경우)
        cleanedCount += cleanupRedisOnlyKeys(emitters);

        if (cleanedCount > 0) {
            log.info("Cleaned up {} inactive SSE connections (memory: {}, redis: {})",
                    cleanedCount, emitters.size(), redisService.getActiveConnectionCount());
        }
    }

    /**
     * 메모리에만 있는 연결 정리
     * 
     * @param iterator emitters iterator
     * @param emitters emitters map
     * @return 정리된 연결 수
     */
    private int cleanupMemoryOnlyConnections(Iterator<Map.Entry<Long, SseEmitter>> iterator, Map<Long, SseEmitter> emitters) {
        int cleanedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<Long, SseEmitter> entry = iterator.next();
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                if (!redisService.hasConnection(userId)) {
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

        return cleanedCount;
    }

    /**
     * Redis에만 남아있는 키 정리
     * 
     * @param emitters emitters map
     * @return 정리된 키 수
     */
    private int cleanupRedisOnlyKeys(Map<Long, SseEmitter> emitters) {
        int cleanedCount = 0;

        try {
            Set<String> redisKeys = redisService.getAllConnectionKeys();
            if (redisKeys != null && !redisKeys.isEmpty()) {
                for (String key : redisKeys) {
                    Long userId = redisService.extractUserIdFromKey(key);
                    if (userId == null) {
                        // 잘못된 형식의 키는 삭제 (userId가 null이므로 직접 키 삭제)
                        redisService.deleteKey(key);
                        cleanedCount++;
                        continue;
                    }

                    // 메모리에 해당 사용자의 연결이 없으면 Redis 키도 삭제
                    if (!emitters.containsKey(userId)) {
                        redisService.deleteConnection(userId);
                        cleanedCount++;
                        log.debug("Removed orphaned Redis key for user: {}", userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error cleaning up orphaned Redis keys: {}", e.getMessage(), e);
        }

        return cleanedCount;
    }
}

