package org.example.sharedprompts.domain.production.module.document;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.api.FileArtifact;
import org.example.sharedprompts.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DocumentProductionModule implements ProductionModule {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.DOCUMENT;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof DocumentCommand documentCommand)) {
            throw new CommandValidationException(
                "Expected DocumentCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            if (promptResult == null) {
                return DocumentResult.failure("PromptResult not found in context", startedAt, Instant.now());
            }
            
            String filePath = String.format("output/document/%s.%s", documentCommand.getFileName(), documentCommand.getFormat());
            
            Instant completedAt = Instant.now();
            
            org.example.sharedprompts.domain.production.api.ProductionArtifact artifact = new FileArtifact(filePath);
            return DocumentResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return DocumentResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}

