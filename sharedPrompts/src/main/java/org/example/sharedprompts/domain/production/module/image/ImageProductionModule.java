package org.example.sharedprompts.domain.production.module.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.production.api.DefaultProductionResult;
import org.example.sharedprompts.domain.production.api.ImageArtifact;
import org.example.sharedprompts.domain.production.api.ProductionArtifact;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.infra.production.image.ImageGenerator;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProductionModule implements ProductionModule {
    
    private final ImageGenerator imageGenerator;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.IMAGE;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof ImageCommand imageCommand)) {
            throw new CommandValidationException(
                "Expected ImageCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            String imagePath = imageGenerator.generate(
                imageCommand.getPrompt(),
                imageCommand.getWidth(),
                imageCommand.getHeight()
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new ImageArtifact(imagePath);
            return DefaultProductionResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            log.error("Image generation failed for prompt: {}", imageCommand.getPrompt(), e);
            return DefaultProductionResult.failure(
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                startedAt, Instant.now()
            );
        }
    }
}

