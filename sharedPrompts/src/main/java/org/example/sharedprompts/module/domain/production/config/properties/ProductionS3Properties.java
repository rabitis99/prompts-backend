package org.example.sharedprompts.module.domain.production.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * S3 설정(Properties) 바인딩 + 검증.
 * <p>
 * - production.storage.type = S3인 경우 bucket은 반드시 필요합니다.
 */
@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties(prefix = "production.storage.s3")
public class ProductionS3Properties {

    @NotBlank(message = "S3 bucket은 필수입니다 (production.storage.s3.bucket)")
    private String bucket;

    /**
     * S3 key prefix (optional)
     */
    private String prefix = "production";

    /**
     * presigned url 설정 (기존 yml 구조: production.storage.s3.presigned-url.ttl-seconds)
     */
    private PresignedUrl presignedUrl = new PresignedUrl();

    /**
     * AWS SDK(Client) timeout 설정 (선택).
     * <p>
     * - 미설정 시 AWS SDK 기본값 사용
     * - 배포 안정성을 위해 "무한 대기"를 방지하는 가드로 사용
     */
    private Client client = new Client();

    @Getter
    @Setter
    @ToString
    public static class PresignedUrl {

        @Min(value = 1, message = "presigned URL ttl-seconds는 1 이상이어야 합니다")
        private int ttlSeconds = 300;
    }

    @Getter
    @Setter
    @ToString
    public static class Client {

        /**
         * 한 번의 API 호출 전체 타임아웃(초). (optional)
         */
        @Min(value = 1, message = "client.api-call-timeout-seconds는 1 이상이어야 합니다")
        private Integer apiCallTimeoutSeconds = 30;

        /**
         * 재시도 1회 시도(Attempt) 타임아웃(초). (optional)
         */
        @Min(value = 1, message = "client.api-call-attempt-timeout-seconds는 1 이상이어야 합니다")
        private Integer apiCallAttemptTimeoutSeconds = 10;
    }
}


