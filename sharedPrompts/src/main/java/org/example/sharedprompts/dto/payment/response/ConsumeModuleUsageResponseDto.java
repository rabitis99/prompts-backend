package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeModuleUsageResponseDto {

    @JsonProperty("module_type")
    private ModuleType moduleType;

    private int limit;

    @JsonProperty("consumption_amount")
    private int consumptionAmount;

    @JsonProperty("used_today")
    private int usedToday;

    private int remaining;
}
