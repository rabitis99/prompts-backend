package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.constant.Constant;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TokenRedisServiceImpl implements TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlConfig;

    private static final String ACCESS_PREFIX = "ACCESS:";
    private static final String REFRESH_PREFIX = "REFRESH:";
    private static final String REFRESH_SET_PREFIX = "USER_REFRESH:";

    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> getAndDeleteTempTokenScript = createGetAndDeleteTempTokenScript();

    @SuppressWarnings("rawtypes")
    private static DefaultRedisScript<List> createGetAndDeleteTempTokenScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.GET_AND_DELETE_TEMP_TOKEN);
        script.setResultType(List.class);
        return script;
    }

    // =========================
    // 🔹 Access Token 관리
    // =========================

    @Override
    public void saveAccessToken(String token, Long userId) {
        redisTemplate.opsForValue().set(
                ACCESS_PREFIX + token,
                String.valueOf(userId),
                Duration.ofMillis(ttlConfig.getAccessTokenValidityMillis())
        );
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
    }

    @Override
    public void deleteAccessToken(String token) {
        redisTemplate.delete(ACCESS_PREFIX + token);
    }

    @Override
    public boolean isAccessTokenValidWithUserId(String accessToken, Long userId) {
        String key = ACCESS_PREFIX + accessToken;
        String storedUserIdStr = redisTemplate.opsForValue().get(key);

        if (storedUserIdStr == null) return false;

        try {
            Long savedUserId = Long.valueOf(storedUserIdStr);
            return userId.equals(savedUserId);
        } catch (NumberFormatException e) {
            redisTemplate.delete(key);
            return false;
        }
    }

    // =========================
    // 🔹 Refresh Token 관리 (개별 키 + 사용자별 Set)
    // =========================

    @Override
    public boolean isRefreshTokenValid(String token, Long userId) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        return storedUserId != null && storedUserId.equals(userId.toString());
    }

    @Override
    public Long getRefreshToken(String token) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        if (storedUserId == null) return null;
        try{
            return Long.valueOf(storedUserId);
        } catch (NumberFormatException e) {
            redisTemplate.delete(key);
            return null;
        }
    }


    @Override
    public void deleteRefreshToken(String token, Long userId) {
        redisTemplate.delete(REFRESH_PREFIX + token);

        String userKey = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().remove(userKey, token);
    }

    @Override
    public Set<String> getAllRefreshTokensByUser(Long userId) {
        String userKey = REFRESH_SET_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        return tokens != null ? tokens : Set.of();
    }

    // =========================
    // 🔹 OAuth2 임시 토큰 관리 (access, refresh, state)
    // =========================
    @Override
    public void saveTempToken(String key, String accessToken, String refreshToken, String state,
                              String provider, String providerId, Duration ttl) {
        Map<String, String> tokens = Map.of(
                Constant.ACCESS_TOKEN_KEY, accessToken,
                Constant.REFRESH_TOKEN_KEY, refreshToken,
                Constant.STATE_KEY, state,
                Constant.PROVIDER_KEY, provider,
                Constant.PROVIDER_ID_KEY, providerId
        );
        redisTemplate.opsForHash().putAll(key, tokens);
        redisTemplate.expire(key, ttl);
    }

    @Override
    public Map<String, String> getAndDeleteTempToken(String key) {
        try {
            // Lua 스크립트를 사용하여 원자적으로 조회 및 삭제
            // TOCTOU 문제 방지: 조회와 삭제를 하나의 원자적 연산으로 처리
            @SuppressWarnings("unchecked")
            List<String> entries = redisTemplate.execute(
                    getAndDeleteTempTokenScript,
                    List.of(key)
            );

            if (entries == null || entries.isEmpty()) {
                return null;
            }

            // Lua 스크립트는 평탄화된 배열을 반환: [field1, value1, field2, value2, ...]
            // 이를 Map으로 변환
            Map<String, String> result = new java.util.HashMap<>();
            for (int i = 0; i < entries.size(); i += 2) {
                if (i + 1 < entries.size()) {
                    String field = entries.get(i);
                    String value = entries.get(i + 1);
                    if (field != null && value != null) {
                        result.put(field, value);
                    }
                }
            }

            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            // 예외 발생 시 민감한 정보 노출 방지를 위해 키를 마스킹
            String maskedKey = SensitiveDataMasker.mask(key);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, 
                    "Redis 임시 토큰 조회/삭제 실패: " + maskedKey, e);
        }
    }

}

