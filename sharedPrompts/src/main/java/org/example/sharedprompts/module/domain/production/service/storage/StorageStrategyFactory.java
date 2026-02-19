package org.example.sharedprompts.module.domain.production.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StorageStrategy 팩토리
 * S3 단일 저장 전략을 사용합니다.
 */
@Component
@Slf4j
public class StorageStrategyFactory {

    @Value("${production.storage.type:S3}")
    private String storageType;

    private final List<StorageStrategy> strategies;
    private Map<StorageType, StorageStrategy> strategyMap;

    public StorageStrategyFactory(List<StorageStrategy> strategies) {
        this.strategies = strategies;
        this.strategyMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void initialize() {
        for (StorageStrategy strategy : strategies) {
            strategyMap.put(strategy.getStorageType(), strategy);
            log.info("Registered StorageStrategy: {} for type: {}", 
                    strategy.getClass().getSimpleName(), strategy.getStorageType());
        }
        log.info("StorageStrategyFactory initialized with {} strategies: {}", 
                strategyMap.size(), strategyMap.keySet());
    }

    /**
     * 설정된 저장 전략 조회
     */
    public StorageStrategy getStorageStrategy() {
        StorageType type = StorageType.valueOf(storageType.toUpperCase());
        StorageStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            log.error("No storage strategy found for type: {}. Available strategies: {}", 
                    type, strategyMap.keySet());
            throw new IllegalArgumentException("No storage strategy found for type: " + type + 
                    ". Available strategies: " + strategyMap.keySet());
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

