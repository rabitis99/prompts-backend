package org.example.sharedprompts.domain.prompt.common.enums.action.category.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/** 커리어/취업 관련 액션 타입 enum */
@Getter
@AllArgsConstructor
public enum CareerActionType implements ActionTypeInterface, StableKeyedEnum {
    RESUME_WRITING("이력서 작성", "Resume Writing", "履歴書作成", ActionGroup.CAREER_DOCUMENT_WRITING),
    COVER_LETTER("자기소개서", "Cover Letter", "カバーレター", ActionGroup.CAREER_DOCUMENT_WRITING),
    INTERVIEW_PREPARATION("면접 준비", "Interview Preparation", "面接準備", ActionGroup.INTERVIEW_PREPARATION),
    NETWORKING_MESSAGE("네트워킹 메시지", "Networking Message", "ネットワーキングメッセージ", ActionGroup.MESSAGE_COMPOSITION),
    APPLICATION_WRITING("지원서 작성", "Application Writing", "応募書類作成", ActionGroup.CAREER_DOCUMENT_WRITING);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.CAREER." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

