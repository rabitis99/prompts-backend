package org.example.sharedprompts.domain.production.exception;

public class CommandValidationException extends ProductionException {
    public CommandValidationException(String message) {
        super(message);
    }
}

