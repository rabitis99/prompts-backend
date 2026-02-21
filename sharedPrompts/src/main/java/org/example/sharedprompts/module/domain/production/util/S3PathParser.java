package org.example.sharedprompts.module.domain.production.util;

import org.example.sharedprompts.module.domain.production.model.storage.S3Path;

import java.util.Optional;

/**
 * S3 경로 파서.
 * <p>
 * 지원 형식:
 * - {@code s3://bucket/key}
 * - {@code key} (defaultBucket가 제공된 경우에만 bucket을 채워서 반환)
 */
public final class S3PathParser {

    private static final String S3_PREFIX = "s3://";

    private S3PathParser() {
    }

    /**
     * 파일 경로에서 S3 객체 key만 추출합니다.
     * - s3://bucket/key -> key
     * - key -> key
     */
    public static String extractKey(String filePathOrKey) {
        if (filePathOrKey == null || filePathOrKey.isBlank()) {
            throw new IllegalArgumentException("filePathOrKey must not be blank");
        }
        if (filePathOrKey.startsWith(S3_PREFIX)) {
            S3Path parsed = parseS3Uri(filePathOrKey);
            return parsed.key();
        }
        return filePathOrKey;
    }

    /**
     * S3 경로를 파싱하여 bucket/key를 반환합니다.
     * s3:// 접두사가 없는 경우 defaultBucket가 비어있지 않으면 bucket을 채웁니다.
     */
    public static S3Path parse(String filePathOrKey, String defaultBucket) {
        return tryParse(filePathOrKey, defaultBucket)
                .orElseThrow(() -> new IllegalArgumentException("Invalid S3 path: " + filePathOrKey));
    }

    /**
     * 파싱 실패 시 empty를 반환합니다.
     */
    public static Optional<S3Path> tryParse(String filePathOrKey, String defaultBucket) {
        if (filePathOrKey == null || filePathOrKey.isBlank()) {
            return Optional.empty();
        }

        if (filePathOrKey.startsWith(S3_PREFIX)) {
            try {
                return Optional.of(parseS3Uri(filePathOrKey));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        if (defaultBucket == null || defaultBucket.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new S3Path(defaultBucket, filePathOrKey));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static S3Path parseS3Uri(String s3Uri) {
        String withoutPrefix = s3Uri.substring(S3_PREFIX.length());
        int slashIndex = withoutPrefix.indexOf('/');
        if (slashIndex <= 0 || slashIndex >= withoutPrefix.length() - 1) {
            throw new IllegalArgumentException("Invalid S3 URI (missing bucket or key): " + s3Uri);
        }
        String bucket = withoutPrefix.substring(0, slashIndex);
        String key = withoutPrefix.substring(slashIndex + 1);
        if (key.isBlank()) {
            throw new IllegalArgumentException("Invalid S3 URI (blank key): " + s3Uri);
        }
        return new S3Path(bucket, key);
    }
}



