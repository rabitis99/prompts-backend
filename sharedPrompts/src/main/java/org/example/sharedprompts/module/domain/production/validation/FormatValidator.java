package org.example.sharedprompts.module.domain.production.validation;

import java.util.Set;

public class FormatValidator {
    
    private static final Set<String> DOCUMENT_FORMATS = Set.of(
            "markdown", "md", "json", "txt", "html", "xml", "csv", "pdf"
    );
    
    private static final Set<String> TEXT_FORMATS = Set.of(
            "markdown", "md", "json", "txt", "html", "xml", "csv", "pdf"
    );
    
    public static void validateDocumentFormat(String format) {
        String lowerFormat = format.toLowerCase();
        if (!DOCUMENT_FORMATS.contains(lowerFormat)) {
            throw new ValidationException(
                    "Unsupported document format: " + format + 
                    ". Supported formats: markdown, md, json, txt, html, xml, csv, pdf");
        }
    }
    
    public static void validateTextFormat(String format) {
        String lowerFormat = format.toLowerCase();
        if (!TEXT_FORMATS.contains(lowerFormat)) {
            throw new ValidationException(
                    "Unsupported text format: " + format + 
                    ". Supported formats: markdown, md, json, txt, html, xml, csv, pdf");
        }
    }
    
    public static void validateFormatNotNullOrBlank(String format) {
        if (format == null || format.isBlank()) {
            throw new ValidationException("format must not be blank");
        }
    }
}

