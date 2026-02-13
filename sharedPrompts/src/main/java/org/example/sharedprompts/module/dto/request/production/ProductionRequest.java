package org.example.sharedprompts.module.dto.request.production;

/**
 * Production 요청 공통 인터페이스
 * 
 * <p>모든 Production RequestDto는 이 인터페이스를 구현해야 한다.
 * 컴파일 타임 타입 안정성을 보장하고, 리플렉션 없이 userInput을 추출할 수 있다.</p>
 */
public interface ProductionRequest {
    
    /**
     * 사용자 입력값을 반환한다.
     * 
     * @return 사용자 입력값 (없으면 null)
     */
    String userInput();
}


