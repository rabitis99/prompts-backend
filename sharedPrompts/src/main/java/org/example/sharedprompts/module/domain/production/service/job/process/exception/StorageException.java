package org.example.sharedprompts.module.domain.production.service.job.process.exception;

public class StorageException extends JobProcessingException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

