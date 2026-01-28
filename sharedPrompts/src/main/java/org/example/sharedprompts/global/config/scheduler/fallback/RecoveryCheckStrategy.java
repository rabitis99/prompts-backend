package org.example.sharedprompts.global.config.scheduler.fallback;

/**
 * Redis 복구 감지를 위한 전략 인터페이스
 */
public interface RecoveryCheckStrategy {
    
    /**
     * 이번에 Redis 복구 체크를 해야 하는지 여부
     * 
     * @return 체크가 필요한 경우 true
     */
    boolean shouldCheck();
    
    /**
     * 복구 체크 완료 후 호출 (성공/실패 여부와 관계없이)
     */
    void onCheckCompleted();
    
    /**
     * 전략 리셋
     */
    void reset();
}

