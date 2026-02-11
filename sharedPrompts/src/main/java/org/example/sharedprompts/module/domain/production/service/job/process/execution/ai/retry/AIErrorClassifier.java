package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.retry;

import org.springframework.stereotype.Component;

@Component
public class AIErrorClassifier {

    public boolean isPermanentError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        return errorMessage.contains("Invalid") ||
                errorMessage.contains("Unauthorized") ||
                errorMessage.contains("Forbidden") ||
                errorMessage.contains("Bad Request");
    }
}

