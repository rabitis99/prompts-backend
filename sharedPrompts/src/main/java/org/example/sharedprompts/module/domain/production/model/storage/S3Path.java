package org.example.sharedprompts.module.domain.production.model.storage;

/**
 * S3 경로 값 객체(VO).
 * <p>
 * - bucket은 {@code s3://bucket/key} 형식에서만 존재할 수 있습니다.
 * - key는 S3 객체 키이며, 비어있을 수 없습니다.
 */
public record S3Path(String bucket, String key) {

    public S3Path {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key must not be blank");
        }
        key = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
        if (key.isBlank()) {
            throw new IllegalArgumentException("S3 key must not be blank after normalization");
        }
    }

    public boolean hasBucket() {
        return bucket != null && !bucket.isBlank();
    }

    /**
     * Returns the segment after the last '/'. After constructor normalization, key does not end with '/'.
     */
    public String fileName() {
        int lastSlash = key.lastIndexOf('/');
        return (lastSlash >= 0 && lastSlash < key.length() - 1) ? key.substring(lastSlash + 1) : key;
    }
}



