package org.example.sharedprompts.module.domain.production.util.s3;

import org.example.sharedprompts.module.domain.production.model.storage.S3Path;
import org.example.sharedprompts.module.domain.production.util.S3PathParser;

import java.util.Optional;

public final class S3PathUtils {
    
    private S3PathUtils() {
    }
    
    public static String extractKey(String filePathOrKey) {
        return S3PathParser.extractKey(filePathOrKey);
    }
    
    public static S3Path parse(String filePathOrKey, String defaultBucket) {
        return S3PathParser.parse(filePathOrKey, defaultBucket);
    }
    
    public static Optional<S3Path> tryParse(String filePathOrKey, String defaultBucket) {
        return S3PathParser.tryParse(filePathOrKey, defaultBucket);
    }
}

