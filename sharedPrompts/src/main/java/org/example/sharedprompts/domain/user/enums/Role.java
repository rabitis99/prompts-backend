package org.example.sharedprompts.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {

    USER("일반 사용자", "일반 권한을 가진 사용자"),
    ADMIN("관리자", "관리 권한을 가진 사용자");

    private final String name;
    private final String description;
}
