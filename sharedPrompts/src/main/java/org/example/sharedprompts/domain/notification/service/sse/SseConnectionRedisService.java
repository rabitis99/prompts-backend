package org.example.sharedprompts.domain.notification.service.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * SSE 연결 상태를 Redis에서 관리하는 서비스
 * - Redis 키 생성/삭제/조회
 * - 분산 환경에서 연결 상태 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseConnectionRedisService {

    private static final String SSE_CONNECTION_PREFIX = "sse:connection:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Redis 키 생성
     * 
     * @param userId 사용자 ID
     * @param ttlMillis TTL (밀리초)
     */
    public void saveConnection(Long userId, long ttlMillis) {
        String connectionKey = getConnectionKey(userId);
        redisTemplate.opsForValue().set(connectionKey, "connected", Duration.ofMillis(ttlMillis));
        log.debug("Saved SSE connection key for user: {}", userId);
    }

    /**
     * Redis 키 삭제
     * 
     * @param userId 사용자 ID
     */
    public void deleteConnection(Long userId) {
        String connectionKey = getConnectionKey(userId);
        redisTemplate.delete(connectionKey);
        log.debug("Deleted SSE connection key for user: {}", userId);
    }

    /**
     * Redis 키 직접 삭제 (키 문자열로)
     * 
     * @param key Redis 키
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key);
        log.debug("Deleted SSE connection key: {}", key);
    }

    /**
     * Redis 키 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @return 키 존재 여부
     */
    public boolean hasConnection(Long userId) {
        String connectionKey = getConnectionKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(connectionKey));
    }

    /**
     * 모든 SSE 연결 키 조회
     * 
     * @return SSE 연결 키 Set
     */
    public Set<String> getAllConnectionKeys() {
        return redisTemplate.keys(SSE_CONNECTION_PREFIX + "*");
    }

    /**
     * 모든 SSE 연결 키 삭제
     * 
     * @return 삭제된 키 개수
     */
    public int deleteAllConnections() {
        Set<String> keys = getAllConnectionKeys();
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Deleted {} SSE connection keys", keys.size());
            return keys.size();
        }
        return 0;
    }

    /**
     * Redis 키에서 사용자 ID 추출
     * 
     * @param key Redis 키
     * @return 사용자 ID (파싱 실패 시 null)
     */
    public Long extractUserIdFromKey(String key) {
        try {
            String userIdStr = key.substring(SSE_CONNECTION_PREFIX.length());
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.warn("Invalid SSE connection key format: {}", key);
            return null;
        }
    }

    /**
     * 연결 키 생성
     * 
     * @param userId 사용자 ID
     * @return Redis 키
     */
    public String getConnectionKey(Long userId) {
        return SSE_CONNECTION_PREFIX + userId;
    }

    /**
     * 활성 연결 수 조회
     * 
     * @return 활성 연결 수
     */
    public int getActiveConnectionCount() {
        Set<String> keys = getAllConnectionKeys();
        return keys != null ? keys.size() : 0;
    }
}

