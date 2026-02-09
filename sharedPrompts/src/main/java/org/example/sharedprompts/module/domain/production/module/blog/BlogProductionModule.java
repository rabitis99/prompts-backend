package org.example.sharedprompts.module.domain.production.module.blog;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.TextArtifact;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.model.DefaultProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.example.sharedprompts.module.domain.production.api.module.ProductionModule;
import org.example.sharedprompts.module.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.module.infra.production.blog.BlogComposer;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BlogProductionModule implements ProductionModule {
    
    private final BlogComposer blogComposer;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.BLOG;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof BlogCommand blogCommand)) {
            throw new CommandValidationException(
                "Expected BlogCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            if (promptResult == null) {
                return DefaultProductionResult.failure("PromptResult not found in context", startedAt, Instant.now());
            }
            
            String blogContent = blogComposer.compose(
                blogCommand.getTitle(),
                promptResult.getContent(),
                blogCommand.getTags()
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new TextArtifact(blogContent);
            return DefaultProductionResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return DefaultProductionResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}

