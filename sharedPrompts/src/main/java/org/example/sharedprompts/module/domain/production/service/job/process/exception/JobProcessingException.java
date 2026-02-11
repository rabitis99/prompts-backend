package org.example.sharedprompts.module.domain.production.service.job.process.exception;

public class JobProcessingException extends RuntimeException {
    public JobProcessingException(String message) {
        super(message);
    }

    public JobProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

