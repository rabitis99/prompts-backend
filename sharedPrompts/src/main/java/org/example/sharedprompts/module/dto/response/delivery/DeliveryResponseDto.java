package org.example.sharedprompts.module.dto.response.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDto {
    
    private Long deliveryId;
    private boolean success;
    private String errorMessage;
    
    public static DeliveryResponseDto from(DeliveryResult result, Long deliveryId) {
        return DeliveryResponseDto.builder()
                .deliveryId(deliveryId)
                .success(result.isSuccess())
                .errorMessage(result.getErrorMessage())
                .build();
    }
}

