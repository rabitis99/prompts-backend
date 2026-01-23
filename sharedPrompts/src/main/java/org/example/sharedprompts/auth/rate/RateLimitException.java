package org.example.sharedprompts.auth.rate;

/**
 * Rate Limit 관련 예외
 * 
 * Rate Limit 체크 중 발생하는 예외를 나타냅니다.
 */
public class RateLimitException extends RuntimeException {
    
    public RateLimitException(String message) {
        super(message);
    }
    
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}




