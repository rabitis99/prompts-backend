package org.example.sharedprompts.domain.prompt.common.enums.action.category.writing;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * Writing-related actions. Prefer action group CREATIVE_WRITING via CreativeActionType for creative writing;
 * message composition duplicates (TEXT_MESSAGE, WHATSAPP_MESSAGE) deprecated in favor of MESSAGE_WRITING + metadata.
 */
@Getter
public enum WritingActionType implements ActionTypeInterface, StableKeyedEnum {
    ARTICLE_WRITING("ACTION.WRITING.ARTICLE_WRITING", "기사 작성", "Article Writing", "記事執筆", ActionGroup.LONG_FORM_WRITING),
    ESSAY_WRITING("ACTION.WRITING.ESSAY_WRITING", "에세이 작성", "Essay Writing", "エッセイ執筆", ActionGroup.LONG_FORM_WRITING),
    TECHNICAL_WRITING("ACTION.WRITING.TECHNICAL_WRITING", "기술 문서 작성", "Technical Writing", "技術文書作成", ActionGroup.TECHNICAL_WRITING),
    /** @deprecated Duplicate of CreativeActionType.CREATIVE_WRITING; use that for recommendation. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    CREATIVE_WRITING_GEN("ACTION.WRITING.CREATIVE_WRITING_GEN", "창작 글쓰기", "Creative Writing", "創作執筆", ActionGroup.CREATIVE_WRITING),
    EDITING("ACTION.WRITING.EDITING", "편집", "Editing", "編集", ActionGroup.TEXT_REVISION),
    PROOFREADING("ACTION.WRITING.PROOFREADING", "교정", "Proofreading", "校正", ActionGroup.TEXT_REVISION),
    TRANSLATION("ACTION.WRITING.TRANSLATION", "번역", "Translation", "翻訳", ActionGroup.TRANSLATION),
    DOC_UPDATE("ACTION.WRITING.DOC_UPDATE", "문서 업데이트", "Document Update", "文書更新", ActionGroup.TEXT_REVISION),
    COPYWRITING("ACTION.WRITING.COPYWRITING", "카피라이팅", "Copywriting", "コピーライティング", ActionGroup.LONG_FORM_WRITING),
    GRANT_WRITING("ACTION.WRITING.GRANT_WRITING", "기획안 작성", "Grant Writing", "助成金申請書作成", ActionGroup.LONG_FORM_WRITING),
    LETTER_WRITING("ACTION.WRITING.LETTER_WRITING", "편지 작성", "Letter Writing", "手紙作成", ActionGroup.LETTER_WRITING),
    PERSONAL_LETTER("ACTION.WRITING.PERSONAL_LETTER", "개인 편지", "Personal Letter", "個人手紙", ActionGroup.LETTER_WRITING),
    BUSINESS_LETTER("ACTION.WRITING.BUSINESS_LETTER", "공식 편지", "Business Letter", "ビジネスレター", ActionGroup.LETTER_WRITING),
    INVITATION_CARD("ACTION.WRITING.INVITATION_CARD", "초대장 작성", "Invitation Card", "招待状作成", ActionGroup.SHORT_COPY),
    THANK_YOU_CARD("ACTION.WRITING.THANK_YOU_CARD", "감사 카드", "Thank You Card", "お礼カード", ActionGroup.SHORT_COPY),
    CONGRATULATORY_MESSAGE("ACTION.WRITING.CONGRATULATORY_MESSAGE", "축하 메시지", "Congratulatory Message", "お祝いメッセージ", ActionGroup.MESSAGE_COMPOSITION),
    CONDOLENCE_MESSAGE("ACTION.WRITING.CONDOLENCE_MESSAGE", "조의 메시지", "Condolence Message", "お悔やみメッセージ", ActionGroup.MESSAGE_COMPOSITION),
    MESSAGE_WRITING("ACTION.WRITING.MESSAGE_WRITING", "메시지 작성", "Message Writing", "メッセージ作成", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Channel-specific; use MESSAGE_WRITING + channel as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    TEXT_MESSAGE("ACTION.WRITING.TEXT_MESSAGE", "문자 메시지", "Text Message", "テキストメッセージ", ActionGroup.MESSAGE_COMPOSITION),
    /** @deprecated Channel-specific; use MESSAGE_WRITING + channel as metadata. Kept for key compatibility. */
    @Deprecated(since = "action-enum-refactor", forRemoval = false)
    WHATSAPP_MESSAGE("ACTION.WRITING.WHATSAPP_MESSAGE", "왓츠앱 메시지", "WhatsApp Message", "WhatsAppメッセージ", ActionGroup.MESSAGE_COMPOSITION);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    WritingActionType(
            String stableKey,
            String displayNameKo,
            String displayNameEn,
            String displayNameJa,
            ActionGroup actionGroup
    ) {
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

