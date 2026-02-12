package org.example.sharedprompts.module.domain.production.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${spring.cloud.aws.credentials.access-key:}") String accessKey,
            @Value("${spring.cloud.aws.credentials.secret-key:}") String secretKey,
            @Value("${spring.cloud.aws.region.static:ap-northeast-2}") String region) {
        
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            if (!accessKey.isEmpty() || !secretKey.isEmpty()) {
                log.warn("S3 credentials partially configured - falling back to IAM Role authentication");
            }
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .build();
        } else {
            // Access Key 기반 인증 사용
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
    }
}
