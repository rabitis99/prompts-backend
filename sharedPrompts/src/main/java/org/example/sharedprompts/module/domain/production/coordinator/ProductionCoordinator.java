package org.example.sharedprompts.module.domain.production.coordinator;

import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.model.DefaultProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.example.sharedprompts.module.domain.production.api.module.ProductionModule;
import org.example.sharedprompts.module.domain.production.entity.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.exception.ProductionModuleNotFoundException;
import org.example.sharedprompts.module.domain.production.repository.ProductionArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Component
public class ProductionCoordinator {
    
    private static final Logger log = LoggerFactory.getLogger(ProductionCoordinator.class);
    
    private final ProductionRegistry registry;
    private final ProductionArtifactRepository artifactRepository;
    private final TransactionTemplate transactionTemplate;
    
    public ProductionCoordinator(
            ProductionRegistry registry,
            ProductionArtifactRepository artifactRepository,
            PlatformTransactionManager transactionManager) {
        this.registry = registry;
        this.artifactRepository = artifactRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }
    
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        ProductionModule module = registry.findModule(command.getCommandType());
        
        if (module == null) {
            throw new ProductionModuleNotFoundException(command.getCommandType());
        }
        
        Instant startedAt = Instant.now();
        ProductionResult result;
        
        try {
            result = module.produce(command, context);
        } catch (Exception e) {
            Instant completedAt = Instant.now();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            result = DefaultProductionResult.failure(errorMsg, startedAt, completedAt);
        }
        
        final ProductionResult finalResult = result;
        Long artifactId = null;
        try {
            artifactId = transactionTemplate.execute(status -> {
                return saveArtifact(command, context, finalResult);
            });
        } catch (Exception e) {
            // 아티팩트 저장 실패는 로깅만 하고 원래 결과는 반환
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("아티팩트 저장 실패 - userId: {}, error: {}", 
                    context.getUserId(), errorMsg, e);
        }
        
        // 저장된 artifact ID를 포함한 결과 반환
        if (artifactId != null) {
            if (finalResult.isSuccess()) {
                return DefaultProductionResult.success(
                    finalResult.getArtifact(),
                    finalResult.getStartedAt(),
                    finalResult.getCompletedAt(),
                    artifactId
                );
            } else {
                return DefaultProductionResult.failure(
                    finalResult.getErrorMessage(),
                    finalResult.getStartedAt(),
                    finalResult.getCompletedAt(),
                    artifactId
                );
            }
        }
        return result;
    }
    
    private Long saveArtifact(
            ProductionCommand command,
            ProductionContext context,
            ProductionResult result
    ) {
        var artifact = result.getArtifact();
        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
            .userId(context.getUserId())
            .commandType(command.getCommandType())
            .artifactType(artifact != null ? artifact.getType() : null)
            .location(artifact != null ? artifact.getLocation() : null)
            .startedAt(result.getStartedAt())
            .completedAt(result.getCompletedAt())
            .success(result.isSuccess())
            .errorMessage(result.getErrorMessage())
            .build();
        
        ProductionArtifactEntity saved = artifactRepository.save(entity);
        log.debug("Production artifact saved - id: {}, userId: {}", saved.getId(), saved.getUserId());
        return saved.getId();
    }
}

