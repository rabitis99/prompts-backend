package org.example.sharedprompts.domain.shared.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface BaseCountService {
    
    void increment(String key);
    
    void decrement(String key);

    /**
     * 값을 1 증가시키고 증가된 값을 반환한다.
     * - Redis의 INCR 연산을 기반으로 하며, Lua 없이도 원자성이 보장된다.
     */
    long incrementAndGet(String key);

    /**
     * 값을 1 감소시키되, 0 미만으로 내려가지 않도록 보장하고 감소된 값을 반환한다.
     * - Lua SAFE_DECREMENT 스크립트를 사용하여 동시성 하에서도 음수로 내려가지 않게 제어한다.
     */
    long decrementAndGet(String key);

    /**
     * 관리자/배치 복구용: 특정 키의 값을 정확히 지정된 값으로 설정한다.
     * - 일반 비즈니스 로직에서는 사용하지 말고, 운영/복구 시에만 사용한다.
     */
    void set(String key, long value);

    Map<Long, Long> getCounts(List<Long> ids, Function<Long, String> keyMapper);
}
