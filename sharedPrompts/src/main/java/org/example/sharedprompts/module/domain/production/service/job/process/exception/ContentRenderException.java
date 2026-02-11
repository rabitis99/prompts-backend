package org.example.sharedprompts.module.domain.production.service.job.process.exception;

public class ContentRenderException extends JobProcessingException {
    public ContentRenderException(String message) {
        super(message);
    }

    public ContentRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}

