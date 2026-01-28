package org.example.sharedprompts.global.config.scheduler.fallback;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * Redis 장애 여부를 판단하는 유틸리티 클래스
 */
public class RedisFailureDetector {

    /**
     * 예외가 Redis 장애로 인한 것인지 판단
     * 
     * @param e 예외
     * @return Redis 장애인 경우 true
     */
    public static boolean isRedisFailure(Exception e) {
        if (e == null) {
            return false;
        }
        
        // DataAccessException 체크
        if (e instanceof DataAccessException) {
            return true;
        }
        
        // 원인 예외 체크
        Throwable cause = e.getCause();
        if (cause != null) {
            if (cause instanceof RedisConnectionFailureException
                    || cause instanceof DataAccessException) {
                return true;
            }
            
            // 클래스 이름에 "redis"가 포함된 경우
            String className = cause.getClass().getName().toLowerCase();
            if (className.contains("redis")) {
                return true;
            }
        }
        
        // 메시지 내용 체크
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("redis")
                    || lowerMessage.contains("connection")
                    || lowerMessage.contains("timeout")) {
                return true;
            }
        }
        
        return false;
    }
}

