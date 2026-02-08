package org.example.sharedprompts.domain.prompt.enums.role;

/**
 * RoleType 공통 인터페이스
 * 모든 RoleType enum이 구현해야 하는 공통 메서드 정의
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
}

