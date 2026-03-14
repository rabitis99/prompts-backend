package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum CodingActionType implements ActionTypeInterface, StableKeyedEnum {
    CODE_GENERATION("코드 생성", "Code Generation", "コード生成", ActionGroup.CODE_GENERATION),
    CODE_MODIFICATION("코드 수정", "Code Modification", "コード修正", ActionGroup.CODE_MODIFICATION),
    CODE_REVIEW("코드 리뷰", "Code Review", "コードレビュー", ActionGroup.EVALUATION_OR_AUDIT),
    REFACTORING("리팩토링", "Refactoring", "リファクタリング", ActionGroup.CODE_MODIFICATION),
    TEST_GENERATION("테스트 코드 생성", "Test Code Generation", "テストコード生成", ActionGroup.CODE_GENERATION),
    DEBUGGING("디버깅", "Debugging", "デバッグ", ActionGroup.DEBUGGING),
    CODE_ANALYSIS("코드 분석", "Code Analysis", "コード分析", ActionGroup.CODE_ANALYSIS),
    PATTERN_APPLICATION("디자인 패턴 적용", "Design Pattern Application", "デザインパターン適用", ActionGroup.CODE_GENERATION),
    CODE_OPTIMIZATION("코드 최적화", "Code Optimization", "コード最適化", ActionGroup.CODE_MODIFICATION),
    LEGACY_CODE_MAINTENANCE("레거시 코드 유지보수", "Legacy Code Maintenance", "レガシーコード保守", ActionGroup.CODE_MODIFICATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.CODING." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

