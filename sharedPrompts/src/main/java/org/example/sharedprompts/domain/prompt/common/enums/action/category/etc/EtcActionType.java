package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * Catch-all category actions. Prefer category-specific enums when possible.
 * Intent-like constants (RECOMMENDATION, EXPLANATION, ADVICE, GUIDANCE) are deprecated;
 * use Intent + action group for recommendation instead.
 */
@Getter
public enum EtcActionType implements ActionTypeInterface, StableKeyedEnum {
    GENERAL_CONSULTATION("ACTION.ETC.GENERAL_CONSULTATION", "일반 상담", "General Consultation", "一般相談", ActionGroup.GUIDANCE),
    PROBLEM_SOLVING("ACTION.ETC.PROBLEM_SOLVING", "문제 해결", "Problem Solving", "問題解決", ActionGroup.GUIDANCE),
    INFORMATION_RESEARCH("ACTION.ETC.INFORMATION_RESEARCH", "정보 조사", "Information Research", "情報調査", ActionGroup.GUIDANCE),
    /** @deprecated Intent-level; use Intent RECOMMEND + action group RECOMMENDATION. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    RECOMMENDATION("ACTION.ETC.RECOMMENDATION", "추천", "Recommendation", "推奨", ActionGroup.RECOMMENDATION),
    /** @deprecated Intent-level; use Intent EXPLAIN + action group EXPLANATION. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    EXPLANATION("ACTION.ETC.EXPLANATION", "설명", "Explanation", "説明", ActionGroup.EXPLANATION),
    /** @deprecated Intent-level; use Intent + action group GUIDANCE. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    ADVICE("ACTION.ETC.ADVICE", "조언", "Advice", "アドバイス", ActionGroup.GUIDANCE),
    /** @deprecated Intent-level; use Intent + action group GUIDANCE. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    GUIDANCE("ACTION.ETC.GUIDANCE", "안내", "Guidance", "案内", ActionGroup.GUIDANCE),
    RECIPE_CREATION("ACTION.ETC.RECIPE_CREATION", "레시피 작성", "Recipe Creation", "レシピ作成", ActionGroup.GUIDANCE),
    COOKING_TIPS("ACTION.ETC.COOKING_TIPS", "요리 팁", "Cooking Tips", "料理のコツ", ActionGroup.GUIDANCE),
    HEALTH_MANAGEMENT("ACTION.ETC.HEALTH_MANAGEMENT", "건강 관리", "Health Management", "健康管理", ActionGroup.HEALTH_TRACKING);
    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    EtcActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
        this.stableKey = stableKey;
        this.displayNameKo = displayNameKo;
        this.displayNameEn = displayNameEn;
        this.displayNameJa = displayNameJa;
        this.actionGroup = actionGroup;
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

