package org.example.sharedprompts.domain.prompt.service.guideline;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.enums.guideline.GeneralGuidelines;
import org.example.sharedprompts.domain.prompt.enums.guideline.RuleType;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * English guideline renderer
 */
@Component
@RequiredArgsConstructor
public class EnglishGuidelineRenderer implements GuidelineRenderer {

    @Override
    public String renderPrinciples(List<GuidelineRule> rules) {
        if (rules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("Principles\n");

        for (int i = 0; i < rules.size(); i++) {
            GuidelineRule rule = rules.get(i);
            sb.append(renderCompactRule(rule));
            if (i < rules.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String renderStructuringRules(List<GuidelineRule> rules) {
        if (rules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("Response Rules\n");

        for (int i = 0; i < rules.size(); i++) {
            GuidelineRule rule = rules.get(i);
            sb.append(renderCompactRule(rule));
            if (i < rules.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String renderQualityStandards(List<GuidelineRule> rules) {
        if (rules.isEmpty()) {
            return "";
        }

        // Quality standards integrated into output constraints for simplification
        return "";
    }

    @Override
    public String renderOutputConstraints(List<GuidelineRule> rules) {
        if (rules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < rules.size(); i++) {
            GuidelineRule rule = rules.get(i);
            sb.append(renderCompactRule(rule));
            if (i < rules.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String renderFallbackNotice(TaskDomain domain) {
        if (domain != TaskDomain.GENERAL) {
            return "";
        }

        GuidelineRule notice = GeneralGuidelines.LIMITATION_ACK;
        return "## " + notice.title().en() + "\n\n" + notice.description().en();
    }

    @Override
    public String renderPersonaHeader(RoleTypeInterface role, ToneType tone, StyleType style) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(role.getRoleNameByLang(LanguageType.ENGLISH)).append(".\n");
        sb.append("Respond in a ").append(tone.getGuidelineByLang(LanguageType.ENGLISH).toLowerCase())
          .append(", using ")
          .append(style.getGuidelineByLang(LanguageType.ENGLISH).toLowerCase())
          .append(".");
        return sb.toString();
    }

    @Override
    public String renderEssentialConstraints(List<GuidelineRule> hardRules) {
        if (hardRules.isEmpty()) {
            return "";
        }

        List<GuidelineRule> requires = hardRules.stream()
                .filter(r -> r.type() == RuleType.REQUIRE).toList();
        List<GuidelineRule> forbids = hardRules.stream()
                .filter(r -> r.type() == RuleType.FORBID).toList();
        List<GuidelineRule> allows = hardRules.stream()
                .filter(r -> r.type() == RuleType.ALLOW).toList();

        StringBuilder sb = new StringBuilder();

        if (!requires.isEmpty()) {
            sb.append(requires.stream()
                    .map(r -> r.description().en())
                    .collect(java.util.stream.Collectors.joining(". ")));
        }

        if (!forbids.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(forbids.stream()
                    .map(r -> r.description().en() + " (Forbidden)")
                    .collect(java.util.stream.Collectors.joining(". ")));
        }

        if (!allows.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(allows.stream()
                    .map(r -> r.description().en() + " (Allowed)")
                    .collect(java.util.stream.Collectors.joining(". ")));
        }

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

    /**
     * Render rule in compact format (one line)
     */
    private String renderCompactRule(GuidelineRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.title().en()).append(": ").append(rule.description().en());

        if (rule.type() == RuleType.FORBID) {
            sb.append(" Forbidden");
        }

        return sb.toString();
    }
}

