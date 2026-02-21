package org.example.sharedprompts.module.domain.production.config.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionS3Properties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * S3 연결 상태를 Actuator Health에 노출합니다.
 * production.storage.type=S3 일 때만 등록됩니다.
 *
 * <p>DEPLOYMENT_ISSUES 3.1: S3 클라이언트 리소스 관리 - Health check를 통한 연결 상태 모니터링
 */
@Component
@ConditionalOnStorageType("S3")
@RequiredArgsConstructor
@Slf4j
public class S3HealthIndicator extends AbstractHealthIndicator {

    private final S3Client s3Client;
    private final ProductionS3Properties s3Properties;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String bucket = s3Properties.getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            builder.up()
                    .withDetail("bucket", bucket)
                    .withDetail("message", "S3 bucket accessible");
        } catch (Exception e) {
            log.warn("S3 health check failed - bucket: {}", bucket, e);
            builder.down()
                    .withDetail("bucket", bucket)
                    .withException(e);
        }
    }
}
