package org.example.sharedprompts.module.domain.production.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * S3 다운로드 서비스
 * 파일 다운로드를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3DownloadService {

    private final S3Client s3Client;

    @Value("${production.storage.s3.bucket}")
    private String bucket;

    /**
     * S3에서 파일을 읽어옵니다.
     */
    public byte[] download(String s3Key) {
        log.debug("Reading from S3 - bucket: {}, key: {}", bucket, s3Key);
        try {
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build());
            return responseBytes.asByteArray();
        } catch (Exception e) {
            log.error("S3 read failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 read failed: " + e.getMessage(), e);
        }
    }

    /**
     * S3에 파일이 존재하는지 확인합니다.
     */
    public boolean exists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("S3 exists check failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 exists check failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("S3 exists check failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 exists check failed: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     */
    public void delete(String s3Key) {
        log.info("Deleting from S3 - bucket: {}, key: {}", bucket, s3Key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
        } catch (Exception e) {
            log.error("S3 delete failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 delete failed: " + e.getMessage(), e);
        }
    }
}

