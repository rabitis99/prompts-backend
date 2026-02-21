package org.example.sharedprompts.module.domain.production.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Optional;

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
     * 메모리에 로드할 수 있는 최대 파일 크기 (바이트)
     * 기본값: 100MB (104,857,600 bytes)
     * 이 크기를 초과하는 파일은 다운로드하지 않습니다.
     */
    @Value("${production.storage.s3.max-download-size-bytes:104857600}")
    private long maxDownloadSizeBytes;

    /**
     * S3에서 파일을 읽어옵니다.
     * 파일 크기가 maxDownloadSizeBytes를 초과하면 예외를 발생시킵니다.
     * 대용량 파일의 경우 스트리밍 방식이나 Presigned URL을 사용하는 것을 권장합니다.
     *
     * @param s3Key S3 객체 키
     * @return 파일 내용 (byte 배열)
     * @throws S3StorageException 파일 크기가 제한을 초과하거나 다운로드 실패 시
     */
    public byte[] download(String s3Key) {
        log.debug("Reading from S3 - bucket: {}, key: {}", bucket, s3Key);
        try {
            // 먼저 파일 크기를 확인
            HeadObjectResponse headResponse = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build());

            Long contentLength = headResponse.contentLength();
            if (contentLength != null && contentLength > maxDownloadSizeBytes) {
                String errorMessage = String.format(
                        "File size (%d bytes) exceeds maximum allowed size (%d bytes) for in-memory download. " +
                        "Consider using streaming or presigned URL for large files. Key: %s",
                        contentLength, maxDownloadSizeBytes, s3Key);
                log.error(errorMessage);
                throw new S3StorageException(errorMessage);
            }

            // 파일 크기가 허용 범위 내이면 다운로드
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build());
            
            byte[] result = responseBytes.asByteArray();
            log.debug("S3 read completed - bucket: {}, key: {}, size: {} bytes", bucket, s3Key, result.length);
            return result;
        } catch (S3StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("S3 read failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 read failed: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일의 일부만 읽어옵니다 (Range 요청).
     * 이미지 포맷 감지 등 파일의 시작 부분만 필요한 경우에 사용합니다.
     *
     * @param s3Key S3 객체 키
     * @param startByteRange 시작 바이트 위치 (0-based, inclusive)
     * @param endByteRange 종료 바이트 위치 (inclusive)
     * @return 파일의 일부 내용 (byte 배열)
     * @throws IllegalArgumentException 잘못된 byte range 입력 시
     * @throws S3StorageException 다운로드 실패 시
     */
    public byte[] downloadRange(String s3Key, long startByteRange, long endByteRange) {
        log.debug("Reading range from S3 - bucket: {}, key: {}, range: {}-{}", bucket, s3Key, startByteRange, endByteRange);
        
        // 입력 검증: try 블록 밖에서 수행하여 IllegalArgumentException이 S3StorageException으로 래핑되지 않도록 함
        if (startByteRange < 0 || endByteRange < startByteRange) {
            throw new IllegalArgumentException(
                    String.format("Invalid byte range: start=%d, end=%d", startByteRange, endByteRange));
        }

        try {
            String range = String.format("bytes=%d-%d", startByteRange, endByteRange);
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .range(range)
                            .build());

            byte[] result = responseBytes.asByteArray();
            log.debug("S3 range read completed - bucket: {}, key: {}, range: {}-{}, size: {} bytes",
                    bucket, s3Key, startByteRange, endByteRange, result.length);
            return result;
        } catch (Exception e) {
            log.error("S3 range read failed - bucket: {}, key: {}, range: {}-{}", bucket, s3Key, startByteRange, endByteRange, e);
            throw new S3StorageException("S3 range read failed: " + e.getMessage(), e);
        }
    }

    /**
     * S3 객체의 Content-Length(바이트)를 반환합니다.
     * 객체가 없거나 메타데이터 조회에 실패하면 빈 Optional을 반환합니다.
     */
    public Optional<Long> getContentLength(String s3Key) {
        try {
            HeadObjectResponse headResponse = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build());
            return Optional.ofNullable(headResponse.contentLength());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.warn("S3 headObject failed - bucket: {}, key: {}", bucket, s3Key, e);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("S3 headObject failed - bucket: {}, key: {}", bucket, s3Key, e);
            return Optional.empty();
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

}

