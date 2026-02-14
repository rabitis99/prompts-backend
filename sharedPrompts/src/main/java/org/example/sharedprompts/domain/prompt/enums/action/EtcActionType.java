package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum EtcActionType implements ActionTypeInterface {
    GENERAL_CONSULTATION("일반 상담", "General Consultation", "一般相談"),
    PROBLEM_SOLVING("문제 해결", "Problem Solving", "問題解決"),
    INFORMATION_RESEARCH("정보 조사", "Information Research", "情報調査"),
    RECOMMENDATION("추천", "Recommendation", "推奨"),
    EXPLANATION("설명", "Explanation", "説明"),
    ADVICE("조언", "Advice", "アドバイス"),
    GUIDANCE("안내", "Guidance", "案内"),
    RECIPE_CREATION("레시피 작성", "Recipe Creation", "レシピ作成"),
    COOKING_TIPS("요리 팁", "Cooking Tips", "料理のコツ"),
    HEALTH_MANAGEMENT("건강 관리", "Health Management", "健康管理");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.GENERAL);
    }
}

