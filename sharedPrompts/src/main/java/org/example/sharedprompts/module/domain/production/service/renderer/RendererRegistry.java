package org.example.sharedprompts.module.domain.production.service.renderer;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer 레지스트리
 * CommandType별로 적절한 Renderer를 찾아 렌더링 수행
 */
@Component
@Slf4j
public class RendererRegistry {

    private final Map<ProductionCommandType, ProductionRenderer> rendererMap = new ConcurrentHashMap<>();

    public RendererRegistry(List<ProductionRenderer> renderers) {
        // 모든 Renderer 구현체를 등록
        for (ProductionRenderer renderer : renderers) {
            for (ProductionCommandType commandType : ProductionCommandType.values()) {
                if (renderer.supports(commandType)) {
                    rendererMap.put(commandType, renderer);
                    log.info("Registered Renderer: {} for CommandType: {}", 
                            renderer.getClass().getSimpleName(), commandType);
                    break;
                }
            }
        }
    }

    /**
     * CommandType에 맞는 Renderer 조회
     */
    public ProductionRenderer getRenderer(ProductionCommandType commandType) {
        ProductionRenderer renderer = rendererMap.get(commandType);
        if (renderer == null) {
            throw new IllegalArgumentException("No renderer found for CommandType: " + commandType);
        }
        return renderer;
    }
}

