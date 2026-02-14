package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.StyleType;

/**
 * 간단한 StyleType 응답 DTO
 * <p>하위 리소스 엔드포인트에서 사용되는 간소화된 스타일 정보를 포함한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleStyleResponseDto {
    private StyleType style;
    @JsonProperty("display_name")
    private String displayName;

    public static SimpleStyleResponseDto from(StyleType style) {
        return SimpleStyleResponseDto.builder()
                .style(style)
                .displayName(style.getDisplayName())
                .build();
    }
}


