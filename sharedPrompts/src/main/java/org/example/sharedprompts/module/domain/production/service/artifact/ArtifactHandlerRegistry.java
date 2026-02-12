package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ArtifactHandlerRegistry {

    private final Map<ArtifactType, ArtifactHandler> handlerMap = new ConcurrentHashMap<>();

    public ArtifactHandlerRegistry(List<ArtifactHandler> handlers) {
        for (ArtifactHandler handler : handlers) {
            handlerMap.put(handler.getSupportedType(), handler);
            log.info("Registered ArtifactHandler: {} for type: {}",
                    handler.getClass().getSimpleName(), handler.getSupportedType());
        }
    }

    public ArtifactHandler getHandler(ArtifactType type) {
        ArtifactHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No ArtifactHandler found for type: " + type);
        }
        return handler;
    }
}
