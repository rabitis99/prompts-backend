package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.ToneType;

/**
 * 간단한 ToneType 응답 DTO
 * <p>하위 리소스 엔드포인트에서 사용되는 간소화된 톤 정보를 포함한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleToneResponseDto {
    private ToneType tone;
    @JsonProperty("display_name")
    private String displayName;

    public static SimpleToneResponseDto from(ToneType tone) {
        return SimpleToneResponseDto.builder()
                .tone(tone)
                .displayName(tone.getDisplayName())
                .build();
    }
}


