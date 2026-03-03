package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ContentActionType implements ActionTypeInterface {
    CONTENT_CREATION("콘텐츠 생성", "Content Creation", "コンテンツ作成"),
    CONTENT_REVISION("콘텐츠 수정", "Content Revision", "コンテンツ修正"),
    CONTENT_PLANNING("콘텐츠 기획", "Content Planning", "コンテンツ企画"),
    BLOG_WRITING("블로그 작성", "Blog Writing", "ブログ執筆"),
    SOCIAL_MEDIA_POST("소셜 미디어 게시물", "Social Media Post", "ソーシャルメディア投稿"),
    INSTAGRAM_CAPTION("인스타그램 캡션", "Instagram Caption", "Instagramキャプション"),
    FACEBOOK_POST("페이스북 게시물", "Facebook Post", "Facebook投稿"),
    X_POST("X 게시물", "X Post", "X投稿"),
    COMMENT_WRITING("댓글 작성", "Comment Writing", "コメント作成"),
    VIDEO_SCRIPT("영상 스크립트", "Video Script", "動画スクリプト"),
    CONTENT_OPTIMIZATION("콘텐츠 최적화", "Content Optimization", "コンテンツ最適化"),
    MULTIMEDIA_PRODUCTION("멀티미디어 제작", "Multimedia Production", "マルチメディア制作"),
    PODCAST_SCRIPT("팟캐스트 스크립트", "Podcast Script", "ポッドキャストスクリプト"),
    NEWSLETTER_WRITING("뉴스레터 작성", "Newsletter Writing", "ニュースレター執筆");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }
}

