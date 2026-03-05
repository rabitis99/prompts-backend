package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

@Getter
@AllArgsConstructor
public enum WritingRoleType implements RoleTypeInterface, StableKeyedEnum {
    TECHNICAL_WRITER(
            "기술 문서 작성자",
            "기술 문서 작성 및 기술적 내용 설명 전문가",
            "Technical Writer",
            "A specialist in technical document writing and technical content explanation",
            "技術文書執筆者",
            "技術文書作成と技術的内容説明の専門家"
    ),
    CONTENT_WRITER(
            "콘텐츠 작가",
            "다양한 형식의 콘텐츠 작성 및 편집 전문가",
            "Content Writer",
            "A specialist in writing and editing content in various formats",
            "コンテンツライター",
            "様々な形式のコンテンツ作成と編集の専門家"
    ),
    COPYWRITER(
            "카피라이터",
            "마케팅 및 광고를 위한 카피라이팅 전문가",
            "Copywriter",
            "A specialist in copywriting for marketing and advertising",
            "コピーライター",
            "マーケティングと広告のためのコピーライティング専門家"
    ),
    EDITOR(
            "편집자",
            "문서 편집 및 교정 전문가",
            "Editor",
            "A specialist in document editing and proofreading",
            "編集者",
            "文書編集と校正の専門家"
    ),
    TRANSLATOR(
            "번역가",
            "다국어 번역 및 언어 서비스 전문가",
            "Translator",
            "A specialist in multilingual translation and language services",
            "翻訳者",
            "多言語翻訳と言語サービスの専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String key() {
        return "ROLE.WRITING." + name();
    }
}

