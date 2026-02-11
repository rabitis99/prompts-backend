package org.example.sharedprompts.module.domain.production.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.util.ContentTypeUtils;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArtifactMetadataHelper {

    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }

    public static String determineContentType(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "text/plain";
        }
        String fileName = extractFileName(filePath);
        if (fileName == null) {
            return "text/plain";
        }
        return ContentTypeUtils.guessContentType(fileName);
    }

    public static ArtifactType determineArtifactType(ProductionCommandType commandType) {
        return switch (commandType) {
            case TEXT, EMAIL, BLOG, DOCUMENT -> ArtifactType.TEXT;
            case IMAGE -> ArtifactType.IMAGE;
        };
    }

    public static StorageFormat determineStorageFormat(String filePath) {
        return (filePath != null && !filePath.isBlank()) 
                ? StorageFormat.FILE_PATH 
                : StorageFormat.INLINE_TEXT;
    }
}

