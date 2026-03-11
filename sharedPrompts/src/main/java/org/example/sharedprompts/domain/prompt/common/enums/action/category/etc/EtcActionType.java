package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum EtcActionType implements ActionTypeInterface, StableKeyedEnum {
    GENERAL_CONSULTATION("일반 상담", "General Consultation", "一般相談", OutputBehaviorType.GENERAL_CONSULTATION),
    PROBLEM_SOLVING("문제 해결", "Problem Solving", "問題解決", OutputBehaviorType.GENERAL_CONSULTATION),
    INFORMATION_RESEARCH("정보 조사", "Information Research", "情報調査", OutputBehaviorType.GENERAL_CONSULTATION),
    RECOMMENDATION("추천", "Recommendation", "推奨", OutputBehaviorType.GENERAL_CONSULTATION),
    EXPLANATION("설명", "Explanation", "説明", OutputBehaviorType.GENERAL_CONSULTATION),
    ADVICE("조언", "Advice", "アドバイス", OutputBehaviorType.GENERAL_CONSULTATION),
    GUIDANCE("안내", "Guidance", "案内", OutputBehaviorType.GENERAL_CONSULTATION),
    RECIPE_CREATION("레시피 작성", "Recipe Creation", "レシピ作成", OutputBehaviorType.GENERAL_CONSULTATION),
    COOKING_TIPS("요리 팁", "Cooking Tips", "料理のコツ", OutputBehaviorType.GENERAL_CONSULTATION),
    HEALTH_MANAGEMENT("건강 관리", "Health Management", "健康管理", OutputBehaviorType.GENERAL_CONSULTATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.ETC." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.GENERAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

