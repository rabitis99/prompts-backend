package org.example.sharedprompts.domain.prompt.enums.action;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;

/**
 * ActionType 공통 인터페이스
 * 모든 ActionType enum이 구현해야 하는 공통 메서드 정의
 * 
 * <p><b>구현 시 주의사항:</b>
 * <p>모든 ActionType enum은 다음 순서로 3개의 String 파라미터를 받는 생성자를 가져야 합니다:
 * <ol>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 * 
 * <p><b>파라미터 순서 주의:</b>
 * 모든 파라미터가 String 타입이므로 컴파일 타임에 순서 오류를 감지할 수 없습니다.
 * enum 상수 선언 시 각 파라미터 옆에 주석을 추가하여 순서를 명확히 하는 것을 권장합니다.
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
     * 
     * @return enum의 name() 값
     * @throws UnsupportedOperationException 비-enum 구현체인 경우
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
     * 
     * @param lang 언어 타입
     * @return 해당 언어의 표시 이름
     */
    default String getDisplayNameByLang(LanguageType lang) {
        return switch (lang) {
            case ENGLISH -> getDisplayNameEn();
            case JAPANESE -> getDisplayNameJa();
            default -> getDisplayNameKo();
        };
    }
}

