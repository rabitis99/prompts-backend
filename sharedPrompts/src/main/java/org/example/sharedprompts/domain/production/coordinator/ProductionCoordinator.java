package org.example.sharedprompts.domain.production.coordinator;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.entity.ProductionArtifactEntity;
import org.example.sharedprompts.domain.production.exception.ProductionModuleNotFoundException;
import org.example.sharedprompts.domain.production.repository.ProductionArtifactRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductionCoordinator {
    
    private final ProductionRegistry registry;
    private final ProductionArtifactRepository artifactRepository;
    
    @Transactional
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        ProductionModule module = registry.findModule(command.getCommandType());
        
        if (module == null) {
            throw new ProductionModuleNotFoundException(command.getCommandType());
        }
        
        ProductionResult result = module.produce(command, context);
        
        saveArtifact(command, context, result);
        
        return result;
    }
    
    private void saveArtifact(
            ProductionCommand command,
            ProductionContext context,
            ProductionResult result
    ) {
        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
            .productionId(context.getProductionId())
            .userId(context.getUserId())
            .commandType(command.getCommandType())
            .artifactType(result.getArtifact() != null ? result.getArtifact().getType() : null)
            .location(result.getArtifact() != null ? result.getArtifact().getLocation() : null)
            .startedAt(result.getStartedAt())
            .completedAt(result.getCompletedAt())
            .success(result.isSuccess())
            .errorMessage(result.getErrorMessage())
            .build();
        
        artifactRepository.save(entity);
    }
}

