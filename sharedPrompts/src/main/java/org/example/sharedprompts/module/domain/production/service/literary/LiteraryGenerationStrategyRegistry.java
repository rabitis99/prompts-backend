package org.example.sharedprompts.module.domain.production.service.literary;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LiteraryGenerationStrategyRegistry {

    private final Map<LiteraryType, LiteraryGenerationStrategy> strategyMap = new ConcurrentHashMap<>();

    public LiteraryGenerationStrategyRegistry(List<LiteraryGenerationStrategy> strategies) {
        for (LiteraryGenerationStrategy strategy : strategies) {
            strategyMap.put(strategy.getLiteraryType(), strategy);
            log.info("Registered LiteraryGenerationStrategy: {} for {}", strategy.getClass().getSimpleName(), strategy.getLiteraryType());
        }
    }

    public LiteraryGenerationStrategy getStrategy(LiteraryType literaryType) {
        LiteraryGenerationStrategy strategy = strategyMap.get(literaryType);
        if (strategy == null) {
            throw new IllegalArgumentException("No LiteraryGenerationStrategy for type: " + literaryType);
        }
        return strategy;
    }
}
