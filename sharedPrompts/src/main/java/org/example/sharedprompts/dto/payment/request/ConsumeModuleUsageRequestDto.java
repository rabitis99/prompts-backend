package org.example.sharedprompts.dto.payment.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeModuleUsageRequestDto {

    @NotBlank(message = "moduleType은 필수입니다.")
    private String moduleType;
}
