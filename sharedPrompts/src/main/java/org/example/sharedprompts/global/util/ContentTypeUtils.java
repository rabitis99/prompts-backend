package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Content-Type 추측 유틸리티 클래스
 * 
 * 파일 확장자 기반으로 MIME 타입을 추측하는 기능을 제공합니다.
 * 모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentTypeUtils {

    /**
     * 파일명의 확장자를 기반으로 Content-Type을 추측합니다.
     * 
     * @param fileName 파일명 (확장자 포함)
     * @return 추측된 Content-Type, 추측 불가능한 경우 "application/octet-stream"
     */
    public static String guessContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lower = fileName.toLowerCase();
        
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "text/markdown";
        }
        if (lower.endsWith(".csv")) {
            return "text/csv";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain";
        }
        
        return "application/octet-stream";
    }
}

