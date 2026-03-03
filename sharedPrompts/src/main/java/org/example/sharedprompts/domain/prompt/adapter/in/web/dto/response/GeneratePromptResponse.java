package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeneratePromptResponse(
        Long id,
        String title,
        String content,
        @JsonProperty("quality_badges")
        List<BadgeDto> qualityBadges
) {
}
