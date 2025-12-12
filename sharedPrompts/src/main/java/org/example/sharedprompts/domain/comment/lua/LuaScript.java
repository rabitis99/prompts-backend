package org.example.sharedprompts.domain.comment.lua;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LuaScript {

    public static final String SAFE_DECREMENT = """
            local val = redis.call('GET', KEYS[1])
            if not val then
                return 0
            end
            val = tonumber(val)
            if val <= 0 then
                return 0
            else
                return redis.call('DECR', KEYS[1])
            end
            """;
}
