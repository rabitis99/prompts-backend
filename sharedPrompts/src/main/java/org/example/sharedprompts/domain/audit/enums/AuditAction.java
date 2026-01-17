package org.example.sharedprompts.domain.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 감사 로그에 기록될 작업 타입
 */
@Getter
@AllArgsConstructor
public enum AuditAction {

    // 공통
    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제"),
    VIEW("조회"),

    // 사용자 관련
    BLOCK("차단"),
    UNBLOCK("차단 해제"),
    ROLE_CHANGE("권한 변경"),
    PASSWORD_CHANGE("비밀번호 변경"),
    SOFT_DELETE("탈퇴"),

    // 프롬프트 관련
    PUBLIC_TOGGLE("공개 상태 변경"),
    CATEGORY_CHANGE("카테고리 변경"),

    // 신고 관련
    REPORT_CREATE("신고 생성"),
    REPORT_PROCESS("신고 처리"),
    REPORT_RESOLVE("신고 처리 완료"),
    REPORT_REJECT("신고 반려");

    private final String displayName;
}

