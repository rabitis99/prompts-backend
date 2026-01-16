package org.example.sharedprompts.domain.report.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportType {
    PROMPT("프롬프트", "프롬프트 신고"),
    COMMENT("댓글", "댓글 신고");

    private final String name;
    private final String description;
}


