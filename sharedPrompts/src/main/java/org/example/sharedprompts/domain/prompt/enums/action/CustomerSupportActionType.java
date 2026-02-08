package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerSupportActionType implements ActionTypeInterface {
    TICKET_CREATION("티켓 생성", "Ticket Creation", "チケット作成"),
    TICKET_RESOLUTION("티켓 해결", "Ticket Resolution", "チケット解決"),
    KNOWLEDGE_BASE_MANAGEMENT("지식 기반 관리", "Knowledge Base Management", "ナレッジベース管理"),
    LIVE_CHAT_SUPPORT("실시간 채팅 지원", "Live Chat Support", "ライブチャットサポート"),
    CUSTOMER_SERVICE("고객 서비스", "Customer Service", "カスタマーサービス"),
    FAQ_CREATION("FAQ 작성", "FAQ Creation", "FAQ作成"),
    SUPPORT_DOCUMENTATION("지원 문서화", "Support Documentation", "サポート文書化"),
    CUSTOMER_ONBOARDING("고객 온보딩", "Customer Onboarding", "カスタマーオンボーディング"),
    CUSTOMER_RETENTION("고객 유지", "Customer Retention", "顧客維持"),
    COMPLAINT_HANDLING("불만 처리", "Complaint Handling", "苦情処理"),
    CUSTOMER_FEEDBACK_ANALYSIS("고객 피드백 분석", "Customer Feedback Analysis", "顧客フィードバック分析"),
    SUPPORT_TRAINING("지원 교육", "Support Training", "サポート教育"),
    REMOTE_SUPPORT("원격 지원", "Remote Support", "リモートサポート"),
    CUSTOMER_SATISFACTION("고객 만족도", "Customer Satisfaction", "顧客満足도"),
    EMAIL_WRITING("이메일 작성", "Email Writing", "メール作成"),
    BUSINESS_EMAIL("비즈니스 이메일", "Business Email", "ビジネスメール"),
    PERSONAL_EMAIL("개인 이메일", "Personal Email", "個人メール"),
    THANK_YOU_EMAIL("감사 이메일", "Thank You Email", "お礼メール"),
    APOLOGY_EMAIL("사과 이메일", "Apology Email", "謝罪メール"),
    INQUIRY_EMAIL("문의 이메일", "Inquiry Email", "問い合わせメール"),
    INVITATION_EMAIL("초대 이메일", "Invitation Email", "招待メール"),
    FOLLOW_UP_EMAIL("후속 이메일", "Follow-up Email", "フォローアップメール"),
    REJECTION_EMAIL("거절 이메일", "Rejection Email", "断りメール"),
    CONFIRMATION_EMAIL("확인 이메일", "Confirmation Email", "確認メール");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

