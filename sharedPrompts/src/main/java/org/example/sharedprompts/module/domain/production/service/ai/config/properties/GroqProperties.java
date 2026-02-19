package org.example.sharedprompts.module.domain.production.service.ai.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Groq AI 설정 Properties
 * 
 * <p>@ConfigurationPropertiesScan을 통해 자동으로 스캔되어 빈으로 등록됩니다.
 * SharedPromptsApplication에 @ConfigurationPropertiesScan이 선언되어 있습니다.
 */
@Getter
@Setter
@ToString(exclude = "secretKey")
@Validated
@ConditionalOnProperty(name = "ai.provider.groq.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "ai.provider.groq")
public class GroqProperties {

    /**
     * Groq API Secret Key (필수)
     */
    @NotBlank(message = "Groq API Secret Key는 필수입니다 (ai.provider.groq.secret-key)")
    private String secretKey;

    /**
     * Groq API Base URL (기본값: https://api.groq.com/openai/v1)
     */
    private String baseUrl = "https://api.groq.com/openai/v1";

    /**
     * 기본 모델 ID (기본값: llama-3.3-70b-versatile)
     */
    private String defaultModelId = "llama-3.3-70b-versatile";

    /**
     * 요청 타임아웃 (초, 기본값: 30)
     */
    @Min(value = 1, message = "타임아웃은 1초 이상이어야 합니다")
    private int timeoutSeconds = 30;

    /**
     * 최대 재시도 횟수 (기본값: 3)
     */
    @Min(value = 0, message = "최대 재시도 횟수는 0 이상이어야 합니다")
    private int maxRetries = 3;

    /**
     * 재시도 초기 지연 시간 (밀리초, 기본값: 1000)
     */
    @Min(value = 0, message = "초기 재시도 지연 시간은 0 이상이어야 합니다")
    private long initialRetryDelayMs = 1000L;
}

