package org.example.sharedprompts.domain.shared.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface BaseCountService {
    
    void increment(String key);
    
    void decrement(String key);
    
    Map<Long, Long> getCounts(List<Long> ids, Function<Long, String> keyMapper);
}
