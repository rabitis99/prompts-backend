package org.example.sharedprompts.domain.report.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatus {
    PENDING("대기중", "신고 접수 대기 상태"),
    PROCESSING("처리중", "신고 처리 중인 상태"),
    RESOLVED("처리완료", "신고 처리 완료 상태"),
    REJECTED("반려", "신고 반려 상태");

    private final String name;
    private final String description;
}

