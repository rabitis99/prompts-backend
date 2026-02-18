package org.example.sharedprompts.module.dto.request.production;

/**
 * Production 요청 공통 인터페이스
 */
public interface ProductionRequest {
    
    /**
     * 사용자 입력값을 반환한다.
     */
    String userInput();
}