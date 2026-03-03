package org.example.sharedprompts.domain.prompt.application.service.guideline;

import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.springframework.stereotype.Component;

/**
 * English guideline renderer
 */
@Component
public class EnglishGuidelineRenderer extends AbstractGuidelineRenderer {

    @Override
    protected LanguageType getLanguageType() {
        return LanguageType.ENGLISH;
    }

    @Override
    protected String getRuleTitle(GuidelineRule rule) {
        return rule.title().en();
    }

    @Override
    protected String getRuleDescription(GuidelineRule rule) {
        return rule.description().en();
    }

    @Override
    protected String getNoticeTitle(GuidelineRule notice) {
        return notice.title().en();
    }

    @Override
    protected String getNoticeDescription(GuidelineRule notice) {
        return notice.description().en();
    }

    @Override
    protected String getPersonaHeaderPrefix() {
        return "You are ";
    }

    @Override
    protected String getPersonaHeaderSuffix() {
        return ".";
    }

    @Override
    protected String getToneStyleConnector() {
        return ", using ";
    }

    @Override
    protected String getPersonaHeaderEnding() {
        return ".";
    }

    @Override
    protected String getForbidSuffix() {
        return " Forbidden";
    }

    @Override
    protected String getAllowSuffix() {
        return " (Allowed)";
    }

    @Override
    protected String getDescriptionSeparator() {
        return ". ";
    }

    @Override
    protected String renderToneStyleLine(ToneType tone, StyleType style) {
        if (tone == null || style == null) {
            return "";
        }
        
        return "Respond in a "
                + tone.getGuidelineByLang(getLanguageType()).toLowerCase()
                + getToneStyleConnector()
                + style.getGuidelineByLang(getLanguageType()).toLowerCase()
                + getPersonaHeaderEnding();
    }

    @Override
    public String renderExperienceContext(ExperienceLevel experience) {
        if (experience == null) {
            return "";
        }
        return switch (experience) {
            case BEGINNER -> "Minimize jargon and explain step-by-step from basics.";
            case INTERMEDIATE -> "Assume foundational knowledge; focus on practical details and pitfalls.";
            case ADVANCED -> "Include in-depth analysis, optimization techniques, and design trade-offs.";
            case EXPERT -> "Cover latest trends, edge cases, performance benchmarks, and advanced architectural decisions.";
        };
    }
}

