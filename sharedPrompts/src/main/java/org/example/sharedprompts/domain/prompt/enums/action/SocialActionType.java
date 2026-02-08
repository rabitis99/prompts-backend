package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SocialActionType implements ActionTypeInterface {
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
    TRAVEL_PLANNING("여행 계획", "Travel Planning", "旅行計画"),
    EVENT_PLANNING("행사 기획", "Event Planning", "イベント企画"),
    PARTY_PLANNING("파티 기획", "Party Planning", "パーティー企画"),
    GIFT_SELECTION("선물 선택", "Gift Selection", "贈り物選び"),
    DECORATION_IDEAS("장식 아이디어", "Decoration Ideas", "装飾アイデア"),
    HOME_ORGANIZATION("집 정리", "Home Organization", "家の整理"),
    CHILD_CARE_TIPS("육아 팁", "Child Care Tips", "子育てのコツ"),
    EDUCATION_CONSULTATION("교육 상담", "Education Consultation", "教育相談"),
    RELATIONSHIP_ADVICE("관계 조언", "Relationship Advice", "人間関係のアドバイス"),
    CONFLICT_RESOLUTION("갈등 해결", "Conflict Resolution", "紛争解決"),
    DECISION_MAKING("의사결정", "Decision Making", "意思決定"),
    GOAL_SETTING("목표 설정", "Goal Setting", "目標設定"),
    HABIT_FORMATION("습관 형성", "Habit Formation", "習慣形成"),
    MOTIVATION("동기 부여", "Motivation", "モチベーション"),
    SELF_IMPROVEMENT("자기계발", "Self Improvement", "自己啓発"),
    STRESS_MANAGEMENT("스트레스 관리", "Stress Management", "ストレス管理"),
    TIME_OFF_PLANNING("휴가 계획", "Time Off Planning", "休暇計画"),
    HOBBY_EXPLORATION("취미 탐색", "Hobby Exploration", "趣味探し"),
    BOOK_RECOMMENDATION("도서 추천", "Book Recommendation", "本の推薦"),
    MOVIE_RECOMMENDATION("영화 추천", "Movie Recommendation", "映画の推薦"),
    RESTAURANT_RECOMMENDATION("맛집 추천", "Restaurant Recommendation", "レストラン推薦"),
    PRODUCT_REVIEW("제품 리뷰", "Product Review", "製品レビュー"),
    COMPARISON_SHOPPING("상품 비교", "Comparison Shopping", "商品比較"),
    PRICE_NEGOTIATION("가격 협상", "Price Negotiation", "価格交渉"),
    COMPLAINT_WRITING("불만 접수", "Complaint Writing", "苦情申し立て"),
    FEEDBACK_WRITING("피드백 작성", "Feedback Writing", "フィードバック作成"),
    REVIEW_WRITING("리뷰 작성", "Review Writing", "レビュー作成"),
    TESTIMONIAL_WRITING("추천서 작성", "Testimonial Writing", "推薦文作成"),
    APPLICATION_WRITING("지원서 작성", "Application Writing", "応募書類作成"),
    RESUME_WRITING("이력서 작성", "Resume Writing", "履歴書作成"),
    COVER_LETTER("자기소개서", "Cover Letter", "カバーレター"),
    INTERVIEW_PREPARATION("면접 준비", "Interview Preparation", "面接準備"),
    NETWORKING_MESSAGE("네트워킹 메시지", "Networking Message", "ネットワーキングメッセージ"),
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
}

