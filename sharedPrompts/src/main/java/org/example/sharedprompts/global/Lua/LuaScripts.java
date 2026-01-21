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

}
