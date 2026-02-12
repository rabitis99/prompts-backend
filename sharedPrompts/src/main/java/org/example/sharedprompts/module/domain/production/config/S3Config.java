package org.example.sharedprompts.module.domain.production.config;

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

    @Bean
    public S3Presigner s3Presigner(
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.credentials.access-key:}") String accessKey,
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.credentials.secret-key:}") String secretKey,
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.region.static:ap-northeast-2}") String region) {
        
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            // IAM Role 기반 인증 사용 (EC2/ECS 환경)
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

