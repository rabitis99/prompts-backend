package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

/**
 * 간단한 TaskDomain 응답 DTO
 * <p>하위 리소스 엔드포인트에서 사용되는 간소화된 도메인 정보를 포함한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleDomainResponseDto {
    private TaskDomain domain;
    @JsonProperty("display_name")
    private String displayName;

    public static SimpleDomainResponseDto from(TaskDomain domain) {
        return SimpleDomainResponseDto.builder()
                .domain(domain)
                .displayName(domain.getDisplayName())
                .build();
    }
}


