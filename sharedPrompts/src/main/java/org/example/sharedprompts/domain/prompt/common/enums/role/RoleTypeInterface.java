package org.example.sharedprompts.domain.prompt.common.enums.role;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;

/**
 * RoleType 공통 인터페이스
 * 모든 RoleType enum이 구현해야 하는 공통 메서드 정의.
 *
 * <p><b>책임:</b> RoleType = identity of perspective. Display/description text is supplied by
 * {@link org.example.sharedprompts.domain.prompt.domain.descriptor.RoleDescriptorPort}; prefer that port
 * over calling getRoleName* / getDescription* directly so that enums remain identity-oriented.
 * Interface display methods are retained for backward compatibility and for use by the default
 * descriptor implementation.</p>
 *
 * <p><b>key() 계약:</b> 공통 규칙은 {@link #keyPrefix()} + "." + {@link Enum#name()} 이다.
 * 각 enum은 {@link #keyPrefix()}만 구현하면 되며, key 문자열을 수동 조합할 필요가 없어 신규 RoleType 추가 시 드리프트를 막을 수 있다.</p>
 */
public interface RoleTypeInterface {

    /**
     * 이 RoleType enum의 stable key 접두사 (예: "ROLE.CONTENT", "ROLE.ETC").
     * {@link #key()}는 이 접두사와 {@link Enum#name()}을 점(.)으로 이어 반환한다.
     */
    String keyPrefix();

    /**
     * Stable identifier for serialization and equality.
     * Default implementation returns {@code keyPrefix() + "." + name()};
     * compatible with {@link org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum#key()}.
     */
    default String key() {
        return keyPrefix() + "." + ((Enum<?>) this).name();
    }

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

