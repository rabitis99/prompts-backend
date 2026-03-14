package org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * Email-related action types (content_creation package).
 * All map to action group MESSAGE_COMPOSITION; purpose (thank-you, apology, etc.) should be metadata.
 * Purpose-specific constants are deprecated for new recommendation; keys retained for backward compatibility.
 */
@Getter
public enum EmailActionType implements ActionTypeInterface, StableKeyedEnum {
    EMAIL_WRITING("ACTION.EMAIL.EMAIL_WRITING", "이메일 작성", "Email Writing", "メール作成", ActionGroup.MESSAGE_COMPOSITION),
    BUSINESS_EMAIL("ACTION.EMAIL.BUSINESS_EMAIL", "비즈니스 이메일", "Business Email", "ビジネスメール", ActionGroup.MESSAGE_COMPOSITION),
    PERSONAL_EMAIL("ACTION.EMAIL.PERSONAL_EMAIL", "개인 이메일", "Personal Email", "個人メール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    THANK_YOU_EMAIL("ACTION.EMAIL.THANK_YOU_EMAIL", "감사 이메일", "Thank You Email", "お礼メール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    APOLOGY_EMAIL("ACTION.EMAIL.APOLOGY_EMAIL", "사과 이메일", "Apology Email", "謝罪メール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    INQUIRY_EMAIL("ACTION.EMAIL.INQUIRY_EMAIL", "문의 이메일", "Inquiry Email", "問い合わせメール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    INVITATION_EMAIL("ACTION.EMAIL.INVITATION_EMAIL", "초대 이메일", "Invitation Email", "招待メール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    FOLLOW_UP_EMAIL("ACTION.EMAIL.FOLLOW_UP_EMAIL", "후속 이메일", "Follow-up Email", "フォローアップメール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    REJECTION_EMAIL("ACTION.EMAIL.REJECTION_EMAIL", "거절 이메일", "Rejection Email", "断りメール", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Use EMAIL_WRITING + purpose as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    CONFIRMATION_EMAIL("ACTION.EMAIL.CONFIRMATION_EMAIL", "확인 이메일", "Confirmation Email", "確認メール", ActionGroup.MESSAGE_COMPOSITION);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    EmailActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
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
