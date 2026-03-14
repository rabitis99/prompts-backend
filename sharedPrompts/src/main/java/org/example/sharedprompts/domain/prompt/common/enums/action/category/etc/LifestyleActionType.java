package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/** 라이프스타일/개인 관련 액션 타입 enum */
@Getter
@AllArgsConstructor
public enum LifestyleActionType implements ActionTypeInterface, StableKeyedEnum {
    HOME_ORGANIZATION("ACTION.LIFESTYLE.HOME_ORGANIZATION", "집 정리", "Home Organization", "家の整理", ActionGroup.PERSONAL_PRODUCTIVITY),
    CHILD_CARE_TIPS("ACTION.LIFESTYLE.CHILD_CARE_TIPS", "육아 팁", "Child Care Tips", "子育てのコツ", ActionGroup.GUIDANCE),
    HOBBY_EXPLORATION("ACTION.LIFESTYLE.HOBBY_EXPLORATION", "취미 탐색", "Hobby Exploration", "趣味探し", ActionGroup.GUIDANCE),
    TIME_OFF_PLANNING("ACTION.LIFESTYLE.TIME_OFF_PLANNING", "휴가 계획", "Time Off Planning", "休暇計画", ActionGroup.GENERAL_PLANNING),
    TRAVEL_PLANNING("ACTION.LIFESTYLE.TRAVEL_PLANNING", "여행 계획", "Travel Planning", "旅行計画", ActionGroup.GENERAL_PLANNING),
    EVENT_PLANNING("ACTION.LIFESTYLE.EVENT_PLANNING", "행사 기획", "Event Planning", "イベント企画", ActionGroup.GENERAL_PLANNING),
    PARTY_PLANNING("ACTION.LIFESTYLE.PARTY_PLANNING", "파티 기획", "Party Planning", "パーティー企画", ActionGroup.GENERAL_PLANNING),
    GIFT_SELECTION("ACTION.LIFESTYLE.GIFT_SELECTION", "선물 선택", "Gift Selection", "贈り物選び", ActionGroup.RECOMMENDATION),
    DECORATION_IDEAS("ACTION.LIFESTYLE.DECORATION_IDEAS", "장식 아이디어", "Decoration Ideas", "装飾アイデア", ActionGroup.GUIDANCE),
    EDUCATION_CONSULTATION("ACTION.LIFESTYLE.EDUCATION_CONSULTATION", "교육 상담", "Education Consultation", "教育相談", ActionGroup.GUIDANCE),
    RELATIONSHIP_ADVICE("ACTION.LIFESTYLE.RELATIONSHIP_ADVICE", "관계 조언", "Relationship Advice", "人間関係のアドバイス", ActionGroup.GUIDANCE),
    CONFLICT_RESOLUTION("ACTION.LIFESTYLE.CONFLICT_RESOLUTION", "갈등 해결", "Conflict Resolution", "紛争解決", ActionGroup.GUIDANCE);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

