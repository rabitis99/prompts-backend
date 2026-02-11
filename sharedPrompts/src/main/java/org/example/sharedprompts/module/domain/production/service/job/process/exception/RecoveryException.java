package org.example.sharedprompts.module.domain.production.service.job.process.exception;

public class RecoveryException extends JobProcessingException {
    public RecoveryException(String message) {
        super(message);
    }

    public RecoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

