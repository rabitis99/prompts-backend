package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인기 태그 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularTagDto {

    /**
     * 태그 이름
     */
    @JsonProperty("name")
    private String name;

    /**
     * 사용 횟수
     */
    @JsonProperty("count")
    private Long count;
}
