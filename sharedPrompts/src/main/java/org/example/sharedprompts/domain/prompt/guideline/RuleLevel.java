package org.example.sharedprompts.domain.prompt.guideline;

/**
 * 규칙 강도 — 위반 시 영향도를 구분
 */
public enum RuleLevel {
    /** 위반 시 품질 실패로 간주 */
    HARD,
    /** 가능하면 준수 */
    SOFT
}

