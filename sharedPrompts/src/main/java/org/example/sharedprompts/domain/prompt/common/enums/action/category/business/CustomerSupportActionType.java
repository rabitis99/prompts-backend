package org.example.sharedprompts.domain.prompt.common.enums.action.category.business;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
public enum CustomerSupportActionType implements ActionTypeInterface, StableKeyedEnum {
    TICKET_CREATION("ACTION.CUSTOMER_SUPPORT.TICKET_CREATION", "티켓 생성", "Ticket Creation", "チケット作成", ActionGroup.TICKET_HANDLING),
    TICKET_RESOLUTION("ACTION.CUSTOMER_SUPPORT.TICKET_RESOLUTION", "티켓 해결", "Ticket Resolution", "チケット解決", ActionGroup.TICKET_HANDLING),
    KNOWLEDGE_BASE_MANAGEMENT("ACTION.CUSTOMER_SUPPORT.KNOWLEDGE_BASE_MANAGEMENT", "지식 기반 관리", "Knowledge Base Management", "ナレッジベース管理", ActionGroup.FAQ_AND_KNOWLEDGE_BASE),
    LIVE_CHAT_SUPPORT("ACTION.CUSTOMER_SUPPORT.LIVE_CHAT_SUPPORT", "실시간 채팅 지원", "Live Chat Support", "ライブチャットサポート", ActionGroup.TICKET_HANDLING),
    CUSTOMER_SERVICE("ACTION.CUSTOMER_SUPPORT.CUSTOMER_SERVICE", "고객 서비스", "Customer Service", "カスタマーサービス", ActionGroup.TICKET_HANDLING),
    FAQ_CREATION("ACTION.CUSTOMER_SUPPORT.FAQ_CREATION", "FAQ 작성", "FAQ Creation", "FAQ作成", ActionGroup.FAQ_AND_KNOWLEDGE_BASE),
    SUPPORT_DOCUMENTATION("ACTION.CUSTOMER_SUPPORT.SUPPORT_DOCUMENTATION", "지원 문서화", "Support Documentation", "サポート文書化", ActionGroup.FAQ_AND_KNOWLEDGE_BASE),
    CUSTOMER_ONBOARDING("ACTION.CUSTOMER_SUPPORT.CUSTOMER_ONBOARDING", "고객 온보딩", "Customer Onboarding", "カスタマーオンボーディング", ActionGroup.CUSTOMER_ONBOARDING),
    CUSTOMER_RETENTION("ACTION.CUSTOMER_SUPPORT.CUSTOMER_RETENTION", "고객 유지", "Customer Retention", "顧客維持", ActionGroup.TICKET_HANDLING),
    COMPLAINT_HANDLING("ACTION.CUSTOMER_SUPPORT.COMPLAINT_HANDLING", "불만 처리", "Complaint Handling", "苦情処理", ActionGroup.COMPLAINT_RESPONSE),
    CUSTOMER_FEEDBACK_ANALYSIS("ACTION.CUSTOMER_SUPPORT.CUSTOMER_FEEDBACK_ANALYSIS", "고객 피드백 분석", "Customer Feedback Analysis", "顧客フィードバック分析", ActionGroup.TICKET_HANDLING),
    SUPPORT_TRAINING("ACTION.CUSTOMER_SUPPORT.SUPPORT_TRAINING", "지원 교육", "Support Training", "サポート教育", ActionGroup.CUSTOMER_ONBOARDING),
    REMOTE_SUPPORT("ACTION.CUSTOMER_SUPPORT.REMOTE_SUPPORT", "원격 지원", "Remote Support", "リモートサポート", ActionGroup.TICKET_HANDLING),
    CUSTOMER_SATISFACTION("ACTION.CUSTOMER_SUPPORT.CUSTOMER_SATISFACTION", "고객 만족도", "Customer Satisfaction", "顧客満足度", ActionGroup.TICKET_HANDLING);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    CustomerSupportActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
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

