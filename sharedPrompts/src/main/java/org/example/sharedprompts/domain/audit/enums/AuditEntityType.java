package org.example.sharedprompts.domain.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 감사 로그에 기록될 엔티티 타입
 */
@Getter
@AllArgsConstructor
public enum AuditEntityType {

    USER("사용자"),
    PROMPT("프롬프트"),
    COMMENT("댓글"),
    REPORT("신고"),
    NOTIFICATION("알림"),
    LIKE("좋아요"),
    TAG("태그");

    private final String displayName;
}

