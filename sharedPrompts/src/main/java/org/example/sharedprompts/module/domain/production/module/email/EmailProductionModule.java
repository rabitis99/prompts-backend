package org.example.sharedprompts.module.domain.production.module.email;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.DefaultProductionResult;
import org.example.sharedprompts.module.domain.production.api.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.ProductionModule;
import org.example.sharedprompts.module.domain.production.api.ProductionResult;
import org.example.sharedprompts.module.domain.production.api.TextArtifact;
import org.example.sharedprompts.module.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.module.infra.production.email.EmailComposer;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EmailProductionModule implements ProductionModule {
    
    private final EmailComposer emailComposer;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.EMAIL;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof EmailCommand emailCommand)) {
            throw new CommandValidationException(
                "Expected EmailCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            if (promptResult == null) {
                return DefaultProductionResult.failure("PromptResult not found in context", startedAt, Instant.now());
            }
            
            String emailContent = emailComposer.compose(
                emailCommand.getSubject(),
                promptResult.getContent(),
                emailCommand.getRecipient()
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new TextArtifact(emailContent);
            return DefaultProductionResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return DefaultProductionResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}

