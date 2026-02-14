package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;

import java.util.List;

/**
 * ToneType 메타데이터 응답 DTO
 * <p>톤별 추천 TaskDomain 정보를 포함한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToneMetadataResponseDto {
    private ToneType tone;
    @JsonProperty("display_name")
    private String displayName;
    @JsonProperty("recommended_domains")
    private List<String> recommendedDomains;

    public static ToneMetadataResponseDto from(ToneType tone) {
        return ToneMetadataResponseDto.builder()
                .tone(tone)
                .displayName(tone.getDisplayName())
                .recommendedDomains(tone.getRecommendedDomains().stream()
                        .map(TaskDomain::name)
                        .toList())
                .build();
    }
}

