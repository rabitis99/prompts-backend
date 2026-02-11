package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StorageStrategy 팩토리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageStrategyFactory {

    @Value("${production.storage.type:LOCAL}")
    private String storageType;

    private final Map<StorageType, StorageStrategy> strategyMap = new ConcurrentHashMap<>();

    public StorageStrategyFactory(List<StorageStrategy> strategies) {
        for (StorageStrategy strategy : strategies) {
            strategyMap.put(strategy.getStorageType(), strategy);
            log.info("Registered StorageStrategy: {} for type: {}", 
                    strategy.getClass().getSimpleName(), strategy.getStorageType());
        }
    }

    /**
     * 설정된 저장 전략 조회
     */
    public StorageStrategy getStorageStrategy() {
        StorageType type = StorageType.valueOf(storageType);
        StorageStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No storage strategy found for type: " + type);
        }
        return strategy;
    }

    /**
     * 특정 타입의 저장 전략 조회
     */
    public StorageStrategy getStorageStrategy(StorageType type) {
        StorageStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No storage strategy found for type: " + type);
        }
        return strategy;
    }
}

