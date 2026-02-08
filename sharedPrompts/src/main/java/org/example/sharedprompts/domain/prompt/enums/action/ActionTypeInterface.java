package org.example.sharedprompts.domain.prompt.enums.action;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;

/**
 * ActionType 공통 인터페이스
 * 모든 ActionType enum이 구현해야 하는 공통 메서드 정의
 * 
 * <p>향후 리팩토링 시 Builder 패턴이나 record 기반 구조를 고려할 수 있습니다.
 */
public interface ActionTypeInterface {
    String getDisplayNameKo();
    String getDisplayNameEn();
    String getDisplayNameJa();
    
    /**
     * Enum의 name() 메서드를 안전하게 호출하기 위한 디폴트 메서드
     * 현재 모든 구현체는 enum이지만, 향후 비-enum 클래스가 구현할 경우를 대비
     */
    default String name() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }
        throw new UnsupportedOperationException("name() not supported for non-enum implementations");
    }
    
    /**
     * 언어 타입에 따라 적절한 표시 이름을 반환하는 디폴트 메서드
     * GuidelineBuilder 등에서 언어별 분기 로직을 줄이기 위해 사용
     */
    default String getDisplayNameByLang(LanguageType lang) {
        if (lang == null) {
            return getDisplayNameKo();
        }
        return switch (lang) {
            case KOREAN -> getDisplayNameKo();
            case ENGLISH -> getDisplayNameEn();
            case JAPANESE -> getDisplayNameJa();
        };
    }
}

