package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum WritingActionType implements ActionTypeInterface {
    ARTICLE_WRITING("기사 작성", "Article Writing", "記事執筆"),
    ESSAY_WRITING("에세이 작성", "Essay Writing", "エッセイ執筆"),
    TECHNICAL_WRITING("기술 문서 작성", "Technical Writing", "技術文書作成"),
    CREATIVE_WRITING_GEN("창작 글쓰기", "Creative Writing", "創作執筆"),
    EDITING("편집", "Editing", "編集"),
    PROOFREADING("교정", "Proofreading", "校正"),
    TRANSLATION("번역", "Translation", "翻訳"),
    DOC_UPDATE("문서 업데이트", "Document Update", "文書更新"),
    COPYWRITING("카피라이팅", "Copywriting", "コピーライティング"),
    GRANT_WRITING("기획안 작성", "Grant Writing", "助成金申請書作成"),
    LETTER_WRITING("편지 작성", "Letter Writing", "手紙作成"),
    PERSONAL_LETTER("개인 편지", "Personal Letter", "個人手紙"),
    BUSINESS_LETTER("공식 편지", "Business Letter", "ビジネスレター"),
    INVITATION_CARD("초대장 작성", "Invitation Card", "招待状作成"),
    THANK_YOU_CARD("감사 카드", "Thank You Card", "お礼カード"),
    CONGRATULATORY_MESSAGE("축하 메시지", "Congratulatory Message", "お祝いメッセージ"),
    CONDOLENCE_MESSAGE("조의 메시지", "Condolence Message", "お悔やみメッセージ"),
    MESSAGE_WRITING("메시지 작성", "Message Writing", "メッセージ作成"),
    TEXT_MESSAGE("문자 메시지", "Text Message", "テキストメッセージ"),
    WHATSAPP_MESSAGE("왓츠앱 메시지", "WhatsApp Message", "WhatsAppメッセージ");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }
}

