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
        
        local num = tonumber(val)
        if not num then
            return 0
        end

        if num <= 0 then
            return 0
        end

        return redis.call('DECR', KEYS[1])
        """;

}
