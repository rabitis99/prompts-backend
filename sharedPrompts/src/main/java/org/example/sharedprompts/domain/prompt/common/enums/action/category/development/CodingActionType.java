package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CodingActionType implements ActionTypeInterface, StableKeyedEnum {
    CODE_GENERATION("코드 생성", "Code Generation", "コード生成", OutputBehaviorType.CODE_IMPLEMENTATION),
    CODE_MODIFICATION("코드 수정", "Code Modification", "コード修正", OutputBehaviorType.CODE_IMPLEMENTATION),
    CODE_REVIEW("코드 리뷰", "Code Review", "コードレビュー", OutputBehaviorType.CODE_REVIEW_FEEDBACK),
    REFACTORING("리팩토링", "Refactoring", "リファクタリング", OutputBehaviorType.CODE_IMPLEMENTATION),
    TEST_GENERATION("테스트 코드 생성", "Test Code Generation", "テストコード生成", OutputBehaviorType.CODE_IMPLEMENTATION),
    DEBUGGING("디버깅", "Debugging", "デバッグ", OutputBehaviorType.DEBUGGING_SESSION),
    CODE_ANALYSIS("코드 분석", "Code Analysis", "コード分析", OutputBehaviorType.CODE_IMPLEMENTATION),
    PATTERN_APPLICATION("디자인 패턴 적용", "Design Pattern Application", "デザインパターン適用", OutputBehaviorType.CODE_IMPLEMENTATION),
    CODE_OPTIMIZATION("코드 최적화", "Code Optimization", "コード最適化", OutputBehaviorType.CODE_IMPLEMENTATION),
    LEGACY_CODE_MAINTENANCE("레거시 코드 유지보수", "Legacy Code Maintenance", "レガシーコード保守", OutputBehaviorType.CODE_IMPLEMENTATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CODING." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

