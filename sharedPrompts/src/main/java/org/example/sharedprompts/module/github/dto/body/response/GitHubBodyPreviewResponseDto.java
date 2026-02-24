package org.example.sharedprompts.module.github.dto.body.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub 본문 미리보기 API 응답 DTO.
 * S3에서 읽어온 마크다운 본문을 그대로 반환합니다.
 */
public record GitHubBodyPreviewResponseDto(
        @JsonProperty("markdown")
        String markdown
) {}
