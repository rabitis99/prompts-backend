package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.retry;

import org.springframework.stereotype.Component;

@Component
public class AIErrorClassifier {

    public boolean isPermanentError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        String lowerMessage = errorMessage.toLowerCase();
        return lowerMessage.contains("invalid") ||
                lowerMessage.contains("unauthorized") ||
                lowerMessage.contains("forbidden") ||
                lowerMessage.contains("bad request") ||
                lowerMessage.contains("not retryable") ||
                lowerMessage.contains("content filter") ||
                lowerMessage.contains("filter blocked");
    }
    
    /**
     * 예외가 영구적 오류(재시도 불가능)인지 확인합니다.
     */
    public boolean isPermanentError(Exception exception) {
        if (exception == null) {
            return false;
        }
        
        // 예외 메시지 확인
        String message = exception.getMessage();
        if (message != null && isPermanentError(message)) {
            return true;
        }
        
        // 원인 예외 확인
        Throwable cause = exception.getCause();
        if (cause != null && cause instanceof Exception) {
            return isPermanentError((Exception) cause);
        }
        
        return false;
    }
}

