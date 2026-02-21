package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionS3Properties;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "storage.lifecycle.enabled", havingValue = "true")
@ConditionalOnStorageType("S3")
@Slf4j
public class StorageLifecycleService {

    private final S3Client s3Client;
    private final ProductionS3Properties productionS3Properties;
    private final int glacierDays;
    private final int deepArchiveDays;
    // NOTE: AtomicBoolean은 단일 JVM 인스턴스 내에서만 중복 실행을 방지합니다.
    // 다중 인스턴스 환경에서는 분산 락(Redis, DB lock 등)으로 교체가 필요합니다.
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final String LIFECYCLE_TAG_KEY = "lifecycle-status";
    private static final String TAG_GLACIER = "glacier";
    private static final String TAG_DEEP_ARCHIVE = "deep-archive";

    // P2-6: lifecycle 관련 설정을 @Value로 읽으며 기본값 의존
    // 환경변수 미설정 시 기본값으로 동작하여 의도치 않은 lifecycle 전환 가능
    // TODO: lifecycle 관련 설정을 @ConfigurationProperties로 이동하고 @Validated 적용
    public StorageLifecycleService(
            S3Client s3Client,
            ProductionS3Properties productionS3Properties,
            @Value("${storage.lifecycle.glacier-days:30}") int glacierDays,
            @Value("${storage.lifecycle.deep-archive-days:90}") int deepArchiveDays) {
        if (glacierDays <= 0 || deepArchiveDays <= 0) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Lifecycle days must be positive");
        }
        if (glacierDays >= deepArchiveDays) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "glacierDays (" + glacierDays + ") must be less than deepArchiveDays (" + deepArchiveDays + ")");
        }
        this.s3Client = s3Client;
        this.productionS3Properties = productionS3Properties;
        this.glacierDays = glacierDays;
        this.deepArchiveDays = deepArchiveDays;
    }

    public int processLifecycleTransitions() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Storage lifecycle processing is already running - skipping this invocation");
            return 0;
        }
        String bucket = productionS3Properties.getBucket();
        String prefix = productionS3Properties.getPrefix();
        log.info("Starting storage lifecycle processing - bucket: {}, prefix: {}", bucket, prefix);
        int processedCount = 0;

        try {
            Instant now = Instant.now();
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix + "/")
                    .build();

            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : response.contents()) {
                    if (processObject(s3Object, now)) {
                        processedCount++;
                    }
                }

                listRequest = listRequest.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();

            } while (response.isTruncated());

            log.info("Storage lifecycle processing completed - processed: {} objects", processedCount);

        } catch (Exception e) {
            log.error("Storage lifecycle processing failed", e);
        } finally {
            running.set(false);
        }

        return processedCount;
    }

    private boolean processObject(S3Object s3Object, Instant now) {
        try {
            Instant lastModified = s3Object.lastModified();
            long ageInDays = ChronoUnit.DAYS.between(lastModified, now);

            if (ageInDays >= deepArchiveDays) {
                return tagObject(s3Object.key(), TAG_DEEP_ARCHIVE);
            } else if (ageInDays >= glacierDays) {
                return tagObject(s3Object.key(), TAG_GLACIER);
            }

            return false;
        } catch (Exception e) {
            log.warn("Failed to process object - key: {}", s3Object.key(), e);
            return false;
        }
    }

    private boolean tagObject(String key, String lifecycleTag) {
        try {
            Map<String, String> existingTags = getObjectTags(key);
            String currentTag = existingTags.get(LIFECYCLE_TAG_KEY);

            if (lifecycleTag.equals(currentTag)) {
                return false;
            }

            existingTags.put(LIFECYCLE_TAG_KEY, lifecycleTag);

            List<Tag> tagList = existingTags.entrySet().stream()
                    .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                    .toList();

            s3Client.putObjectTagging(PutObjectTaggingRequest.builder()
                    .bucket(productionS3Properties.getBucket())
                    .key(key)
                    .tagging(Tagging.builder().tagSet(tagList).build())
                    .build());

            log.info("Object tagged for lifecycle transition - key: {}, tag: {}", key, lifecycleTag);
            return true;
        } catch (Exception e) {
            log.warn("Failed to tag object - key: {}, tag: {}", key, lifecycleTag, e);
            return false;
        }
    }

    private Map<String, String> getObjectTags(String key) {
        try {
            GetObjectTaggingResponse response = s3Client.getObjectTagging(
            GetObjectTaggingRequest.builder()
                    .bucket(productionS3Properties.getBucket())
                    .key(key)
                    .build());

            HashMap<String, String> tags = new HashMap<>();
            response.tagSet().forEach(tag -> tags.put(tag.key(), tag.value()));
            return tags;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
