package org.example.sharedprompts.module.domain.production.service.literary;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LiteraryGenerationStrategyRegistry {

    private final Map<LiteraryType, LiteraryGenerationStrategy> strategyMap;

    public LiteraryGenerationStrategyRegistry(List<LiteraryGenerationStrategy> strategies) {
        Map<LiteraryType, LiteraryGenerationStrategy> map = new EnumMap<>(LiteraryType.class);
        for (LiteraryGenerationStrategy strategy : strategies) {
            LiteraryGenerationStrategy existing = map.put(strategy.getLiteraryType(), strategy);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate LiteraryGenerationStrategy for type " + strategy.getLiteraryType()
                                + ": " + existing.getClass().getSimpleName() + " and " + strategy.getClass().getSimpleName());
            }
            log.info("Registered LiteraryGenerationStrategy: {} for {}", strategy.getClass().getSimpleName(), strategy.getLiteraryType());
        }
        this.strategyMap = Collections.unmodifiableMap(map);
    }

    public LiteraryGenerationStrategy getStrategy(LiteraryType literaryType) {
        if (literaryType == null) {
            throw new IllegalArgumentException("literaryType must not be null");
        }
        LiteraryGenerationStrategy strategy = strategyMap.get(literaryType);
        if (strategy == null) {
            throw new IllegalArgumentException("No LiteraryGenerationStrategy for type: " + literaryType);
        }
        return strategy;
    }
}
