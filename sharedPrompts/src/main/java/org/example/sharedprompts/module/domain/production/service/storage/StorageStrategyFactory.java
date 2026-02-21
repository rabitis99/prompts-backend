package org.example.sharedprompts.module.domain.production.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionStorageProperties;
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

    private final ProductionStorageProperties storageProperties;

    private final List<StorageStrategy> strategies;
    private final Map<StorageType, StorageStrategy> strategyMap;
    private StorageType resolvedStorageType;

    public StorageStrategyFactory(ProductionStorageProperties storageProperties, List<StorageStrategy> strategies) {
        this.storageProperties = storageProperties;
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
        
        // 설정된 기본 전략 타입이 유효한지 조기 검증 및 캐싱
        StorageType defaultType = storageProperties.getType();
        this.resolvedStorageType = defaultType;
        
        if (!strategyMap.containsKey(defaultType)) {
            throw new IllegalStateException(
                    String.format("Configured storage type '%s' has no registered strategy. Available strategies: %s",
                            defaultType, strategyMap.keySet()));
        }
        
        log.info("StorageStrategyFactory initialized with {} strategies: {}. Default type: {}",
                strategyMap.size(), strategyMap.keySet(), defaultType);
    }

    /**
     * 설정된 저장 전략 조회
     */
    public StorageStrategy getStorageStrategy() {
        StorageStrategy strategy = strategyMap.get(resolvedStorageType);
        if (strategy == null) {
            log.error("No storage strategy found for type: {}. Available strategies: {}", 
                    resolvedStorageType, strategyMap.keySet());
            throw new IllegalArgumentException("No storage strategy found for type: " + resolvedStorageType + 
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

