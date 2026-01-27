package org.example.sharedprompts.global.Lua;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LuaScripts {

    public static final String SAFE_DECREMENT = """
        local val = redis.call('GET', KEYS[1])
        if not val then
            return 0
        end
        
        local num = tonumber(val)
        if not num then
            return 0
        end

        if num <= 0 then
            return 0
        end

        return redis.call('DECR', KEYS[1])
        """;

    /**
     * INCR + EXPIRE 를 하나의 Lua 스크립트에서 처리하여
     * TTL 설정까지 원자적으로 보장하기 위한 스크립트입니다.
     *
     * KEYS[1] : 대상 key
     * ARGV[1] : TTL (seconds)
     */
    public static final String INCREMENT_WITH_TTL = """
        local key = KEYS[1]
        local ttl = tonumber(ARGV[1])

        local newVal = redis.call('INCR', key)

        if ttl and ttl > 0 then
            redis.call('EXPIRE', key, ttl)
        end

        return newVal
        """;

    /**
     * SAFE_DECREMENT + EXPIRE 를 하나의 Lua 스크립트에서 처리하여
     * 감소 연산과 TTL 설정을 원자적으로 보장하기 위한 스크립트입니다.
     *
     * KEYS[1] : 대상 key
     * ARGV[1] : TTL (seconds)
     */
    public static final String SAFE_DECREMENT_WITH_TTL = """
        local key = KEYS[1]
        local ttl = tonumber(ARGV[1])

        local val = redis.call('GET', key)
        if not val then
            return 0
        end

        local num = tonumber(val)
        if not num then
            return 0
        end

        if num <= 0 then
            return 0
        end

        local newVal = redis.call('DECR', key)

        if ttl and ttl > 0 then
            redis.call('EXPIRE', key, ttl)
        end

        return newVal
        """;

    /**
     * Refresh Token을 원자적으로 조회 및 삭제하는 스크립트입니다.
     * 1회용 보장을 위해 조회와 삭제를 원자적으로 처리합니다.
     *
     * KEYS[1] : 토큰 키 (refreshToken:{token})
     * KEYS[2] : 메타데이터 키 (refreshToken:{token}:meta)
     * ARGV[1] : 토큰 값 (Set에서 제거하기 위함)
     * ARGV[2] : 사용자별 Set 키의 prefix (refreshToken:set:)
     *
     * 반환값: [userId, ip, userAgent, remainingTtlMillis] 또는 nil (토큰이 없는 경우)
     * 
     * <p>동작 방식:
     * <ol>
     *   <li>토큰 키에서 userId 조회</li>
     *   <li>메타데이터(ip, userAgent) 조회</li>
     *   <li>TTL 계산 (밀리초 단위, PTTL 사용)</li>
     *   <li>userId를 기반으로 userSetKey 동적 구성: prefix + userId</li>
     *   <li>토큰, 메타데이터, Set에서 토큰 제거</li>
     *   <li>결과 반환</li>
     * </ol>
     */
    public static final String GET_AND_DELETE_REFRESH_TOKEN = """
        local tokenKey = KEYS[1]
        local metaKey = KEYS[2]
        local token = ARGV[1]
        local setKeyPrefix = ARGV[2]
        
        -- 토큰 조회 및 삭제 (원자적 연산)
        local userId = redis.call('GET', tokenKey)
        if not userId then
            return nil
        end
        
        -- 메타데이터 조회
        local ip = redis.call('HGET', metaKey, 'ip')
        local userAgent = redis.call('HGET', metaKey, 'userAgent')
        
        -- TTL 계산 (밀리초 단위, PTTL 사용)
        local ttl = redis.call('PTTL', tokenKey)
        local remainingTtlMillis = ttl and ttl > 0 and ttl or 0
        
        -- 사용자별 Set 키 동적 구성: prefix + userId
        local userSetKey = setKeyPrefix .. userId
        
        -- 삭제 (원자적 연산)
        redis.call('DEL', tokenKey)
        redis.call('DEL', metaKey)
        redis.call('SREM', userSetKey, token)
        
        -- 결과 반환: [userId, ip, userAgent, remainingTtlMillis]
        return {userId, ip or '', userAgent or '', tostring(remainingTtlMillis)}
        """;

    /**
     * OAuth2 임시 토큰을 원자적으로 조회 및 삭제하는 스크립트입니다.
     * TOCTOU 문제를 방지하기 위해 조회와 삭제를 원자적으로 처리합니다.
     *
     * KEYS[1] : 임시 토큰 키
     *
     * 반환값: Hash의 모든 필드와 값을 포함한 배열 [field1, value1, field2, value2, ...] 또는 nil (키가 없는 경우)
     * 
     * <p>동작 방식:
     * <ol>
     *   <li>Hash의 모든 필드와 값을 조회</li>
     *   <li>키 삭제</li>
     *   <li>결과 반환 (평탄화된 배열: [field1, value1, field2, value2, ...])</li>
     * </ol>
     */
    public static final String GET_AND_DELETE_TEMP_TOKEN = """
        local key = KEYS[1]
        
        -- Hash 조회
        local entries = redis.call('HGETALL', key)
        if not entries or #entries == 0 then
            return nil
        end
        
        -- 키 삭제 (원자적 연산)
        redis.call('DEL', key)
        
        -- 결과 반환 (평탄화된 배열: [field1, value1, field2, value2, ...])
        return entries
        """;

    /**
     * Refresh Token을 원자적으로 삭제하는 스크립트입니다.
     * 토큰 키, 메타데이터 키, 사용자별 Set에서 토큰을 원자적으로 제거합니다.
     *
     * KEYS[1] : 토큰 키 (refreshToken:{token})
     * KEYS[2] : 메타데이터 키 (refreshToken:{token}:meta)
     * KEYS[3] : 사용자별 Set 키 (refreshToken:set:{userId})
     * ARGV[1] : 토큰 값 (Set에서 제거하기 위함)
     *
     * 반환값: 삭제된 키의 개수 (0 이상)
     */
    public static final String DELETE_REFRESH_TOKEN = """
        local tokenKey = KEYS[1]
        local metaKey = KEYS[2]
        local userSetKey = KEYS[3]
        local token = ARGV[1]
        
        -- 원자적으로 삭제
        local deletedCount = 0
        if redis.call('DEL', tokenKey) == 1 then
            deletedCount = deletedCount + 1
        end
        if redis.call('DEL', metaKey) == 1 then
            deletedCount = deletedCount + 1
        end
        if redis.call('SREM', userSetKey, token) == 1 then
            deletedCount = deletedCount + 1
        end
        
        return deletedCount
        """;

    /**
     * 사용자별 모든 Refresh Token을 원자적으로 삭제하는 스크립트입니다.
     * 사용자별 Set의 모든 토큰과 메타데이터를 조회하여 삭제한 후 Set 자체도 삭제합니다.
     *
     * KEYS[1] : 사용자별 Set 키 (refreshToken:set:{userId})
     * ARGV[1] : 토큰 키 prefix (refreshToken:)
     *
     * 반환값: 삭제된 토큰의 개수
     */
    public static final String DELETE_ALL_REFRESH_TOKENS_BY_USER = """
        local userSetKey = KEYS[1]
        local tokenPrefix = ARGV[1]
        
        -- Set에서 모든 토큰 조회
        local tokens = redis.call('SMEMBERS', userSetKey)
        if not tokens or #tokens == 0 then
            -- Set이 비어있거나 없으면 Set만 삭제하고 0 반환
            redis.call('DEL', userSetKey)
            return 0
        end
        
        local deletedCount = 0
        
        -- 각 토큰과 메타데이터 삭제
        for i = 1, #tokens do
            local token = tokens[i]
            local tokenKey = tokenPrefix .. token
            local metaKey = tokenKey .. ":meta"
            
            if redis.call('DEL', tokenKey) == 1 then
                deletedCount = deletedCount + 1
            end
            redis.call('DEL', metaKey)
        end
        
        -- Set 삭제
        redis.call('DEL', userSetKey)
        
        return deletedCount
        """;

    /**
     * 태그 카운트 증가 스크립트 (TTL 포함)
     * INCR + EXPIRE를 원자적으로 처리합니다.
     *
     * KEYS[1] : 태그 카운트 키
     * ARGV[1] : TTL (seconds)
     *
     * 반환값: 증가된 카운트 값
     */
    public static final String TAG_COUNT_INCREMENT = """
        local key = KEYS[1]
        local ttl = tonumber(ARGV[1])
        local result = redis.call('INCR', key)
        if ttl and ttl > 0 then
            redis.call('EXPIRE', key, ttl)
        end
        return result
        """;

    /**
     * 태그 카운트 감소 스크립트 (TTL 포함)
     * 0 이하로 내려가지 않도록 보장하며, EXPIRE를 원자적으로 처리합니다.
     *
     * KEYS[1] : 태그 카운트 키
     * ARGV[1] : TTL (seconds)
     *
     * 반환값: 감소된 카운트 값 (0 이상)
     */
    public static final String TAG_COUNT_DECREMENT = """
        local key = KEYS[1]
        local ttl = tonumber(ARGV[1])
        local val = redis.call('GET', key)
        if not val then
            return 0
        end
        local num = tonumber(val)
        if not num or num <= 0 then
            return 0
        end
        local result = redis.call('DECR', key)
        if ttl and ttl > 0 then
            redis.call('EXPIRE', key, ttl)
        end
        return result
        """;

}
