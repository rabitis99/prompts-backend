package org.example.sharedprompts.module.domain.production.service.ai.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Leonardo AI 설정 Properties
 */
@Getter
@Setter
@Component
@Validated
@ConditionalOnProperty(name = "ai.provider.leonardo.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "ai.provider.leonardo")
public class LeonardoProperties {

    /**
     * Leonardo API Secret Key (필수)
     */
    @NotBlank(message = "Leonardo API Secret Key는 필수입니다 (ai.provider.leonardo.secret-key)")
    private String secretKey;

    /**
     * Leonardo API Base URL (기본값: https://cloud.leonardo.ai/api/rest/v1)
     */
    private String baseUrl = "https://cloud.leonardo.ai/api/rest/v1";

    /**
     * 기본 모델 ID
     */
    private String defaultModelId = "7b592283-e8a7-4c5a-9ba6-d18c31f258b9";

    /**
     * 요청 타임아웃 (초, 기본값: 30)
     */
    private int timeoutSeconds = 30;

    /**
     * 최대 재시도 횟수 (기본값: 3)
     */
    private int maxRetries = 3;

    /**
     * 재시도 초기 지연 시간 (밀리초, 기본값: 1000)
     */
    private long initialRetryDelayMs = 1000L;

    /**
     * 이미지 생성 상태 폴링 최대 대기 시간 (초, 기본값: 60)
     */
    private int maxPollingWaitSeconds = 60;

    /**
     * 이미지 생성 상태 폴링 간격 (초, 기본값: 2)
     */
    private int pollingIntervalSeconds = 2;

    /**
     * Alchemy 기능 활성화 여부 (기본값: false)
     * Leonardo.AI의 Alchemy 기능은 이미지 품질을 향상시킵니다.
     */
    private Boolean alchemy = false;

    /**
     * 대비 조정 값 (기본값: null, 선택적)
     * 이미지의 대비를 조정합니다. 일반적으로 1.0 ~ 5.0 범위입니다.
     */
    private Double contrast;

    /**
     * 스타일 UUID (기본값: null, 선택적)
     * 특정 스타일을 적용하려면 스타일 UUID를 지정합니다.
     * 예: "111dc692-d470-4eec-b791-3475abac4c46"
     */
    private String styleUUID;

    /**
     * Ultra 모드 활성화 여부 (기본값: false)
     * Leonardo.AI의 Ultra 모드는 고품질 이미지 생성을 제공합니다.
     */
    private Boolean ultra = false;
}

