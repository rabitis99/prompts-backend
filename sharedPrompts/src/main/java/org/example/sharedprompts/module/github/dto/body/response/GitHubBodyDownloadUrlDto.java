package org.example.sharedprompts.module.github.dto.body.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub 본문 다운로드 API 응답 DTO (JSON 반환 시).
 * Presigned URL을 담아 반환합니다.
 */
public record GitHubBodyDownloadUrlDto(
        @JsonProperty("download_url")
        String downloadUrl
) {}
