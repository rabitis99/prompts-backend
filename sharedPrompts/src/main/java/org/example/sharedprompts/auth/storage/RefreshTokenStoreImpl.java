package org.example.sharedprompts.auth.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Refresh Token 저장소 구현체
 * 
 * Redis 기반으로 Refresh Token을 저장하고 관리합니다.
 * 1회용 보장을 위해 getAndDelete 메서드를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreImpl implements RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlProperties;
    
    private final DefaultRedisScript<List<String>> getAndDeleteScript = createGetAndDeleteScript();
    private final DefaultRedisScript<Long> deleteScript = createDeleteScript();
    private final DefaultRedisScript<Long> deleteAllByUserScript = createDeleteAllByUserScript();
    
    private static DefaultRedisScript<List<String>> createGetAndDeleteScript() {
        DefaultRedisScript<List<String>> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.GET_AND_DELETE_REFRESH_TOKEN);
        // Spring Data Redis는 런타임에 제네릭 타입 정보를 잃어버리므로 raw type을 사용
        @SuppressWarnings("unchecked")
        Class<List<String>> resultType = (Class<List<String>>) (Class<?>) List.class;
        script.setResultType(resultType);
        return script;
    }
    
    private static DefaultRedisScript<Long> createDeleteScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.DELETE_REFRESH_TOKEN);
        script.setResultType(Long.class);
        return script;
    }
    
    private static DefaultRedisScript<Long> createDeleteAllByUserScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.DELETE_ALL_REFRESH_TOKENS_BY_USER);
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public void save(String token, Long userId, String ip, String userAgent) {
        long ttlMillis = ttlProperties.getRefreshTokenValidityMillis();
        saveWithTtl(token, userId, ip, userAgent, ttlMillis);
    }
    
    @Override
    public void saveWithTtl(String token, Long userId, String ip, String userAgent, long ttlMillis) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            Duration ttl = Duration.ofMillis(ttlMillis);
            
            // 토큰 저장
            redisTemplate.opsForValue().set(
                    key,
                    String.valueOf(userId),
                    ttl
            );
            
            // 메타데이터 저장 (Hash)
            String metaKey = key + ":meta";
            Map<String, String> metadata = Map.of(
                    "ip", ip != null ? ip : "",
                    "userAgent", userAgent != null ? userAgent : ""
            );
            redisTemplate.opsForHash().putAll(metaKey, metadata);
            redisTemplate.expire(metaKey, ttl);
            
            // 사용자별 Set에 추가
            // TOCTOU 경쟁 조건 방지: add 후 항상 expire 호출
            // Redis의 EXPIRE는 키가 없어도 에러를 발생시키지 않으므로 안전합니다.
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            redisTemplate.opsForSet().add(userKey, token);
            // 키가 이미 존재하면 TTL이 유지되거나 갱신되며, 없으면 새로 생성된 키에 TTL이 설정됩니다.
            // 경쟁 조건을 방지하기 위해 항상 expire를 호출합니다.
            redisTemplate.expire(userKey, ttl);
        } catch (DataAccessException e) {
            log.error("Refresh Token 저장 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public RefreshTokenMetadata getAndDelete(String token) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            String metaKey = key + ":meta";
            String setKeyPrefix = RedisKeyFactory.getRefreshTokenSetPrefix();
            
            // Lua 스크립트를 사용하여 원자적으로 조회 및 삭제
            // 스크립트 내부에서 userId를 조회한 후 userSetKey를 동적으로 구성
            List<String> result = redisTemplate.execute(
                    getAndDeleteScript,
                    List.of(key, metaKey),
                    token,
                    setKeyPrefix
            );
            
            if (result == null || result.isEmpty()) {
                return null; // 토큰이 없거나 이미 사용됨
            }
            
            // 결과 파싱: [userId, ip, userAgent, remainingTtlMillis]
            String userIdStr = result.get(0);
            String ip = result.size() > 1 ? result.get(1) : "";
            String userAgent = result.size() > 2 ? result.get(2) : "";
            long remainingTtlMillis = result.size() > 3 ? Long.parseLong(result.get(3)) : 0L;
            
            Long userId = Long.valueOf(userIdStr);
            return new RefreshTokenMetadata(userId, ip, userAgent, remainingTtlMillis);
        } catch (DataAccessException e) {
            log.error("Refresh Token 조회/삭제 실패: token={}", SensitiveDataMasker.maskToken(token), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (NumberFormatException e) {
            log.error("Refresh Token 메타데이터 파싱 실패: token={}", SensitiveDataMasker.maskToken(token), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean isValid(String token, Long userId) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            String storedUserId = redisTemplate.opsForValue().get(key);
            return storedUserId != null && storedUserId.equals(userId.toString());
        } catch (DataAccessException e) {
            log.error("Refresh Token 유효성 검증 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String token, Long userId) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            String metaKey = key + ":meta";
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            
            // Lua 스크립트를 사용하여 원자적으로 삭제
            Long deletedCount = redisTemplate.execute(
                    deleteScript,
                    List.of(key, metaKey, userKey),
                    token
            );
            
            if (deletedCount == null || deletedCount == 0) {
                log.debug("삭제할 Refresh Token이 없음: token={}, userId={}", 
                        SensitiveDataMasker.maskToken(token), userId);
            }
        } catch (DataAccessException e) {
            log.error("Refresh Token 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Set<String> getAllByUser(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            Set<String> tokens = redisTemplate.opsForSet().members(userKey);
            return tokens != null ? tokens : Set.of();
        } catch (DataAccessException e) {
            log.error("Refresh Token 목록 조회 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteAllByUser(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            String tokenPrefix = RedisKeyFactory.getRefreshTokenPrefix();
            
            // Lua 스크립트를 사용하여 원자적으로 모든 토큰 삭제
            Long deletedCount = redisTemplate.execute(
                    deleteAllByUserScript,
                    List.of(userKey),
                    tokenPrefix
            );
            
            if (deletedCount == null) {
                deletedCount = 0L;
            }
            log.debug("사용자별 Refresh Token 전체 삭제 완료: userId={}, deletedCount={}", userId, deletedCount);
        } catch (DataAccessException e) {
            log.error("사용자별 Refresh Token 전체 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void cleanupExpiredTokens(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            Set<String> tokens = getAllByUser(userId);
            
            if (tokens == null || tokens.isEmpty()) {
                return;
            }
            
            for (String token : tokens) {
                String key = RedisKeyFactory.refreshToken(token);
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    // 만료된 토큰 제거
                    redisTemplate.opsForSet().remove(userKey, token);
                    log.debug("만료된 Refresh Token 정리: userId={}", userId);
                }
            }
        } catch (DataAccessException e) {
            log.error("만료된 Refresh Token 정리 실패: userId={}", userId, e);
            // 정리 작업 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
}

