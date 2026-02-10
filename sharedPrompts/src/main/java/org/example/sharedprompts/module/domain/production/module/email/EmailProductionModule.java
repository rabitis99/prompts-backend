package org.example.sharedprompts.module.domain.production.module.email;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.TextArtifact;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.model.DefaultModuleProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ModuleProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.module.ProductionModule;
import org.example.sharedprompts.module.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.module.domain.production.exception.ProductionException;
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
    public ModuleProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof EmailCommand emailCommand)) {
            throw new CommandValidationException(
                "Expected EmailCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
        
        if (promptResult == null) {
            throw new ProductionException("PromptResult not found in context");
        }
        
        String emailContent = emailComposer.compose(
            emailCommand.getSubject(),
            promptResult.getContent(),
            emailCommand.getRecipient()
        );
        
        Instant completedAt = Instant.now();
        
        ProductionArtifact artifact = new TextArtifact(emailContent);
        return DefaultModuleProductionResult.success(artifact, startedAt, completedAt);
    }
}

