package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

/**
 * 이메일 관련 액션 타입 enum
 */
@Getter
@AllArgsConstructor
public enum EmailActionType implements ActionTypeInterface {
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

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }
}

