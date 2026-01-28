package org.example.sharedprompts.domain.notification.service.core.strategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 알림 서비스 전략 팩토리
 * 
 * 조회 전략과 읽음 처리 전략을 선택하여 반환합니다.
 * 확장성을 위해 전략들을 Map으로 관리하며, 기본 전략을 제공합니다.
 * 
 * 초기화:
 * - @PostConstruct를 사용하여 애플리케이션 시작 시 전략들을 미리 초기화합니다.
 * - 이를 통해 런타임 시 동기화 오버헤드를 방지하고 성능을 향상시킵니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceFactory {

    private final List<NotificationQueryStrategy> queryStrategies;
    private final List<NotificationReadStrategy> readStrategies;

    private Map<QueryStrategyType, NotificationQueryStrategy> queryStrategyMap;
    private Map<ReadStrategyType, NotificationReadStrategy> readStrategyMap;

    /**
     * 애플리케이션 시작 시 전략들을 초기화합니다.
     */
    @PostConstruct
    public void initialize() {
        initializeQueryStrategies();
        initializeReadStrategies();
        log.info("NotificationServiceFactory initialized: {} query strategies, {} read strategies",
                queryStrategyMap.size(), readStrategyMap.size());
    }

    /**
     * 조회 전략을 초기화합니다.
     */
    private void initializeQueryStrategies() {
        queryStrategyMap = queryStrategies.stream()
                .collect(Collectors.toMap(
                        NotificationQueryStrategy::getStrategyType,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("Duplicate query strategy type detected: {}. Using existing strategy.", 
                                    existing.getStrategyType());
                            return existing;
                        }
                ));
        
        // 기본 전략 존재 여부 확인
        if (!queryStrategyMap.containsKey(QueryStrategyType.UNREAD_ONLY)) {
            throw new IllegalStateException(
                    "Default query strategy (UNREAD_ONLY) not found. Please ensure UnreadOnlyQueryStrategy is registered.");
        }
    }

    /**
     * 읽음 처리 전략을 초기화합니다.
     */
    private void initializeReadStrategies() {
        readStrategyMap = readStrategies.stream()
                .collect(Collectors.toMap(
                        NotificationReadStrategy::getStrategyType,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("Duplicate read strategy type detected: {}. Using existing strategy.", 
                                    existing.getStrategyType());
                            return existing;
                        }
                ));
        
        // 기본 전략 존재 여부 확인
        if (!readStrategyMap.containsKey(ReadStrategyType.STANDARD)) {
            throw new IllegalStateException(
                    "Default read strategy (STANDARD) not found. Please ensure StandardReadStrategy is registered.");
        }
    }

    /**
     * 조회 전략을 반환합니다.
     * 
     * @param strategyType 전략 타입 (null인 경우 기본 전략 반환)
     * @return 조회 전략
     */
    public NotificationQueryStrategy getQueryStrategy(QueryStrategyType strategyType) {
        if (strategyType == null) {
            return queryStrategyMap.get(QueryStrategyType.UNREAD_ONLY);
        }
        
        NotificationQueryStrategy strategy = queryStrategyMap.get(strategyType);
        if (strategy == null) {
            // 기본 전략으로 폴백
            log.warn("Query strategy not found for type: {}. Falling back to default strategy.", strategyType);
            return queryStrategyMap.get(QueryStrategyType.UNREAD_ONLY);
        }
        
        return strategy;
    }

    /**
     * 읽음 처리 전략을 반환합니다.
     * 
     * @param strategyType 전략 타입 (null인 경우 기본 전략 반환)
     * @return 읽음 처리 전략
     */
    public NotificationReadStrategy getReadStrategy(ReadStrategyType strategyType) {
        if (strategyType == null) {
            return readStrategyMap.get(ReadStrategyType.STANDARD);
        }
        
        NotificationReadStrategy strategy = readStrategyMap.get(strategyType);
        if (strategy == null) {
            // 기본 전략으로 폴백
            log.warn("Read strategy not found for type: {}. Falling back to default strategy.", strategyType);
            return readStrategyMap.get(ReadStrategyType.STANDARD);
        }
        
        return strategy;
    }

    /**
     * 기본 조회 전략을 반환합니다.
     * 
     * @return 기본 조회 전략
     */
    public NotificationQueryStrategy getDefaultQueryStrategy() {
        return getQueryStrategy(QueryStrategyType.UNREAD_ONLY);
    }

    /**
     * 기본 읽음 처리 전략을 반환합니다.
     * 
     * @return 기본 읽음 처리 전략
     */
    public NotificationReadStrategy getDefaultReadStrategy() {
        return getReadStrategy(ReadStrategyType.STANDARD);
    }
}

