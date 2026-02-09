package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.coordinator.DeliveryCoordinator;
import org.example.sharedprompts.module.domain.production.api.artifact.FileArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.ImageArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.TextArtifact;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.model.DefaultProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.example.sharedprompts.module.domain.production.coordinator.ProductionCoordinator;
import org.example.sharedprompts.module.domain.production.repository.ProductionArtifactRepository;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptProductionDeliveryResponse;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptProductionDeliveryFacade {
    
    private final PromptCreationFlow promptCreationFlow;
    private final ProductionCoordinator productionCoordinator;
    private final DeliveryCoordinator deliveryCoordinator;
    private final PromptService promptService;
    private final ProductionArtifactRepository productionArtifactRepository;
    
    // 프롬프트 생성은 PromptCreationFlow.create()에서 자체 트랜잭션 관리
    // 프로덕션과 배달은 외부 호출(AI, API)이므로 트랜잭션 외부에서 실행
    public PromptProductionDeliveryResponse createProduceAndDeliver(
            PromptRequestDto request, 
            Long userId,
            ProductionCommand productionCommand,
            DeliveryContext deliveryContext
    ) {
        PromptResponseDto promptResult = promptCreationFlow.create(request, userId);
        
        ProductionContext productionContext = new ProductionContext(userId);
        productionContext.setAttribute("promptResult", promptResult);
        
        ProductionResult productionResult = productionCoordinator.produce(
            productionCommand, 
            productionContext
        );
        
        if (!productionResult.isSuccess()) {
            return PromptProductionDeliveryResponse.of(
                promptResult, 
                productionResult, 
                null
            );
        }
        
        // productionId를 context에 설정하여 DeliveryCoordinator가 저장 시 사용할 수 있도록 함
        deliveryContext.setAttribute("productionId", productionContext.getProductionId());
        
        // DeliveryCoordinator가 결과 저장까지 처리
        DeliveryResult deliveryResult = deliveryCoordinator.deliver(
            productionResult.getArtifact(),
            deliveryContext
        );
        
        return PromptProductionDeliveryResponse.of(
            promptResult, 
            productionResult, 
            deliveryResult
        );
    }
    
    public ProductionResult executeProduction(
            Long promptId,
            Long userId,
            ProductionCommand productionCommand,
            String userInput
    ) {
        PromptResponseDto promptResult = promptService.getPromptDetail(promptId, userId);
        
        ProductionContext productionContext = new ProductionContext(userId);
        productionContext.setAttribute("promptResult", promptResult);
        if (userInput != null && !userInput.isBlank()) {
            productionContext.setAttribute("userInput", userInput);
        }
        
        return productionCoordinator.produce(productionCommand, productionContext);
    }
    
    public DeliveryResult executeDelivery(
            String productionId,
            DeliveryContext deliveryContext
    ) {
        ProductionResult productionResult = getProductionResult(productionId, deliveryContext.getUserId());
        
        if (!productionResult.isSuccess() || productionResult.getArtifact() == null) {
            throw new IllegalStateException(
                "Production이 성공하지 않았거나 Artifact가 없습니다. productionId: " + productionId
            );
        }
        
        // productionId를 context에 설정하여 DeliveryCoordinator가 저장 시 사용할 수 있도록 함
        deliveryContext.setAttribute("productionId", productionId);
        
        // DeliveryCoordinator가 결과 저장까지 처리
        return deliveryCoordinator.deliver(productionResult.getArtifact(), deliveryContext);
    }
    
    public ProductionResult getProductionResult(String productionId, Long userId) {
        return productionArtifactRepository.findByProductionId(productionId)
                .filter(entity -> entity.getUserId().equals(userId))
                .map(entity -> {
                    ProductionArtifact artifact = null;
                    if (entity.getArtifactType() != null && entity.getLocation() != null) {
                        artifact = switch (entity.getArtifactType()) {
                            case TEXT -> new TextArtifact(entity.getLocation());
                            case FILE -> new FileArtifact(entity.getLocation());
                            case IMAGE -> new ImageArtifact(entity.getLocation());
                        };
                    }
                    
                    if (entity.isSuccess()) {
                        return DefaultProductionResult.success(
                            artifact,
                            entity.getStartedAt(),
                            entity.getCompletedAt()
                        );
                    } else {
                        return DefaultProductionResult.failure(
                            entity.getErrorMessage(),
                            entity.getStartedAt(),
                            entity.getCompletedAt()
                        );
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException(
                    "Production 결과를 찾을 수 없습니다. productionId: " + productionId
                ));
    }
}

