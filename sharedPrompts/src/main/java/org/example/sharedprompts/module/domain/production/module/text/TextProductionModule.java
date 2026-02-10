package org.example.sharedprompts.module.domain.production.module.text;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.artifact.FileArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.model.DefaultModuleProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ModuleProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.module.ProductionModule;
import org.example.sharedprompts.module.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.module.domain.production.exception.ProductionException;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.module.infra.production.text.TextFileWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
    public ModuleProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof TextCommand textCommand)) {
            throw new CommandValidationException(
                "Expected TextCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
        
        if (promptResult == null) {
            throw new ProductionException("PromptResult not found in context");
        }
        
        String filePath;
        try {
            filePath = textFileWriter.write(
                promptResult.getContent(),
                textCommand.getFileName(),
                textCommand.getFormat()
            );
        } catch (IOException e) {
            throw new ProductionException("Failed to write text file: " + e.getMessage(), e);
        }
        
        Instant completedAt = Instant.now();
        
        ProductionArtifact artifact = new FileArtifact(filePath);
        return DefaultModuleProductionResult.success(artifact, startedAt, completedAt);
    }
}

