package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ArtifactHandlerRegistry {

    private final Map<ArtifactType, ArtifactHandler> handlerMap;

    public ArtifactHandlerRegistry(List<ArtifactHandler> handlers) {
        Map<ArtifactType, ArtifactHandler> map = new HashMap<>();
        for (ArtifactHandler handler : handlers) {
            ArtifactHandler existing = map.put(handler.getSupportedType(), handler);
            if (existing != null) {
                log.warn("Duplicate ArtifactHandler for type: {}. {} replaced by {}",
                        handler.getSupportedType(),
                        existing.getClass().getSimpleName(),
                        handler.getClass().getSimpleName());
            }
            log.info("Registered ArtifactHandler: {} for type: {}",
                    handler.getClass().getSimpleName(), handler.getSupportedType());
        }
        this.handlerMap = Collections.unmodifiableMap(map);
    }

    public ArtifactHandler getHandler(ArtifactType type) {
        ArtifactHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No ArtifactHandler found for type: " + type);
        }
        return handler;
    }
}
