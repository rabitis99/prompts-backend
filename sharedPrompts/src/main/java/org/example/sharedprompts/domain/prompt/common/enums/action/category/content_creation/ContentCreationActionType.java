package org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * PromptCategory.CONTENT_CREATION actions. Keys are explicit for stability (ACTION.CONTENT.*).
 * Channel-specific posts (INSTAGRAM_CAPTION, FACEBOOK_POST, X_POST) map to SHORT_COPY; channel as metadata.
 */
@Getter
public enum ContentCreationActionType implements ActionTypeInterface, StableKeyedEnum {
    CONTENT_CREATION("ACTION.CONTENT.CONTENT_CREATION", "콘텐츠 생성", "Content Creation", "コンテンツ作成", ActionGroup.LONG_FORM_WRITING),
    CONTENT_REVISION("ACTION.CONTENT.CONTENT_REVISION", "콘텐츠 수정", "Content Revision", "コンテンツ修正", ActionGroup.TEXT_REVISION),
    CONTENT_PLANNING("ACTION.CONTENT.CONTENT_PLANNING", "콘텐츠 기획", "Content Planning", "コンテンツ企画", ActionGroup.GENERAL_PLANNING),
    BLOG_WRITING("ACTION.CONTENT.BLOG_WRITING", "블로그 작성", "Blog Writing", "ブログ執筆", ActionGroup.LONG_FORM_WRITING),
    SOCIAL_MEDIA_POST("ACTION.CONTENT.SOCIAL_MEDIA_POST", "소셜 미디어 게시물", "Social Media Post", "ソーシャルメディア投稿", ActionGroup.SHORT_COPY),
    INSTAGRAM_CAPTION("ACTION.CONTENT.INSTAGRAM_CAPTION", "인스타그램 캡션", "Instagram Caption", "Instagramキャプション", ActionGroup.SHORT_COPY),
    FACEBOOK_POST("ACTION.CONTENT.FACEBOOK_POST", "페이스북 게시물", "Facebook Post", "Facebook投稿", ActionGroup.SHORT_COPY),
    X_POST("ACTION.CONTENT.X_POST", "X 게시물", "X Post", "X投稿", ActionGroup.SHORT_COPY),
    COMMENT_WRITING("ACTION.CONTENT.COMMENT_WRITING", "댓글 작성", "Comment Writing", "コメント作成", ActionGroup.SHORT_COPY),
    VIDEO_SCRIPT("ACTION.CONTENT.VIDEO_SCRIPT", "영상 스크립트", "Video Script", "動画スクリプト", ActionGroup.SCRIPT_OR_MEDIA_WRITING),
    CONTENT_OPTIMIZATION("ACTION.CONTENT.CONTENT_OPTIMIZATION", "콘텐츠 최적화", "Content Optimization", "コンテンツ最適化", ActionGroup.TEXT_REVISION),
    MULTIMEDIA_PRODUCTION("ACTION.CONTENT.MULTIMEDIA_PRODUCTION", "멀티미디어 제작", "Multimedia Production", "マルチメディア制作", ActionGroup.SCRIPT_OR_MEDIA_WRITING),
    PODCAST_SCRIPT("ACTION.CONTENT.PODCAST_SCRIPT", "팟캐스트 스크립트", "Podcast Script", "ポッドキャストスクリプト", ActionGroup.SCRIPT_OR_MEDIA_WRITING),
    NEWSLETTER_WRITING("ACTION.CONTENT.NEWSLETTER_WRITING", "뉴스레터 작성", "Newsletter Writing", "ニュースレター執筆", ActionGroup.LONG_FORM_WRITING);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    ContentCreationActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
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
