package org.example.sharedprompts.module.domain.production.config;

import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnStorageType("S3")
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);
    private static final String DEFAULT_REGION = "ap-northeast-2";

    @PostConstruct
    public void init() {
        log.info("✓ S3Config is active - S3 storage type is configured");
    }

    /**
     * S3 클라이언트 빈. 애플리케이션 종료 시 Spring이 destroyMethod="close"로 자동 정리합니다.
     * DEPLOYMENT_ISSUES 3.1: 리소스 정리는 close() 호출로 보장됨.
     */
    @Bean(destroyMethod = "close")
    public S3Client s3Client(
            @Value("${spring.cloud.aws.credentials.access-key:${AWS_ACCESS_KEY_ID:}}") String accessKey,
            @Value("${spring.cloud.aws.credentials.secret-key:${AWS_SECRET_ACCESS_KEY:}}") String secretKey,
            @Value("${spring.cloud.aws.region.static:${AWS_REGION:ap-northeast-2}}") String region) {
        
        try {
            CredentialsConfig credentials = normalizeCredentials(accessKey, secretKey, region);
            log.info("Creating S3Client bean - region: {}, hasAccessKey: {}, hasSecretKey: {}", 
                    credentials.region(), !credentials.accessKey().isEmpty(), !credentials.secretKey().isEmpty());
            
            Region awsRegion = parseRegion(credentials.region());
            StaticCredentialsProvider credentialsProvider = createCredentialsProviderIfAvailable(
                    credentials.accessKey(), credentials.secretKey());
            
            var builder = S3Client.builder().region(awsRegion);
            if (credentialsProvider != null) {
                builder.credentialsProvider(credentialsProvider);
                log.info("S3Client created with Access Key authentication");
            } else {
                log.info("S3Client created with IAM Role authentication");
            }
            
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create S3Client bean. This will prevent S3StorageStrategy from being created.", e);
            throw new IllegalStateException("Failed to create S3Client: " + e.getMessage(), e);
        }
    }

    /**
     * S3 Presigner 빈. 애플리케이션 종료 시 Spring이 destroyMethod="close"로 자동 정리합니다.
     */
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${spring.cloud.aws.credentials.access-key:${AWS_ACCESS_KEY_ID:}}") String accessKey,
            @Value("${spring.cloud.aws.credentials.secret-key:${AWS_SECRET_ACCESS_KEY:}}") String secretKey,
            @Value("${spring.cloud.aws.region.static:${AWS_REGION:ap-northeast-2}}") String region) {
        
        try {
            CredentialsConfig credentials = normalizeCredentials(accessKey, secretKey, region);
            log.info("Creating S3Presigner bean - region: {}, hasAccessKey: {}, hasSecretKey: {}", 
                    credentials.region(), !credentials.accessKey().isEmpty(), !credentials.secretKey().isEmpty());
            
            Region awsRegion = parseRegion(credentials.region());
            StaticCredentialsProvider credentialsProvider = createCredentialsProviderIfAvailable(
                    credentials.accessKey(), credentials.secretKey());
            
            var builder = S3Presigner.builder().region(awsRegion);
            if (credentialsProvider != null) {
                builder.credentialsProvider(credentialsProvider);
                log.info("S3Presigner created with Access Key authentication");
            } else {
                log.info("S3Presigner created with IAM Role authentication");
            }
            
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create S3Presigner bean. This will prevent S3StorageStrategy from being created.", e);
            throw new IllegalStateException("Failed to create S3Presigner: " + e.getMessage(), e);
        }
    }

    /**
     * 설정값을 정규화하고 검증합니다.
     */
    private CredentialsConfig normalizeCredentials(String accessKey, String secretKey, String region) {
        String normalizedAccessKey = accessKey != null ? accessKey.trim() : "";
        String normalizedSecretKey = secretKey != null ? secretKey.trim() : "";
        String normalizedRegion = region != null ? region.trim() : DEFAULT_REGION;
        
        // 부분적으로만 설정된 경우 경고
        if ((normalizedAccessKey.isEmpty() && !normalizedSecretKey.isEmpty()) ||
            (!normalizedAccessKey.isEmpty() && normalizedSecretKey.isEmpty())) {
            log.warn("S3 credentials partially configured - falling back to IAM Role authentication");
        }
        
        return new CredentialsConfig(normalizedAccessKey, normalizedSecretKey, normalizedRegion);
    }

    /**
     * AWS Region 문자열을 Region 객체로 변환합니다.
     * 유효하지 않은 경우 기본값을 반환합니다.
     */
    private Region parseRegion(String region) {
        try {
            return Region.of(region);
        } catch (IllegalArgumentException e) {
            log.error("Invalid AWS region: '{}'. Using default region: {}", region, DEFAULT_REGION, e);
            return Region.AP_NORTHEAST_2;
        }
    }

    /**
     * Access Key와 Secret Key가 모두 제공된 경우에만 CredentialsProvider를 생성합니다.
     * 그렇지 않으면 null을 반환하여 IAM Role 인증을 사용합니다.
     */
    private StaticCredentialsProvider createCredentialsProviderIfAvailable(String accessKey, String secretKey) {
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            return null;
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
    }

    /**
     * 인증 정보 설정을 담는 레코드
     */
    private record CredentialsConfig(String accessKey, String secretKey, String region) {
    }
}
