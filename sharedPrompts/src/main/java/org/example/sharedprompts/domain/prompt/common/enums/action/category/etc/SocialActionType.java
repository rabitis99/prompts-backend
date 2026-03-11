package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 소셜/커뮤니티 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서:
 * <ol>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 *   <li>outputBehavior - 출력 행동 유형 ({@link OutputBehaviorType})</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum SocialActionType implements ActionTypeInterface, StableKeyedEnum {
    COMMUNITY_ENGAGEMENT("커뮤니티 참여", "Community Engagement", "コミュニティ参加", OutputBehaviorType.STRATEGIC_PLAN),
    EVENT_ORGANIZATION("이벤트 조직", "Event Organization", "イベントオーガナイズ", OutputBehaviorType.STRATEGIC_PLAN),
    SOCIAL_CAUSE_SUPPORT("사회적 원인 지원", "Social Cause Support", "社会的な原因支援", OutputBehaviorType.STRATEGIC_PLAN),
    VOLUNTEER_COORDINATION("자원봉사 조정", "Volunteer Coordination", "ボランティア調整", OutputBehaviorType.STRATEGIC_PLAN),
    FUNDRAISING("모금", "Fundraising", "資金調達", OutputBehaviorType.STRATEGIC_PLAN),
    COMMUNITY_OUTREACH("커뮤니티 외부 연계", "Community Outreach", "コミュニティアウトリーチ", OutputBehaviorType.STRATEGIC_PLAN),
    SOCIAL_MEDIA_ENGAGEMENT("소셜 미디어 참여", "Social Media Engagement", "ソーシャルメディア参加", OutputBehaviorType.STRATEGIC_PLAN),
    NETWORKING_EVENT("네트워킹 이벤트", "Networking Event", "ネットワーキングイベント", OutputBehaviorType.STRATEGIC_PLAN),
    COMMUNITY_BUILDING("커뮤니티 구축", "Community Building", "コミュニティ構築", OutputBehaviorType.STRATEGIC_PLAN),
    SOCIAL_ACTIVISM("사회 운동", "Social Activism", "社会運動", OutputBehaviorType.STRATEGIC_PLAN),
    ADVOCACY("옹호 활동", "Advocacy", "擁護活動", OutputBehaviorType.STRATEGIC_PLAN),
    PUBLIC_SPEAKING("공개 연설", "Public Speaking", "公開演説", OutputBehaviorType.STRATEGIC_PLAN),
    SOCIAL_IMPACT("사회적 영향", "Social Impact", "社会的影響", OutputBehaviorType.STRATEGIC_PLAN),
    COLLABORATION("협업", "Collaboration", "協力", OutputBehaviorType.STRATEGIC_PLAN),
    PARTNERSHIP_BUILDING("파트너십 구축", "Partnership Building", "パートナーシップ構築", OutputBehaviorType.STRATEGIC_PLAN),
    CROSS_CULTURAL_COMMUNICATION("문화 간 소통", "Cross-cultural Communication", "異文化コミュニケーション", OutputBehaviorType.STRATEGIC_PLAN),
    COMPLAINT_WRITING("불만 접수", "Complaint Writing", "苦情申し立て", OutputBehaviorType.LONG_FORM_WRITING),
    FEEDBACK_WRITING("피드백 작성", "Feedback Writing", "フィードバック作成", OutputBehaviorType.LONG_FORM_WRITING),
    REVIEW_WRITING("리뷰 작성", "Review Writing", "レビュー作成", OutputBehaviorType.LONG_FORM_WRITING),
    TESTIMONIAL_WRITING("추천서 작성", "Testimonial Writing", "推薦文作成", OutputBehaviorType.LONG_FORM_WRITING),
    FOLLOW_UP_MESSAGE("후속 메시지", "Follow-up Message", "フォローアップメッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    APPOINTMENT_SCHEDULING("약속 잡기", "Appointment Scheduling", "予約調整", OutputBehaviorType.MESSAGE_COMPOSITION),
    REMINDER_MESSAGE("알림 메시지", "Reminder Message", "リマインダーメッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    BIRTHDAY_MESSAGE("생일 메시지", "Birthday Message", "誕生日メッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    ANNIVERSARY_MESSAGE("기념일 메시지", "Anniversary Message", "記念日メッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    HOLIDAY_GREETING("명절 인사", "Holiday Greeting", "祝日の挨拶", OutputBehaviorType.SHORT_COPY),
    SEASONAL_GREETING("계절 인사", "Seasonal Greeting", "季節の挨拶", OutputBehaviorType.SHORT_COPY);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.SOCIAL." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

