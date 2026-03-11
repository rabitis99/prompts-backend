package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.domain.service.recommendation.RecommendationRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;

import java.util.List;

/**
 * TaskDomain 메타데이터 응답 DTO
 * <p>도메인별 추천 StyleType과 ToneType 정보를 포함한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainMetadataResponseDto {
    private TaskDomain domain;
    @JsonProperty("display_name")
    private String displayName;
    @JsonProperty("recommended_styles")
    private List<String> recommendedStyles;
    @JsonProperty("recommended_tones")
    private List<String> recommendedTones;

    public static DomainMetadataResponseDto from(TaskDomain domain, RecommendationRegistry recommendationRegistry) {
        return DomainMetadataResponseDto.builder()
                .domain(domain)
                .displayName(domain.getDisplayName())
                .recommendedStyles(recommendationRegistry.getRecommendedStyles(domain).stream()
                        .map(StyleType::name)
                        .toList())
                .recommendedTones(recommendationRegistry.getRecommendedTones(domain).stream()
                        .map(ToneType::name)
                        .toList())
                .build();
    }
}

