package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 라이프스타일/개인 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>stableKey - 안정적인 키 (직렬화/역직렬화 및 모듈 간 매핑용)</li>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum LifestyleActionType implements ActionTypeInterface, StableKeyedEnum {
    HOME_ORGANIZATION("ACTION.LIFESTYLE.HOME_ORGANIZATION", "집 정리", "Home Organization", "家の整理"),
    CHILD_CARE_TIPS("ACTION.LIFESTYLE.CHILD_CARE_TIPS", "육아 팁", "Child Care Tips", "子育てのコツ"),
    HOBBY_EXPLORATION("ACTION.LIFESTYLE.HOBBY_EXPLORATION", "취미 탐색", "Hobby Exploration", "趣味探し"),
    TIME_OFF_PLANNING("ACTION.LIFESTYLE.TIME_OFF_PLANNING", "휴가 계획", "Time Off Planning", "休暇計画"),
    TRAVEL_PLANNING("ACTION.LIFESTYLE.TRAVEL_PLANNING", "여행 계획", "Travel Planning", "旅行計画"),
    EVENT_PLANNING("ACTION.LIFESTYLE.EVENT_PLANNING", "행사 기획", "Event Planning", "イベント企画"),
    PARTY_PLANNING("ACTION.LIFESTYLE.PARTY_PLANNING", "파티 기획", "Party Planning", "パーティー企画"),
    GIFT_SELECTION("ACTION.LIFESTYLE.GIFT_SELECTION", "선물 선택", "Gift Selection", "贈り物選び"),
    DECORATION_IDEAS("ACTION.LIFESTYLE.DECORATION_IDEAS", "장식 아이디어", "Decoration Ideas", "装飾アイデア"),
    EDUCATION_CONSULTATION("ACTION.LIFESTYLE.EDUCATION_CONSULTATION", "교육 상담", "Education Consultation", "教育相談"),
    RELATIONSHIP_ADVICE("ACTION.LIFESTYLE.RELATIONSHIP_ADVICE", "관계 조언", "Relationship Advice", "人間関係のアドバイス"),
    CONFLICT_RESOLUTION("ACTION.LIFESTYLE.CONFLICT_RESOLUTION", "갈등 해결", "Conflict Resolution", "紛争解決");

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.GENERAL);
    }
}

