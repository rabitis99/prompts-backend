package org.example.sharedprompts.dto.admin.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;

import java.time.LocalDateTime;

/**
 * Rate Limit 로그 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitLogResponseDto {

    private Long id;

    @JsonProperty("rule_name")
    private String ruleName;

    @JsonProperty("rate_limit_key")
    private String rateLimitKey;

    @JsonProperty("current_count")
    private Long currentCount;

    @JsonProperty("capacity")
    private Long capacity;

    @JsonProperty("retry_after")
    private Long retryAfter;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("client_ip")
    private String clientIp;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("http_method")
    private String httpMethod;

    @JsonProperty("rate_limit_type")
    private RateLimitType rateLimitType;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static RateLimitLogResponseDto from(RateLimitLog log) {
        return RateLimitLogResponseDto.builder()
                .id(log.getId())
                .ruleName(log.getRuleName())
                .rateLimitKey(log.getRateLimitKey())
                .currentCount(log.getCurrentCount())
                .capacity(log.getCapacity())
                .retryAfter(log.getRetryAfter())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .clientIp(log.getClientIp())
                .uri(log.getUri())
                .httpMethod(log.getHttpMethod())
                .rateLimitType(log.getRateLimitType())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

