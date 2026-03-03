package org.example.sharedprompts.domain.prompt.common.enums.role;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;

/**
 * RoleType 공통 인터페이스
 * 모든 RoleType enum이 구현해야 하는 공통 메서드 정의
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
     * 언어 타입에 따라 적절한 역할 이름을 반환하는 디폴트 메서드
     * 가이드라인 시스템 등에서 언어별 분기 로직을 줄이기 위해 사용
     */
    default String getRoleNameByLang(LanguageType lang) {
        if (lang == null) {
            return getRoleNameKo();
        }
        return switch (lang) {
            case KOREAN -> getRoleNameKo();
            case ENGLISH -> getRoleNameEn();
            case JAPANESE -> getRoleNameJa();
        };
    }
    
    /**
     * 언어 타입에 따라 적절한 역할 설명을 반환하는 디폴트 메서드
     * 가이드라인 시스템 등에서 언어별 분기 로직을 줄이기 위해 사용
     */
    default String getDescriptionByLang(LanguageType lang) {
        if (lang == null) {
            return getDescriptionKo();
        }
        return switch (lang) {
            case KOREAN -> getDescriptionKo();
            case ENGLISH -> getDescriptionEn();
            case JAPANESE -> getDescriptionJa();
        };
    }
}

