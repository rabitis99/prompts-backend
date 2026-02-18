package org.example.sharedprompts.module.domain.production.validation;

/**
 * 포맷 검증 유틸리티
 */
public class FormatValidator {
    
    /**
     * 문서 포맷 검증
     */
    public static void validateDocumentFormat(String format) {
        validateFormatNotNullOrBlank(format);
        if (!DocumentFormat.isSupported(format)) {
            throw new ValidationException(
                    "Unsupported document format: " + format + 
                    ". Supported formats: " + DocumentFormat.getSupportedFormatsString());
        }
    }
    
    /**
     * 텍스트 포맷 검증
     */
    public static void validateTextFormat(String format) {
        validateFormatNotNullOrBlank(format);
        if (!TextFormat.isSupported(format)) {
            throw new ValidationException(
                    "Unsupported text format: " + format + 
                    ". Supported formats: " + TextFormat.getSupportedFormatsString());
        }
    }
    
    /**
     * 포맷이 null이거나 blank가 아닌지 검증
     */
    public static void validateFormatNotNullOrBlank(String format) {
        if (format == null || format.isBlank()) {
            throw new ValidationException("format must not be blank");
        }
    }
}

