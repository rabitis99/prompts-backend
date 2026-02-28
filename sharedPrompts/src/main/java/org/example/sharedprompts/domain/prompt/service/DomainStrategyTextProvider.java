package org.example.sharedprompts.domain.prompt.service;

import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.guideline.RuleLevel;
import org.example.sharedprompts.global.util.ValidationUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 도메인별 메타프롬프트 문단 제공.
 * <p>{@link PromptGenerator}의 도메인 전략·품질 힌트 문단을 분리하여 단일 책임을 갖는다.</p>
 */
@Component
public class DomainStrategyTextProvider {

    /**
     * 도메인별 AI 생성 전략 문단 — 도메인 특성에 맞는 프롬프트 생성 방향 지시
     */
    public String buildDomainStrategy(TaskDomain domain) {
        ValidationUtils.requireNonNull(domain, "domain");
        StringBuilder strategy = new StringBuilder();
        strategy.append("## Generation Strategy\n");

        String domainStrategyText = switch (domain) {
            case TECHNICAL -> "This is a TECHNICAL domain prompt. The generated prompt must:\n"
                              + "- Name specific technologies, tools, patterns, or methodologies relevant to the topic\n"
                              + "- Request concrete implementation details (configurations, architecture decisions, code patterns)\n"
                              + "- Include performance, security, and scalability aspects as sub-tasks\n"
                              + "- Use precise technical terminology — avoid vague phrases without specifying them\n"
                              + "- Break down the request into 3-5 specific deliverable areas\n";
            case CREATIVE -> "This is a CREATIVE domain prompt. The generated prompt must:\n"
                             + "- Encourage original perspectives and unexpected angles\n"
                             + "- Emphasize emotional resonance and sensory details\n"
                             + "- Allow open-ended exploration without forcing rigid structure\n"
                             + "- Avoid clinical or list-based formatting — let the creative flow naturally\n"
                             + "- Specify the desired mood, atmosphere, or emotional tone\n";
            case ANALYTICAL -> "This is an ANALYTICAL domain prompt. The generated prompt must:\n"
                               + "- Request evidence-based reasoning with data and concrete examples\n"
                               + "- Demand multi-perspective analysis including pros, cons, and trade-offs\n"
                               + "- Require clear cause-effect relationships and logical structure\n"
                               + "- Ask for a summary/conclusion at the beginning followed by detailed analysis\n"
                               + "- Distinguish between facts and interpretations explicitly\n";
            case PRACTICAL -> "This is a PRACTICAL domain prompt. The generated prompt must:\n"
                              + "- Request immediately actionable steps and real-world deliverables\n"
                              + "- Demand concrete outputs (templates, checklists, step-by-step guides)\n"
                              + "- Include realistic constraints (time, budget, resources) as context\n"
                              + "- Ask for expected outcomes for each recommended action\n"
                              + "- Focus on results over theory — minimize background explanation\n";
            case EDUCATIONAL -> "This is an EDUCATIONAL domain prompt. The generated prompt must:\n"
                                + "- Build progressively from foundational concepts to advanced topics\n"
                                + "- Request real-world analogies and examples to illustrate concepts\n"
                                + "- Include self-check questions or practice exercises\n"
                                + "- Define key terms before using them in complex contexts\n"
                                + "- Specify the target learner's level and adapt language accordingly\n";
            case GENERAL -> "This is a GENERAL domain prompt. The generated prompt must:\n"
                            + "- Adapt to the specific nature of the user's request\n"
                            + "- Provide balanced, well-rounded guidance\n"
                            + "- Keep language accessible and universally useful\n";
        };
        strategy.append(domainStrategyText);

        strategy.append("\n");
        return strategy.toString();
    }

    /**
     * SOFT 규칙을 AI 생성 품질 힌트 문단으로 변환
     */
    public String buildQualityHints(TaskDomain domain) {
        ValidationUtils.requireNonNull(domain, "domain");
        List<GuidelineRule> softRules = domain.getRulesByLevel(RuleLevel.SOFT);
        if (softRules.isEmpty()) {
            return "";
        }
        StringBuilder items = new StringBuilder();
        for (GuidelineRule rule : softRules) {
            if (rule == null || rule.description() == null) {
                continue;
            }
            String descriptionEn = rule.description().en();
            if (descriptionEn == null || descriptionEn.isBlank()) {
                continue;
            }
            items.append("- ").append(descriptionEn).append("\n");
        }
        if (items.length() == 0) {
            return "";
        }
        StringBuilder hints = new StringBuilder();
        hints.append("## Quality Hints\n");
        hints.append("The generated prompt should encourage these qualities in the AI's response:\n");
        hints.append(items);
        hints.append("\n");
        return hints.toString();
    }
}
