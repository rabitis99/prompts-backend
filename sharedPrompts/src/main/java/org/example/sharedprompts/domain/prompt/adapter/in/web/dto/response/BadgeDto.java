package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 배지 DTO — UX에 표시될 배지 정보만 담는다.
 */
public record BadgeDto(
        String code,
        @JsonProperty("display_name")
        String displayName
) {
}

