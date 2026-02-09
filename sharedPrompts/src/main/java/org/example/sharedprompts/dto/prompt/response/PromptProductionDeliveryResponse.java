package org.example.sharedprompts.dto.prompt.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.module.domain.production.api.ProductionResult;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptProductionDeliveryResponse {
    
    private PromptResponseDto promptResult;
    private ProductionResult productionResult;
    private DeliveryResult deliveryResult;
    
    public static PromptProductionDeliveryResponse of(
            PromptResponseDto promptResult,
            ProductionResult productionResult,
            DeliveryResult deliveryResult
    ) {
        return PromptProductionDeliveryResponse.builder()
                .promptResult(promptResult)
                .productionResult(productionResult)
                .deliveryResult(deliveryResult)
                .build();
    }
}

