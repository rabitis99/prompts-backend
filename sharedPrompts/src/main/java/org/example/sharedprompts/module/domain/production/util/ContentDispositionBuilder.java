package org.example.sharedprompts.module.domain.production.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * RFC 6266 준수 Content-Disposition 헤더 빌더.
 * <p>
 * 기존 코드(여러 곳에 중복되어 있던 buildContentDisposition 로직)를 단일화합니다.
 */
public final class ContentDispositionBuilder {

    private ContentDispositionBuilder() {
    }

    /**
     * attachment 형태의 Content-Disposition을 안전하게 생성합니다.
     * <p>
     * - filename: ASCII-safe(호환성)
     * - filename*: UTF-8 인코딩(비 ASCII 지원)
     */
    public static String attachment(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }

        String sanitized = fileName.replaceAll("[\"\\r\\n]", "_")
                .replaceAll("[^\\x20-\\x7E]", "_");

        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s", sanitized, encoded);
    }
}



