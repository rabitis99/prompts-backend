package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 품질 배지 응답 DTO
 */
public record BadgeDto(

        String code,

        @JsonProperty("display_name")
        String displayName

) {
}