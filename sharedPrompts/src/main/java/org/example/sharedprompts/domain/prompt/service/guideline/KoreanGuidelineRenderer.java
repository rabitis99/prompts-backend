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
 * 한국어 가이드라인 렌더러
 */
@Component
@RequiredArgsConstructor
public class KoreanGuidelineRenderer implements GuidelineRenderer {

    @Override
    public String renderPrinciples(List<GuidelineRule> rules) {
        if (rules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("원칙\n");

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

        StringBuilder sb = new StringBuilder("응답 규칙\n");

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

        // 품질 기준은 출력 제약사항과 통합하여 간소화
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
        return "## " + notice.title().ko() + "\n\n" + notice.description().ko();
    }

    @Override
    public String renderPersonaHeader(RoleTypeInterface role, ToneType tone, StyleType style) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 ").append(role.getRoleNameByLang(LanguageType.KOREAN)).append("입니다.\n");
        sb.append(tone.getGuidelineByLang(LanguageType.KOREAN))
          .append("로, ")
          .append(style.getGuidelineByLang(LanguageType.KOREAN))
          .append(" 형식으로 답변하세요.");
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
                    .map(r -> r.description().ko())
                    .collect(java.util.stream.Collectors.joining(". ")));
        }

        if (!forbids.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(forbids.stream()
                    .map(r -> r.description().ko() + " 금지")
                    .collect(java.util.stream.Collectors.joining(". ")));
        }

        if (!allows.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(allows.stream()
                    .map(r -> r.description().ko() + " 허용")
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
            case BEGINNER -> "전문 용어를 최소화하고, 기초부터 단계별로 설명하세요.";
            case INTERMEDIATE -> "기본 개념은 가정하고, 실용적 세부사항과 주의점에 집중하세요.";
            case ADVANCED -> "심화 내용, 최적화 기법, 설계 트레이드오프를 포함하세요.";
            case EXPERT -> "최신 동향, 엣지 케이스, 성능 벤치마크, 고급 아키텍처 판단을 다루세요.";
        };
    }

    /**
     * 간결한 형식으로 규칙 렌더링 (한 줄)
     */
    private String renderCompactRule(GuidelineRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.title().ko()).append(": ").append(rule.description().ko());

        if (rule.type() == RuleType.FORBID) {
            sb.append(" 금지");
        }

        return sb.toString();
    }
}

