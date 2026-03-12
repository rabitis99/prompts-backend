package org.example.sharedprompts.domain.prompt.common.enums.action.category.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CustomerSupportActionType implements ActionTypeInterface, StableKeyedEnum {
    TICKET_CREATION("티켓 생성", "Ticket Creation", "チケット作成", OutputBehaviorType.QA_STYLE_RESPONSE),
    TICKET_RESOLUTION("티켓 해결", "Ticket Resolution", "チケット解決", OutputBehaviorType.QA_STYLE_RESPONSE),
    KNOWLEDGE_BASE_MANAGEMENT("지식 기반 관리", "Knowledge Base Management", "ナレッジベース管理", OutputBehaviorType.QA_STYLE_RESPONSE),
    LIVE_CHAT_SUPPORT("실시간 채팅 지원", "Live Chat Support", "ライブチャットサポート", OutputBehaviorType.QA_STYLE_RESPONSE),
    CUSTOMER_SERVICE("고객 서비스", "Customer Service", "カスタマーサービス", OutputBehaviorType.QA_STYLE_RESPONSE),
    FAQ_CREATION("FAQ 작성", "FAQ Creation", "FAQ作成", OutputBehaviorType.QA_STYLE_RESPONSE),
    SUPPORT_DOCUMENTATION("지원 문서화", "Support Documentation", "サポート文書化", OutputBehaviorType.QA_STYLE_RESPONSE),
    CUSTOMER_ONBOARDING("고객 온보딩", "Customer Onboarding", "カスタマーオンボーディング", OutputBehaviorType.QA_STYLE_RESPONSE),
    CUSTOMER_RETENTION("고객 유지", "Customer Retention", "顧客維持", OutputBehaviorType.QA_STYLE_RESPONSE),
    COMPLAINT_HANDLING("불만 처리", "Complaint Handling", "苦情処理", OutputBehaviorType.QA_STYLE_RESPONSE),
    CUSTOMER_FEEDBACK_ANALYSIS("고객 피드백 분석", "Customer Feedback Analysis", "顧客フィードバック分析", OutputBehaviorType.QA_STYLE_RESPONSE),
    SUPPORT_TRAINING("지원 교육", "Support Training", "サポート教育", OutputBehaviorType.QA_STYLE_RESPONSE),
    REMOTE_SUPPORT("원격 지원", "Remote Support", "リモートサポート", OutputBehaviorType.QA_STYLE_RESPONSE),
    CUSTOMER_SATISFACTION("고객 만족도", "Customer Satisfaction", "顧客満足度", OutputBehaviorType.QA_STYLE_RESPONSE);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CUSTOMER_SUPPORT." + name();
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

