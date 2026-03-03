package org.example.sharedprompts.domain.prompt.application.service.strategy;

import org.example.sharedprompts.domain.prompt.application.service.orchestration.PromptGenerator;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
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

        String domainStrategyText = switch (domain) {
            case TECHNICAL -> """
                    This is a TECHNICAL domain prompt. The generated prompt must:
                    - Name specific technologies, tools, patterns, or methodologies relevant to the topic
                    - Request concrete implementation details (configurations, architecture decisions, code patterns)
                    - Include performance, security, and scalability aspects as sub-tasks
                    - Use precise technical terminology — avoid vague phrases without specifying them
                    - Break down the request into 3-5 specific deliverable areas
                    """;
            case CREATIVE -> """
                    This is a CREATIVE domain prompt. The generated prompt must:
                    - Encourage original perspectives and unexpected angles
                    - Emphasize emotional resonance and sensory details
                    - Allow open-ended exploration without forcing rigid structure
                    - Avoid clinical or list-based formatting — let the creative flow naturally
                    - Specify the desired mood, atmosphere, or emotional tone
                    """;
            case ANALYTICAL -> """
                    This is an ANALYTICAL domain prompt. The generated prompt must:
                    - Request evidence-based reasoning with data and concrete examples
                    - Demand multi-perspective analysis including pros, cons, and trade-offs
                    - Require clear cause-effect relationships and logical structure
                    - Ask for a summary/conclusion at the beginning followed by detailed analysis
                    - Distinguish between facts and interpretations explicitly
                    """;
            case PRACTICAL -> """
                    This is a PRACTICAL domain prompt. The generated prompt must:
                    - Request immediately actionable steps and real-world deliverables
                    - Demand concrete outputs (templates, checklists, step-by-step guides)
                    - Include realistic constraints (time, budget, resources) as context
                    - Ask for expected outcomes for each recommended action
                    - Focus on results over theory — minimize background explanation
                    """;
            case EDUCATIONAL -> """
                    This is an EDUCATIONAL domain prompt. The generated prompt must:
                    - Build progressively from foundational concepts to advanced topics
                    - Request real-world analogies and examples to illustrate concepts
                    - Include self-check questions or practice exercises
                    - Define key terms before using them in complex contexts
                    - Specify the target learner's level and adapt language accordingly
                    """;
            case GENERAL -> """
                    This is a GENERAL domain prompt. The generated prompt must:
                    - Adapt to the specific nature of the user's request
                    - Provide balanced, well-rounded guidance
                    - Keep language accessible and universally useful
                    """;
        };

        return "## Generation Strategy\n" +
                domainStrategyText +
                "\n";
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
        if (items.isEmpty()) {
            return "";
        }
        return "## Quality Hints\n" +
                "The generated prompt should encourage these qualities in the AI's response:\n" +
                items +
                "\n";
    }
}
