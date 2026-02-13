package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.List;

/**
 * StyleType 메타데이터 응답 DTO
 * <p>스타일별 추천 TaskDomain 정보를 포함한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleMetadataResponseDto {
    private StyleType style;
    @JsonProperty("display_name")
    private String displayName;
    @JsonProperty("recommended_domains")
    private List<String> recommendedDomains;

    public static StyleMetadataResponseDto from(StyleType style) {
        return StyleMetadataResponseDto.builder()
                .style(style)
                .displayName(style.getDisplayName())
                .recommendedDomains(style.getRecommendedDomains().stream()
                        .map(TaskDomain::name)
                        .toList())
                .build();
    }
}

