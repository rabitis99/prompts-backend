package org.example.sharedprompts.domain.prompt.common.enums.action.category.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 이메일 관련 액션 타입 enum
 */
@Getter
@AllArgsConstructor
public enum EmailActionType implements ActionTypeInterface, StableKeyedEnum {
    EMAIL_WRITING("이메일 작성", "Email Writing", "メール作成", OutputBehaviorType.LONG_FORM_WRITING),
    BUSINESS_EMAIL("비즈니스 이메일", "Business Email", "ビジネスメール", OutputBehaviorType.LONG_FORM_WRITING),
    PERSONAL_EMAIL("개인 이메일", "Personal Email", "個人メール", OutputBehaviorType.LONG_FORM_WRITING),
    THANK_YOU_EMAIL("감사 이메일", "Thank You Email", "お礼メール", OutputBehaviorType.LONG_FORM_WRITING),
    APOLOGY_EMAIL("사과 이메일", "Apology Email", "謝罪メール", OutputBehaviorType.LONG_FORM_WRITING),
    INQUIRY_EMAIL("문의 이메일", "Inquiry Email", "問い合わせメール", OutputBehaviorType.LONG_FORM_WRITING),
    INVITATION_EMAIL("초대 이메일", "Invitation Email", "招待メール", OutputBehaviorType.LONG_FORM_WRITING),
    FOLLOW_UP_EMAIL("후속 이메일", "Follow-up Email", "フォローアップメール", OutputBehaviorType.LONG_FORM_WRITING),
    REJECTION_EMAIL("거절 이메일", "Rejection Email", "断りメール", OutputBehaviorType.LONG_FORM_WRITING),
    CONFIRMATION_EMAIL("확인 이메일", "Confirmation Email", "確認メール", OutputBehaviorType.LONG_FORM_WRITING);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.EMAIL." + name();
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

