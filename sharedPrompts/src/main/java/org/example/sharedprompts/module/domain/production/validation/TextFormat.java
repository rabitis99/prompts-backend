package org.example.sharedprompts.module.domain.production.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 지원되는 텍스트 포맷 enum
 */
public enum TextFormat {
    MARKDOWN("markdown"),
    MD("md"),
    JSON("json"),
    TXT("txt"),
    HTML("html"),
    XML("xml"),
    CSV("csv"),
    PDF("pdf");
    
    private final String value;
    
    TextFormat(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 문자열로부터 TextFormat을 찾습니다 (대소문자 무시)
     * 
     * @param format 포맷 문자열
     * @return 일치하는 TextFormat, 없으면 null
     */
    public static TextFormat fromString(String format) {
        if (format == null) {
            return null;
        }
        String lowerFormat = format.toLowerCase();
        return Arrays.stream(values())
                .filter(f -> f.value.equals(lowerFormat))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 문자열이 지원되는 포맷인지 확인합니다 (대소문자 무시)
     * 
     * @param format 포맷 문자열
     * @return 지원되는 포맷이면 true
     */
    public static boolean isSupported(String format) {
        return fromString(format) != null;
    }
    
    /**
     * 지원되는 모든 포맷 값을 쉼표로 구분된 문자열로 반환합니다
     * 
     * @return "markdown, md, json, txt, html, xml, csv, pdf"
     */
    public static String getSupportedFormatsString() {
        return Arrays.stream(values())
                .map(TextFormat::getValue)
                .collect(Collectors.joining(", "));
    }
    
    /**
     * 지원되는 모든 포맷 값의 Set을 반환합니다
     * 
     * @return 지원되는 포맷 값들의 Set
     */
    public static Set<String> getSupportedFormats() {
        return Arrays.stream(values())
                .map(TextFormat::getValue)
                .collect(Collectors.toSet());
    }
}

