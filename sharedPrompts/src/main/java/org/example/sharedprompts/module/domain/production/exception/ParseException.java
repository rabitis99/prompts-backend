package org.example.sharedprompts.module.domain.production.exception;

/**
 * 파싱 관련 공용 예외
 */
public class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

