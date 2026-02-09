package org.example.sharedprompts.module.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.facade.PromptProductionDeliveryFacade;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.dto.request.delivery.DeliveryRequestDto;
import org.example.sharedprompts.module.dto.response.delivery.DeliveryResponseDto;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.repository.DeliveryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class DeliveryController {
    
    private final PromptProductionDeliveryFacade productionDeliveryFacade;
    private final DeliveryRepository deliveryRepository;
    
    @PostMapping("/production/{productionId}/delivery")
    public ResponseEntity<CustomResponse<DeliveryResponseDto>> executeDelivery(
            @PathVariable String productionId,
            @Valid @RequestBody DeliveryRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        DeliveryResult result = productionDeliveryFacade.executeDelivery(
            productionId,
            request.toDeliveryContext(authUser.getId())
        );
        
        // DeliveryCoordinator가 저장한 결과에서 최신 deliveryId 조회
        Long deliveryId = deliveryRepository.findFirstByProductionIdOrderByCreatedAtDesc(productionId)
                .map(entity -> entity.getId())
                .orElse(null);
        
        DeliveryResponseDto response = DeliveryResponseDto.from(result, deliveryId);
        return CustomResponseHelper.ok(response);
    }
}

