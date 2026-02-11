package org.example.sharedprompts.module.domain.production.service.job.process.execution.content;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.ContentRenderException;
import org.example.sharedprompts.module.domain.production.service.renderer.ProductionRenderer;
import org.example.sharedprompts.module.domain.production.service.renderer.RendererRegistry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentRenderer {

    private final RendererRegistry rendererRegistry;

    public String render(JsonNode parsedResponse, ProductionCommandType commandType) {
        log.info("Rendering content - commandType: {}", commandType);

        try {
            ProductionRenderer renderer = rendererRegistry.getRenderer(commandType);
            return renderer.render(parsedResponse);
        } catch (Exception e) {
            log.error("Failed to render content - commandType: {}", commandType, e);
            throw new ContentRenderException("Failed to render content: " + e.getMessage(), e);
        }
    }
}

