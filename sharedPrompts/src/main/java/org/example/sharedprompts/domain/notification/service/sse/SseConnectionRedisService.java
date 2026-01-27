package org.example.sharedprompts.domain.notification.service.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * SSE 연결 상태를 Redis에서 관리하는 서비스
 * - Redis 키 생성/삭제/조회
 * - 분산 환경에서 연결 상태 추적
 * - 인스턴스별 키 분리로 멀티 인스턴스 환경 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseConnectionRedisService {

    private static final String SSE_CONNECTION_PREFIX = "sse:connection:";
    private static final String SSE_CONNECTION_PATTERN = SSE_CONNECTION_PREFIX + "*";

    private final StringRedisTemplate redisTemplate;
    
    /**
     * 서버 인스턴스 고유 ID (서버 시작 시 생성)
     * 멀티 인스턴스 환경에서 각 인스턴스의 키를 구분하기 위해 사용
     */
    private String instanceId;

    @PostConstruct
    public void init() {
        // 서버 시작 시 고유 인스턴스 ID 생성
        // UUID를 사용하여 각 서버 인스턴스를 고유하게 식별
        instanceId = java.util.UUID.randomUUID().toString();
        log.info("SSE Connection Redis Service initialized with instance ID: {}", instanceId);
    }

    /**
     * 현재 인스턴스 ID 조회
     * 
     * @return 인스턴스 ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Redis 키 생성
     * 
     * @param userId 사용자 ID
     * @param ttlMillis TTL (밀리초)
     */
    public void saveConnection(Long userId, long ttlMillis) {
        String connectionKey = getConnectionKey(userId);
        redisTemplate.opsForValue().set(connectionKey, "connected", Duration.ofMillis(ttlMillis));
        log.debug("Saved SSE connection key for user: {} (instance: {})", userId, instanceId);
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
     * 모든 SSE 연결 키 조회 (SCAN 사용)
     * KEYS 명령어 대신 SCAN을 사용하여 Redis 서버 블로킹 방지
     * 
     * @return SSE 연결 키 Set
     */
    public Set<String> getAllConnectionKeys() {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(SSE_CONNECTION_PATTERN)
                .count(100)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    /**
     * 현재 인스턴스의 SSE 연결 키만 조회 (SCAN 사용)
     * 
     * @return 현재 인스턴스의 SSE 연결 키 Set
     */
    public Set<String> getInstanceConnectionKeys() {
        Set<String> keys = new HashSet<>();
        String instancePattern = getInstanceKeyPattern();
        ScanOptions options = ScanOptions.scanOptions()
                .match(instancePattern)
                .count(100)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    /**
     * 현재 인스턴스의 SSE 연결 키만 삭제
     * 멀티 인스턴스 환경에서 다른 인스턴스의 키를 삭제하지 않도록 보호
     * 
     * @return 삭제된 키 개수
     */
    public int deleteInstanceConnections() {
        Set<String> keys = getInstanceConnectionKeys();
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Deleted {} SSE connection keys for instance: {}", keys.size(), instanceId);
            return keys.size();
        }
        return 0;
    }

    /**
     * 모든 SSE 연결 키 삭제 (주의: 멀티 인스턴스 환경에서는 사용하지 않음)
     * 
     * @return 삭제된 키 개수
     * @deprecated 멀티 인스턴스 환경에서는 deleteInstanceConnections() 사용 권장
     */
    @Deprecated
    public int deleteAllConnections() {
        Set<String> keys = getAllConnectionKeys();
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.warn("Deleted {} SSE connection keys (all instances) - use deleteInstanceConnections() in multi-instance environments", keys.size());
            return keys.size();
        }
        return 0;
    }

    /**
     * Redis 키에서 사용자 ID 추출
     * 
     * @param key Redis 키 (형식: sse:connection:{instanceId}:{userId})
     * @return 사용자 ID (파싱 실패 시 null)
     */
    public Long extractUserIdFromKey(String key) {
        try {
            // 키 형식: sse:connection:{instanceId}:{userId}
            String withoutPrefix = key.substring(SSE_CONNECTION_PREFIX.length());
            int colonIndex = withoutPrefix.indexOf(':');
            if (colonIndex > 0 && colonIndex < withoutPrefix.length() - 1) {
                String userIdStr = withoutPrefix.substring(colonIndex + 1);
                return Long.parseLong(userIdStr);
            }
            // 구형 키 형식 지원 (인스턴스 ID 없이): sse:connection:{userId}
            return Long.parseLong(withoutPrefix);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.warn("Invalid SSE connection key format: {}", key);
            return null;
        }
    }

    /**
     * 연결 키 생성 (인스턴스 ID 포함)
     * 
     * @param userId 사용자 ID
     * @return Redis 키 (형식: sse:connection:{instanceId}:{userId})
     */
    public String getConnectionKey(Long userId) {
        return SSE_CONNECTION_PREFIX + instanceId + ":" + userId;
    }

    /**
     * 현재 인스턴스의 키 패턴 반환
     * 
     * @return 인스턴스별 키 패턴
     */
    private String getInstanceKeyPattern() {
        return SSE_CONNECTION_PREFIX + instanceId + ":*";
    }

    /**
     * Redis 키에서 인스턴스 ID 추출
     * 
     * @param key Redis 키
     * @return 인스턴스 ID (파싱 실패 시 null)
     */
    public String extractInstanceIdFromKey(String key) {
        try {
            // 키 형식: sse:connection:{instanceId}:{userId}
            String withoutPrefix = key.substring(SSE_CONNECTION_PREFIX.length());
            int colonIndex = withoutPrefix.indexOf(':');
            if (colonIndex > 0) {
                return withoutPrefix.substring(0, colonIndex);
            }
            return null;
        } catch (StringIndexOutOfBoundsException e) {
            log.warn("Invalid SSE connection key format: {}", key);
            return null;
        }
    }

    /**
     * 활성 연결 수 조회 (모든 인스턴스)
     * 
     * @return 활성 연결 수
     */
    public int getActiveConnectionCount() {
        Set<String> keys = getAllConnectionKeys();
        return keys != null ? keys.size() : 0;
    }

    /**
     * 현재 인스턴스의 활성 연결 수 조회
     * 
     * @return 현재 인스턴스의 활성 연결 수
     */
    public int getInstanceActiveConnectionCount() {
        Set<String> keys = getInstanceConnectionKeys();
        return keys != null ? keys.size() : 0;
    }
}

