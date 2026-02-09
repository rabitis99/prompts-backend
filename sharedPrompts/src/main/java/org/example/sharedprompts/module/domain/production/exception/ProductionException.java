package org.example.sharedprompts.module.domain.production.exception;

public class ProductionException extends RuntimeException {
    public ProductionException(String message) {
        super(message);
    }

    public ProductionException(String message, Throwable cause) {
        super(message, cause);
    }
}

