package org.example.sharedprompts.domain.production.module.text;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.production.api.DefaultProductionResult;
import org.example.sharedprompts.domain.production.api.FileArtifact;
import org.example.sharedprompts.domain.production.api.ProductionArtifact;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.infra.production.text.TextFileWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TextProductionModule implements ProductionModule {
    
    private final TextFileWriter textFileWriter;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof TextCommand textCommand)) {
            throw new CommandValidationException(
                "Expected TextCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            if (promptResult == null) {
                return DefaultProductionResult.failure("PromptResult not found in context", startedAt, Instant.now());
            }
            
            String filePath = textFileWriter.write(
                promptResult.getContent(),
                textCommand.getFileName(),
                textCommand.getFormat()
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new FileArtifact(filePath);
            return DefaultProductionResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return DefaultProductionResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}

