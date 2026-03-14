package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/** 소셜/커뮤니티 관련 액션 타입 enum. Keys are explicit for stability (ACTION.SOCIAL.*). */
@Getter
public enum SocialActionType implements ActionTypeInterface, StableKeyedEnum {
    COMMUNITY_ENGAGEMENT("ACTION.SOCIAL.COMMUNITY_ENGAGEMENT", "커뮤니티 참여", "Community Engagement", "コミュニティ参加", ActionGroup.SOCIAL_OR_COMMUNITY),
    EVENT_ORGANIZATION("ACTION.SOCIAL.EVENT_ORGANIZATION", "이벤트 조직", "Event Organization", "イベントオーガナイズ", ActionGroup.SOCIAL_OR_COMMUNITY),
    SOCIAL_CAUSE_SUPPORT("ACTION.SOCIAL.SOCIAL_CAUSE_SUPPORT", "사회적 원인 지원", "Social Cause Support", "社会的な原因支援", ActionGroup.SOCIAL_OR_COMMUNITY),
    VOLUNTEER_COORDINATION("ACTION.SOCIAL.VOLUNTEER_COORDINATION", "자원봉사 조정", "Volunteer Coordination", "ボランティア調整", ActionGroup.SOCIAL_OR_COMMUNITY),
    FUNDRAISING("ACTION.SOCIAL.FUNDRAISING", "모금", "Fundraising", "資金調達", ActionGroup.SOCIAL_OR_COMMUNITY),
    COMMUNITY_OUTREACH("ACTION.SOCIAL.COMMUNITY_OUTREACH", "커뮤니티 외부 연계", "Community Outreach", "コミュニティアウトリーチ", ActionGroup.SOCIAL_OR_COMMUNITY),
    SOCIAL_MEDIA_ENGAGEMENT("ACTION.SOCIAL.SOCIAL_MEDIA_ENGAGEMENT", "소셜 미디어 참여", "Social Media Engagement", "ソーシャルメディア参加", ActionGroup.SOCIAL_OR_COMMUNITY),
    NETWORKING_EVENT("ACTION.SOCIAL.NETWORKING_EVENT", "네트워킹 이벤트", "Networking Event", "ネットワーキングイベント", ActionGroup.SOCIAL_OR_COMMUNITY),
    COMMUNITY_BUILDING("ACTION.SOCIAL.COMMUNITY_BUILDING", "커뮤니티 구축", "Community Building", "コミュニティ構築", ActionGroup.SOCIAL_OR_COMMUNITY),
    SOCIAL_ACTIVISM("ACTION.SOCIAL.SOCIAL_ACTIVISM", "사회 운동", "Social Activism", "社会運動", ActionGroup.SOCIAL_OR_COMMUNITY),
    ADVOCACY("ACTION.SOCIAL.ADVOCACY", "옹호 활동", "Advocacy", "擁護活動", ActionGroup.SOCIAL_OR_COMMUNITY),
    PUBLIC_SPEAKING("ACTION.SOCIAL.PUBLIC_SPEAKING", "공개 연설", "Public Speaking", "公開演説", ActionGroup.SOCIAL_OR_COMMUNITY),
    SOCIAL_IMPACT("ACTION.SOCIAL.SOCIAL_IMPACT", "사회적 영향", "Social Impact", "社会的影響", ActionGroup.SOCIAL_OR_COMMUNITY),
    COLLABORATION("ACTION.SOCIAL.COLLABORATION", "협업", "Collaboration", "協力", ActionGroup.SOCIAL_OR_COMMUNITY),
    PARTNERSHIP_BUILDING("ACTION.SOCIAL.PARTNERSHIP_BUILDING", "파트너십 구축", "Partnership Building", "パートナーシップ構築", ActionGroup.SOCIAL_OR_COMMUNITY),
    CROSS_CULTURAL_COMMUNICATION("ACTION.SOCIAL.CROSS_CULTURAL_COMMUNICATION", "문화 간 소통", "Cross-cultural Communication", "異文化コミュニケーション", ActionGroup.SOCIAL_OR_COMMUNITY),
    COMPLAINT_WRITING("ACTION.SOCIAL.COMPLAINT_WRITING", "불만 접수", "Complaint Writing", "苦情申し立て", ActionGroup.REVIEW_OR_FEEDBACK_WRITING),
    FEEDBACK_WRITING("ACTION.SOCIAL.FEEDBACK_WRITING", "피드백 작성", "Feedback Writing", "フィードバック作成", ActionGroup.REVIEW_OR_FEEDBACK_WRITING),
    REVIEW_WRITING("ACTION.SOCIAL.REVIEW_WRITING", "리뷰 작성", "Review Writing", "レビュー作成", ActionGroup.REVIEW_OR_FEEDBACK_WRITING),
    TESTIMONIAL_WRITING("ACTION.SOCIAL.TESTIMONIAL_WRITING", "추천서 작성", "Testimonial Writing", "推薦文作成", ActionGroup.REVIEW_OR_FEEDBACK_WRITING),
    FOLLOW_UP_MESSAGE("ACTION.SOCIAL.FOLLOW_UP_MESSAGE", "후속 메시지", "Follow-up Message", "フォローアップメッセージ", ActionGroup.MESSAGE_COMPOSITION),
    APPOINTMENT_SCHEDULING("ACTION.SOCIAL.APPOINTMENT_SCHEDULING", "약속 잡기", "Appointment Scheduling", "予約調整", ActionGroup.MESSAGE_COMPOSITION),
    REMINDER_MESSAGE("ACTION.SOCIAL.REMINDER_MESSAGE", "알림 메시지", "Reminder Message", "リマインダーメッセージ", ActionGroup.MESSAGE_COMPOSITION),
    BIRTHDAY_MESSAGE("ACTION.SOCIAL.BIRTHDAY_MESSAGE", "생일 메시지", "Birthday Message", "誕生日メッセージ", ActionGroup.MESSAGE_COMPOSITION),
    ANNIVERSARY_MESSAGE("ACTION.SOCIAL.ANNIVERSARY_MESSAGE", "기념일 메시지", "Anniversary Message", "記念日メッセージ", ActionGroup.MESSAGE_COMPOSITION),
    HOLIDAY_GREETING("ACTION.SOCIAL.HOLIDAY_GREETING", "명절 인사", "Holiday Greeting", "祝日の挨拶", ActionGroup.SHORT_COPY),
    SEASONAL_GREETING("ACTION.SOCIAL.SEASONAL_GREETING", "계절 인사", "Seasonal Greeting", "季節の挨拶", ActionGroup.SHORT_COPY);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    SocialActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
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

