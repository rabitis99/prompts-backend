package org.example.sharedprompts.domain.report.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportReason {
    SPAM("스팸", "스팸 또는 광고성 콘텐츠"),
    INAPPROPRIATE_CONTENT("부적절한 내용", "욕설, 비방, 혐오 표현 등 부적절한 내용"),
    COPYRIGHT_VIOLATION("저작권 침해", "저작권을 침해하는 콘텐츠"),
    HARASSMENT("괴롭힘", "타인을 괴롭히거나 협박하는 내용"),
    FALSE_INFORMATION("거짓 정보", "거짓 정보나 오해의 소지가 있는 내용"),
    ETC("기타", "기타 사유");

    private final String name;
    private final String description;
}


