package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.DefaultDeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.coordinator.DeliveryRegistry;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.production.api.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.ProductionResult;
import org.example.sharedprompts.module.domain.production.coordinator.ProductionCoordinator;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptProductionDeliveryResponse;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptProductionDeliveryFacade {
    
    private final PromptCreationFlow promptCreationFlow;
    private final ProductionCoordinator productionCoordinator;
    private final DeliveryRegistry deliveryRegistry;
    
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
        
        DeliveryResult deliveryResult;
        try {
            DeliveryService deliveryService = deliveryRegistry.find(deliveryContext.getDeliveryType());
            
            if (deliveryService == null) {
                deliveryResult = DefaultDeliveryResult.failure(
                    "Delivery service not found for type: " + deliveryContext.getDeliveryType()
                );
            } else {
                deliveryResult = deliveryService.deliver(
                    productionResult.getArtifact(),
                    deliveryContext
                );
            }
        } catch (DeliveryException e) {
            // 배달 실패 시에도 프롬프트는 유지 (프로덕션 실패와 동일한 처리)
            deliveryResult = DefaultDeliveryResult.failure(e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 예외도 실패 결과로 변환
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            deliveryResult = DefaultDeliveryResult.failure("Delivery failed: " + errorMessage);
        }
        
        return PromptProductionDeliveryResponse.of(
            promptResult, 
            productionResult, 
            deliveryResult
        );
    }
}

