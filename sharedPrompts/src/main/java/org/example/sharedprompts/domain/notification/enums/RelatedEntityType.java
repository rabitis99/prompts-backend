package org.example.sharedprompts.domain.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림의 관련 엔티티 타입
 * - relatedEntityId가 어떤 엔티티를 가리키는지 구분하기 위함
 */
@Getter
@AllArgsConstructor
public enum RelatedEntityType {
    PROMPT("프롬프트"),
    COMMENT("댓글"),
    USER("사용자");

    private final String displayName;
}

