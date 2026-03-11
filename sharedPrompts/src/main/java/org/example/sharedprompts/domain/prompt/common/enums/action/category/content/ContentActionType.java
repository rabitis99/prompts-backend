package org.example.sharedprompts.domain.prompt.common.enums.action.category.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ContentActionType implements ActionTypeInterface, StableKeyedEnum {
    CONTENT_CREATION("콘텐츠 생성", "Content Creation", "コンテンツ作成", OutputBehaviorType.LONG_FORM_WRITING),
    CONTENT_REVISION("콘텐츠 수정", "Content Revision", "コンテンツ修正", OutputBehaviorType.LONG_FORM_WRITING),
    CONTENT_PLANNING("콘텐츠 기획", "Content Planning", "コンテンツ企画", OutputBehaviorType.LONG_FORM_WRITING),
    BLOG_WRITING("블로그 작성", "Blog Writing", "ブログ執筆", OutputBehaviorType.LONG_FORM_WRITING),
    SOCIAL_MEDIA_POST("소셜 미디어 게시물", "Social Media Post", "ソーシャルメディア投稿", OutputBehaviorType.LONG_FORM_WRITING),
    INSTAGRAM_CAPTION("인스타그램 캡션", "Instagram Caption", "Instagramキャプション", OutputBehaviorType.LONG_FORM_WRITING),
    FACEBOOK_POST("페이스북 게시물", "Facebook Post", "Facebook投稿", OutputBehaviorType.LONG_FORM_WRITING),
    X_POST("X 게시물", "X Post", "X投稿", OutputBehaviorType.LONG_FORM_WRITING),
    COMMENT_WRITING("댓글 작성", "Comment Writing", "コメント作成", OutputBehaviorType.LONG_FORM_WRITING),
    VIDEO_SCRIPT("영상 스크립트", "Video Script", "動画スクリプト", OutputBehaviorType.LONG_FORM_WRITING),
    CONTENT_OPTIMIZATION("콘텐츠 최적화", "Content Optimization", "コンテンツ最適化", OutputBehaviorType.LONG_FORM_WRITING),
    MULTIMEDIA_PRODUCTION("멀티미디어 제작", "Multimedia Production", "マルチメディア制作", OutputBehaviorType.LONG_FORM_WRITING),
    PODCAST_SCRIPT("팟캐스트 스크립트", "Podcast Script", "ポッドキャストスクリプト", OutputBehaviorType.LONG_FORM_WRITING),
    NEWSLETTER_WRITING("뉴스레터 작성", "Newsletter Writing", "ニュースレター執筆", OutputBehaviorType.LONG_FORM_WRITING);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CONTENT." + name();
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

