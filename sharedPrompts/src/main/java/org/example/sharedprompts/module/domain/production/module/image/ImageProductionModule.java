package org.example.sharedprompts.module.domain.production.module.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.api.artifact.ImageArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.model.DefaultProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.example.sharedprompts.module.domain.production.api.module.ProductionModule;
import org.example.sharedprompts.module.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.module.infra.production.image.ImageGenerator;
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
            // 보안: 프롬프트 전문 대신 길이만 로깅하여 민감 정보 노출 방지
            int promptLength = imageCommand.getPrompt() != null ? imageCommand.getPrompt().length() : 0;
            log.error("Image generation failed: promptLength={}", promptLength, e);
            return DefaultProductionResult.failure(
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                startedAt, Instant.now()
            );
        }
    }
}

