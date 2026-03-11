package org.example.sharedprompts.domain.prompt.common.enums.action.category.writing;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
public enum WritingActionType implements ActionTypeInterface, StableKeyedEnum {
    ARTICLE_WRITING("ACTION.WRITING.ARTICLE_WRITING", "기사 작성", "Article Writing", "記事執筆"),
    ESSAY_WRITING("ACTION.WRITING.ESSAY_WRITING", "에세이 작성", "Essay Writing", "エッセイ執筆"),
    TECHNICAL_WRITING("ACTION.WRITING.TECHNICAL_WRITING", "기술 문서 작성", "Technical Writing", "技術文書作成"),
    CREATIVE_WRITING_GEN("ACTION.WRITING.CREATIVE_WRITING_GEN", "창작 글쓰기", "Creative Writing", "創作執筆"),
    EDITING("ACTION.WRITING.EDITING", "편집", "Editing", "編集"),
    PROOFREADING("ACTION.WRITING.PROOFREADING", "교정", "Proofreading", "校正"),
    TRANSLATION("ACTION.WRITING.TRANSLATION", "번역", "Translation", "翻訳"),
    DOC_UPDATE("ACTION.WRITING.DOC_UPDATE", "문서 업데이트", "Document Update", "文書更新"),
    COPYWRITING("ACTION.WRITING.COPYWRITING", "카피라이팅", "Copywriting", "コピーライティング"),
    GRANT_WRITING("ACTION.WRITING.GRANT_WRITING", "기획안 작성", "Grant Writing", "助成金申請書作成"),
    LETTER_WRITING("ACTION.WRITING.LETTER_WRITING", "편지 작성", "Letter Writing", "手紙作成"),
    PERSONAL_LETTER("ACTION.WRITING.PERSONAL_LETTER", "개인 편지", "Personal Letter", "個人手紙"),
    BUSINESS_LETTER("ACTION.WRITING.BUSINESS_LETTER", "공식 편지", "Business Letter", "ビジネスレター"),
    INVITATION_CARD("ACTION.WRITING.INVITATION_CARD", "초대장 작성", "Invitation Card", "招待状作成", OutputBehaviorType.SHORT_COPY),
    THANK_YOU_CARD("ACTION.WRITING.THANK_YOU_CARD", "감사 카드", "Thank You Card", "お礼カード", OutputBehaviorType.SHORT_COPY),
    CONGRATULATORY_MESSAGE("ACTION.WRITING.CONGRATULATORY_MESSAGE", "축하 메시지", "Congratulatory Message", "お祝いメッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    CONDOLENCE_MESSAGE("ACTION.WRITING.CONDOLENCE_MESSAGE", "조의 메시지", "Condolence Message", "お悔やみメッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    MESSAGE_WRITING("ACTION.WRITING.MESSAGE_WRITING", "메시지 작성", "Message Writing", "メッセージ作成", OutputBehaviorType.MESSAGE_COMPOSITION),
    TEXT_MESSAGE("ACTION.WRITING.TEXT_MESSAGE", "문자 메시지", "Text Message", "テキストメッセージ", OutputBehaviorType.MESSAGE_COMPOSITION),
    WHATSAPP_MESSAGE("ACTION.WRITING.WHATSAPP_MESSAGE", "왓츠앱 메시지", "WhatsApp Message", "WhatsAppメッセージ", OutputBehaviorType.MESSAGE_COMPOSITION);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    WritingActionType(
            String stableKey,
            String displayNameKo,
            String displayNameEn,
            String displayNameJa
    ) {
        this(stableKey, displayNameKo, displayNameEn, displayNameJa, OutputBehaviorType.LONG_FORM_WRITING);
    }

    WritingActionType(
            String stableKey,
            String displayNameKo,
            String displayNameEn,
            String displayNameJa,
            OutputBehaviorType outputBehavior
    ) {
        this.stableKey = stableKey;
        this.displayNameKo = displayNameKo;
        this.displayNameEn = displayNameEn;
        this.displayNameJa = displayNameJa;
        this.outputBehavior = outputBehavior;
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

