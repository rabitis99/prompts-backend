package org.example.sharedprompts.domain.prompt.domain.resolutions;

/**
 * TaskDomain이 선택된 근거(해석 소스).
 */
public enum ResolutionSource {
    /** ActionDomainRegistry (action type → task domain) 로 결정 */
    ACTION_TYPE,

    /** PromptCategory.getDefaultDomain()으로 결정 */
    PROMPT_CATEGORY,

    /** Unified 흐름: Intent/커맨드 도메인 선호도로 결정 (리졸버 매핑 없을 때) */
    INTENT_AFFINITY,

    /** RoleType 매핑으로 결정 (미사용 시 예비) */
    ROLE_TYPE,

    /** 명시적/설계상 기본값 (예: GENERAL) */
    DEFAULT,

    /** 매핑 없음 시 폴백 */
    FALLBACK
}
