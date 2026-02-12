package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.CONTENT_RENDER_ERROR;

public class ContentRenderException extends JobProcessingException {
    
    public ContentRenderException(String message) {
        super(CONTENT_RENDER_ERROR, message);
    }

    public ContentRenderException(String message, Throwable cause) {
        super(CONTENT_RENDER_ERROR, message, cause);
    }

    public ContentRenderException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ContentRenderException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

