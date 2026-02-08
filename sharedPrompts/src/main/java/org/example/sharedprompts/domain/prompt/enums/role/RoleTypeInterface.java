package org.example.sharedprompts.domain.prompt.enums.role;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;

/**
 * RoleType 공통 인터페이스
 * 모든 RoleType enum이 구현해야 하는 공통 메서드 정의
 * 
 * <p><b>구현 시 주의사항:</b>
 * <p>모든 RoleType enum은 다음 순서로 6개의 String 파라미터를 받는 생성자를 가져야 합니다:
 * <ol>
 *   <li>roleNameKo - 역할 이름 (한국어)</li>
 *   <li>descriptionKo - 역할 설명 (한국어)</li>
 *   <li>roleNameEn - 역할 이름 (영어)</li>
 *   <li>descriptionEn - 역할 설명 (영어)</li>
 *   <li>roleNameJa - 역할 이름 (일본어)</li>
 *   <li>descriptionJa - 역할 설명 (일본어)</li>
 * </ol>
 * 
 * <p><b>파라미터 순서 주의:</b>
 * 모든 파라미터가 String 타입이므로 컴파일 타임에 순서 오류를 감지할 수 없습니다.
 * enum 상수 선언 시 각 파라미터 옆에 주석을 추가하여 순서를 명확히 하는 것을 권장합니다.
 * 
 * <p>향후 리팩토링 시 Builder 패턴이나 record 기반 구조를 고려할 수 있습니다.
 */
public interface RoleTypeInterface {
    /** 역할 이름 - 한국어 */
    String getRoleNameKo();
    
    /** 역할 설명 - 한국어 */
    String getDescriptionKo();
    
    /** 역할 이름 - 영어 */
    String getRoleNameEn();
    
    /** 역할 설명 - 영어 */
    String getDescriptionEn();
    
    /** 역할 이름 - 일본어 */
    String getRoleNameJa();
    
    /** 역할 설명 - 일본어 */
    String getDescriptionJa();
    
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
     * 언어 타입에 따라 적절한 역할 이름을 반환하는 디폴트 메서드
     * GuidelineBuilder 등에서 언어별 분기 로직을 줄이기 위해 사용
     * 
     * @param lang 언어 타입
     * @return 해당 언어의 역할 이름
     */
    default String getRoleNameByLang(LanguageType lang) {
        return switch (lang) {
            case ENGLISH -> getRoleNameEn();
            case JAPANESE -> getRoleNameJa();
            default -> getRoleNameKo();
        };
    }
    
    /**
     * 언어 타입에 따라 적절한 역할 설명을 반환하는 디폴트 메서드
     * GuidelineBuilder 등에서 언어별 분기 로직을 줄이기 위해 사용
     * 
     * @param lang 언어 타입
     * @return 해당 언어의 역할 설명
     */
    default String getDescriptionByLang(LanguageType lang) {
        return switch (lang) {
            case ENGLISH -> getDescriptionEn();
            case JAPANESE -> getDescriptionJa();
            default -> getDescriptionKo();
        };
    }
}

