package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 소셜/커뮤니티 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>stableKey - 안정 키 (직렬화/호환성용)</li>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum SocialActionType implements ActionTypeInterface, StableKeyedEnum {
    COMMUNITY_ENGAGEMENT("커뮤니티 참여", "Community Engagement", "コミュニティ参加"),
    EVENT_ORGANIZATION("이벤트 조직", "Event Organization", "イベントオーガナイズ"),
    SOCIAL_CAUSE_SUPPORT("사회적 원인 지원", "Social Cause Support", "社会的な原因支援"),
    VOLUNTEER_COORDINATION("자원봉사 조정", "Volunteer Coordination", "ボランティア調整"),
    FUNDRAISING("모금", "Fundraising", "資金調達"),
    COMMUNITY_OUTREACH("커뮤니티 외부 연계", "Community Outreach", "コミュニティアウトリーチ"),
    SOCIAL_MEDIA_ENGAGEMENT("소셜 미디어 참여", "Social Media Engagement", "ソーシャルメディア参加"),
    NETWORKING_EVENT("네트워킹 이벤트", "Networking Event", "ネットワーキングイベント"),
    COMMUNITY_BUILDING("커뮤니티 구축", "Community Building", "コミュニティ構築"),
    SOCIAL_ACTIVISM("사회 운동", "Social Activism", "社会運動"),
    ADVOCACY("옹호 활동", "Advocacy", "擁護活動"),
    PUBLIC_SPEAKING("공개 연설", "Public Speaking", "公開演説"),
    SOCIAL_IMPACT("사회적 영향", "Social Impact", "社会的影響"),
    COLLABORATION("협업", "Collaboration", "協力"),
    PARTNERSHIP_BUILDING("파트너십 구축", "Partnership Building", "パートナーシップ構築"),
    CROSS_CULTURAL_COMMUNICATION("문화 간 소통", "Cross-cultural Communication", "異文化コミュニケーション"),
    COMPLAINT_WRITING("불만 접수", "Complaint Writing", "苦情申し立て"),
    FEEDBACK_WRITING("피드백 작성", "Feedback Writing", "フィードバック作成"),
    REVIEW_WRITING("리뷰 작성", "Review Writing", "レビュー作成"),
    TESTIMONIAL_WRITING("추천서 작성", "Testimonial Writing", "推薦文作成"),
    FOLLOW_UP_MESSAGE("후속 메시지", "Follow-up Message", "フォローアップメッセージ"),
    APPOINTMENT_SCHEDULING("약속 잡기", "Appointment Scheduling", "予約調整"),
    REMINDER_MESSAGE("알림 메시지", "Reminder Message", "リマインダーメッセージ"),
    BIRTHDAY_MESSAGE("생일 메시지", "Birthday Message", "誕生日メッセージ"),
    ANNIVERSARY_MESSAGE("기념일 메시지", "Anniversary Message", "記念日メッセージ"),
    HOLIDAY_GREETING("명절 인사", "Holiday Greeting", "祝日の挨拶"),
    SEASONAL_GREETING("계절 인사", "Seasonal Greeting", "季節の挨拶");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return "ACTION.SOCIAL." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }
}

