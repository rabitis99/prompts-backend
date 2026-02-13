package org.example.sharedprompts.module.domain.production.service.image;

public record ImageMetadata(
        int width,
        int height,
        long fileSize,
        String format,
        String mimeType
) {}
