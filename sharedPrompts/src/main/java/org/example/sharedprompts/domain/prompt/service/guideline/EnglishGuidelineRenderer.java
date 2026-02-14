package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
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
    protected String getPrinciplesHeader() {
        return "Principles";
    }

    @Override
    protected String getStructuringRulesHeader() {
        return "Response Rules";
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
    public String renderPersonaHeader(RoleTypeInterface role, ToneType tone, StyleType style) {
        StringBuilder sb = new StringBuilder();
        sb.append(getPersonaHeaderPrefix())
                .append(role.getRoleNameByLang(getLanguageType()))
                .append(getPersonaHeaderSuffix()).append("\n");
        sb.append("Respond in a ").append(tone.getGuidelineByLang(getLanguageType()).toLowerCase())
                .append(getToneStyleConnector())
                .append(style.getGuidelineByLang(getLanguageType()).toLowerCase())
                .append(getPersonaHeaderEnding());
        return sb.toString();
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

