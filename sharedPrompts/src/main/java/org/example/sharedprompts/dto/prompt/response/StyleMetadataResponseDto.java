package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.domain.service.recommendation.RecommendationRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

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
    @JsonProperty("axis")
    private String axis;
    @JsonProperty("list_friendly")
    private boolean listFriendly;
    @JsonProperty("long_form_friendly")
    private boolean longFormFriendly;
    @JsonProperty("high_hallucination_risk")
    private boolean highHallucinationRisk;

    public static StyleMetadataResponseDto from(StyleType style, RecommendationRegistry recommendationRegistry) {
        return StyleMetadataResponseDto.builder()
                .style(style)
                .displayName(style.getDisplayName())
                .recommendedDomains(recommendationRegistry.getRecommendedDomainsForStyle(style).stream()
                        .map(TaskDomain::name)
                        .toList())
                .axis(style.getAxis().name())
                .listFriendly(style.isListFriendly())
                .longFormFriendly(style.isLongFormFriendly())
                .highHallucinationRisk(style.isHighHallucinationRisk())
                .build();
    }
}

