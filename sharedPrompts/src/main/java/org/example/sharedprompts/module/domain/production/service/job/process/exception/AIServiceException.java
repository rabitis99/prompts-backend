package org.example.sharedprompts.module.domain.production.service.job.process.exception;

public class AIServiceException extends JobProcessingException {
    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

