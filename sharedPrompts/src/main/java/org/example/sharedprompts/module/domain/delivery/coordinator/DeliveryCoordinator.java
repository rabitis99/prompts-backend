package org.example.sharedprompts.module.domain.delivery.coordinator;

import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.model.DefaultDeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.service.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.entity.DeliveryEntity;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryServiceNotFoundException;
import org.example.sharedprompts.module.domain.delivery.repository.DeliveryRepository;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Component
public class DeliveryCoordinator {
    
    private static final Logger log = LoggerFactory.getLogger(DeliveryCoordinator.class);
    
    private final DeliveryRegistry registry;
    private final DeliveryRepository deliveryRepository;
    private final TransactionTemplate transactionTemplate;
    
    public DeliveryCoordinator(
            DeliveryRegistry registry,
            DeliveryRepository deliveryRepository,
            PlatformTransactionManager transactionManager) {
        this.registry = registry;
        this.deliveryRepository = deliveryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }
    
    public DeliveryResult deliver(
            ProductionArtifact artifact,
            DeliveryContext context
    ) {
        DeliveryService deliveryService = registry.find(context.getDeliveryType());
        
        if (deliveryService == null) {
            throw new DeliveryServiceNotFoundException(context.getDeliveryType());
        }
        
        Instant startedAt = Instant.now();
        DeliveryResult result;
        
        try {
            result = deliveryService.deliver(artifact, context);
        } catch (DeliveryException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            result = DefaultDeliveryResult.failure(errorMsg);
            // DeliveryException은 이미 처리된 예외이므로 저장만 진행
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            result = DefaultDeliveryResult.failure("Delivery failed: " + errorMsg);
        }
        
        final DeliveryResult finalResult = result;
        final Instant finalStartedAt = startedAt;
        final Instant finalCompletedAt = Instant.now();
        
        try {
            transactionTemplate.execute(status -> {
                saveDelivery(context, finalResult, finalStartedAt, finalCompletedAt);
                return null;
            });
        } catch (Exception e) {
            // 배달 저장 실패는 로깅만 하고 원래 결과는 반환
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("배달 저장 실패 - productionId: {}, error: {}", 
                    context.getAttribute("productionId", String.class), errorMsg, e);
        }
        
        return result;
    }
    
    private void saveDelivery(
            DeliveryContext context,
            DeliveryResult result,
            Instant startedAt,
            Instant completedAt
    ) {
        String deliveryId = UUID.randomUUID().toString();
        String productionId = context.getAttribute("productionId", String.class);
        
        DeliveryEntity entity = DeliveryEntity.builder()
            .deliveryId(deliveryId)
            .productionId(productionId)
            .userId(context.getUserId())
            .deliveryType(context.getDeliveryType())
            .startedAt(startedAt)
            .completedAt(completedAt)
            .success(result.isSuccess())
            .errorMessage(result.getErrorMessage())
            .build();
        
        deliveryRepository.save(entity);
    }
}

