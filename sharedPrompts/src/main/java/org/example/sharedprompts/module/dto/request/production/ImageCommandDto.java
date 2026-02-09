package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageCommandDto implements CommandDto {
    @NotNull(message = "프롬프트를 입력해주세요.")
    private String prompt;
    
    @NotNull(message = "너비를 입력해주세요.")
    @Min(value = 1, message = "너비는 1 이상이어야 합니다.")
    private Integer width;
    
    @NotNull(message = "높이를 입력해주세요.")
    @Min(value = 1, message = "높이는 1 이상이어야 합니다.")
    private Integer height;
}

