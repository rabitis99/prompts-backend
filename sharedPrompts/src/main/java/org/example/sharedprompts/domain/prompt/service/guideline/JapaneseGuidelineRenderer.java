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
 * 日本語ガイドラインレンダラー
 */
@Component
@RequiredArgsConstructor
public class JapaneseGuidelineRenderer implements GuidelineRenderer {

    @Override
    public String renderPrinciples(List<GuidelineRule> rules) {
        if (rules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("原則\n");

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

        StringBuilder sb = new StringBuilder("応答ルール\n");

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

        // 品質基準は出力制約に統合して簡素化
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
        return "## " + notice.title().ja() + "\n\n" + notice.description().ja();
    }

    @Override
    public String renderPersonaHeader(RoleTypeInterface role, ToneType tone, StyleType style) {
        StringBuilder sb = new StringBuilder();
        sb.append("あなたは").append(role.getRoleNameByLang(LanguageType.JAPANESE)).append("です。\n");
        sb.append(tone.getGuidelineByLang(LanguageType.JAPANESE))
          .append("で、")
          .append(style.getGuidelineByLang(LanguageType.JAPANESE))
          .append("で回答してください。");
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
                    .map(r -> r.description().ja())
                    .collect(java.util.stream.Collectors.joining("。")));
        }

        if (!forbids.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(forbids.stream()
                    .map(r -> r.description().ja() + " 禁止")
                    .collect(java.util.stream.Collectors.joining("。")));
        }

        if (!allows.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(allows.stream()
                    .map(r -> r.description().ja() + " 許可")
                    .collect(java.util.stream.Collectors.joining("。")));
        }

        return sb.toString();
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

    /**
     * 簡潔な形式でルールをレンダリング（一行）
     */
    private String renderCompactRule(GuidelineRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.title().ja()).append(": ").append(rule.description().ja());

        if (rule.type() == RuleType.FORBID) {
            sb.append(" 禁止");
        }

        return sb.toString();
    }
}

