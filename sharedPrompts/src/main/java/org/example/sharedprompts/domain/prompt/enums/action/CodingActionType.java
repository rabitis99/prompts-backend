package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CodingActionType implements ActionTypeInterface {
    CODE_GENERATION("코드 생성", "Code Generation", "コード生成"),
    CODE_MODIFICATION("코드 수정", "Code Modification", "コード修正"),
    CODE_REVIEW("코드 리뷰", "Code Review", "コードレビュー"),
    REFACTORING("리팩토링", "Refactoring", "リファクタリング"),
    TEST_GENERATION("테스트 코드 생성", "Test Code Generation", "テストコード生成"),
    DEBUGGING("디버깅", "Debugging", "デバッグ"),
    CODE_ANALYSIS("코드 분석", "Code Analysis", "コード分析"),
    PATTERN_APPLICATION("디자인 패턴 적용", "Design Pattern Application", "デザインパターン適用"),
    CODE_OPTIMIZATION("코드 최적화", "Code Optimization", "コード最適化"),
    LEGACY_CODE_MAINTENANCE("레거시 코드 유지보수", "Legacy Code Maintenance", "レガシーコード保守");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

