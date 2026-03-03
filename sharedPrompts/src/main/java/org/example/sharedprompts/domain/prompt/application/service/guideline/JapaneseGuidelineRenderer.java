package org.example.sharedprompts.domain.prompt.application.service.guideline;

import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.springframework.stereotype.Component;

/**
 * 日本語ガイドラインレンダラー
 */
@Component
public class JapaneseGuidelineRenderer extends AbstractGuidelineRenderer {

    @Override
    protected LanguageType getLanguageType() {
        return LanguageType.JAPANESE;
    }

    @Override
    protected String getRuleTitle(GuidelineRule rule) {
        return rule.title().ja();
    }

    @Override
    protected String getRuleDescription(GuidelineRule rule) {
        return rule.description().ja();
    }

    @Override
    protected String getNoticeTitle(GuidelineRule notice) {
        return notice.title().ja();
    }

    @Override
    protected String getNoticeDescription(GuidelineRule notice) {
        return notice.description().ja();
    }

    @Override
    protected String getPersonaHeaderPrefix() {
        return "あなたは";
    }

    @Override
    protected String getPersonaHeaderSuffix() {
        return "です。";
    }

    @Override
    protected String getToneStyleConnector() {
        return "で、";
    }

    @Override
    protected String getPersonaHeaderEnding() {
        return "で回答してください。";
    }

    @Override
    protected String getForbidSuffix() {
        return " 禁止";
    }

    @Override
    protected String getAllowSuffix() {
        return " 許可";
    }

    @Override
    protected String getDescriptionSeparator() {
        return "。";
    }

    @Override
    public String renderExperienceContext(ExperienceLevel experience) {
        if (experience == null) {
            return "";
        }
        return switch (experience) {
            case BEGINNER -> "専門用語を最小限にし、基礎から段階的に説明してください。";
            case INTERMEDIATE -> "基本知識を前提とし、実践的な詳細と注意点に集中してください。";
            case ADVANCED -> "高度な内容、最適化手法、設計トレードオフを含めてください。";
            case EXPERT -> "最新動向、エッジケース、パフォーマンスベンチマーク、高度なアーキテクチャ判断を扱ってください。";
        };
    }
}

