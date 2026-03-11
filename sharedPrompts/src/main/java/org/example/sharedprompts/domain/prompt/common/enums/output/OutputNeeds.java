package org.example.sharedprompts.domain.prompt.common.enums.output;

/**
 * 출력 형식/요구사항 축.
 */
public enum OutputNeeds {

    /** 자유 형식 텍스트 */
    FREE_FORM,

    /** 구조화된 문단/섹션 */
    STRUCTURED_TEXT,

    /** 불릿 리스트 필수 */
    BULLET_LIST_REQUIRED,

    /** 표 형식 필수 */
    TABLE_REQUIRED,

    /** JSON 출력 필수 */
    JSON_REQUIRED,

    /** JSON Schema 준수 필수 */
    JSON_SCHEMA_REQUIRED,

    /** 코드 블록 필수 */
    CODE_BLOCK_REQUIRED
}
