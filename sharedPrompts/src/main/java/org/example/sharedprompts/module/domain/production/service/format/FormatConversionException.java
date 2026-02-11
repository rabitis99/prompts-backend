package org.example.sharedprompts.module.domain.production.service.format;

public class FormatConversionException extends RuntimeException {

    public FormatConversionException(String message) {
        super(message);
    }

    public FormatConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
