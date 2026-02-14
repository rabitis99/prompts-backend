package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.role.EtcRoleType;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.guideline.RuleLevel;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptGenerator {

    private final DomainResolver domainResolver;

    public String generatePrompt(InputRequestDto request) {
        return buildMetaPrompt(request);
    }

    private String buildMetaPrompt(InputRequestDto request) {
        TaskDomain domain = resolveDomain(request);
        StringBuilder metaPrompt = new StringBuilder();

        // === SYSTEM ROLE ===
        metaPrompt.append("You are an expert AI prompt engineer. Transform the user's input into a concise, high-quality prompt.\n\n");

        // === HARD RULES ===
        metaPrompt.append("## Hard Rules\n")
                .append("- Output ONLY the core prompt body (no role assignment, no tone/style labels, no explanations, no greetings).\n")
                .append("- The role, tone, and style headers will be added separately — do NOT include them.\n")
                .append("- Target approximately 450-500 characters total.\n")
                .append("- Break long thoughts into 2-4 clear sentences. Each sentence focuses on one main point.\n\n");

        // === DOMAIN-SPECIFIC GENERATION STRATEGY ===
        metaPrompt.append(buildDomainStrategy(domain));

        // === QUALITY HINTS FROM SOFT RULES ===
        metaPrompt.append(buildQualityHints(domain));

        // === CORE TASK ===
        metaPrompt.append(buildEnhancedRequest(request));

        return metaPrompt.toString();
    }

    /**
     * 도메인별 AI 생성 전략 — 도메인 특성에 맞는 프롬프트 생성 방향을 지시
     */
    private String buildDomainStrategy(TaskDomain domain) {
        StringBuilder strategy = new StringBuilder();
        strategy.append("## Generation Strategy\n");

        switch (domain) {
            case TECHNICAL -> strategy.append(
                    "This is a TECHNICAL domain prompt. The generated prompt must:\n"
                    + "- Name specific technologies, tools, patterns, or methodologies relevant to the topic\n"
                    + "- Request concrete implementation details (configurations, architecture decisions, code patterns)\n"
                    + "- Include performance, security, and scalability aspects as sub-tasks\n"
                    + "- Use precise technical terminology — avoid vague phrases without specifying them\n"
                    + "- Break down the request into 3-5 specific deliverable areas\n");
            case CREATIVE -> strategy.append(
                    "This is a CREATIVE domain prompt. The generated prompt must:\n"
                    + "- Encourage original perspectives and unexpected angles\n"
                    + "- Emphasize emotional resonance and sensory details\n"
                    + "- Allow open-ended exploration without forcing rigid structure\n"
                    + "- Avoid clinical or list-based formatting — let the creative flow naturally\n"
                    + "- Specify the desired mood, atmosphere, or emotional tone\n");
            case ANALYTICAL -> strategy.append(
                    "This is an ANALYTICAL domain prompt. The generated prompt must:\n"
                    + "- Request evidence-based reasoning with data and concrete examples\n"
                    + "- Demand multi-perspective analysis including pros, cons, and trade-offs\n"
                    + "- Require clear cause-effect relationships and logical structure\n"
                    + "- Ask for a summary/conclusion at the beginning followed by detailed analysis\n"
                    + "- Distinguish between facts and interpretations explicitly\n");
            case PRACTICAL -> strategy.append(
                    "This is a PRACTICAL domain prompt. The generated prompt must:\n"
                    + "- Request immediately actionable steps and real-world deliverables\n"
                    + "- Demand concrete outputs (templates, checklists, step-by-step guides)\n"
                    + "- Include realistic constraints (time, budget, resources) as context\n"
                    + "- Ask for expected outcomes for each recommended action\n"
                    + "- Focus on results over theory — minimize background explanation\n");
            case EDUCATIONAL -> strategy.append(
                    "This is an EDUCATIONAL domain prompt. The generated prompt must:\n"
                    + "- Build progressively from foundational concepts to advanced topics\n"
                    + "- Request real-world analogies and examples to illustrate concepts\n"
                    + "- Include self-check questions or practice exercises\n"
                    + "- Define key terms before using them in complex contexts\n"
                    + "- Specify the target learner's level and adapt language accordingly\n");
            case GENERAL -> strategy.append(
                    "This is a GENERAL domain prompt. The generated prompt must:\n"
                    + "- Adapt to the specific nature of the user's request\n"
                    + "- Provide balanced, well-rounded guidance\n"
                    + "- Keep language accessible and universally useful\n");
            default -> strategy.append(
                    "Adapt the prompt generation to the specific needs of the task.\n");
        }

        strategy.append("\n");
        return strategy.toString();
    }

    /**
     * SOFT 규칙을 AI 생성 품질 힌트로 변환 — 기존에 버려지던 SOFT 규칙을 메타프롬프트에 활용
     */
    private String buildQualityHints(TaskDomain domain) {
        List<GuidelineRule> softRules = domain.getRulesByLevel(RuleLevel.SOFT);

        if (softRules.isEmpty()) {
            return "";
        }

        StringBuilder hints = new StringBuilder();
        hints.append("## Quality Hints\n");
        hints.append("The generated prompt should encourage these qualities in the AI's response:\n");
        for (GuidelineRule rule : softRules) {
            hints.append("- ").append(rule.description().en()).append("\n");
        }
        hints.append("\n");
        return hints.toString();
    }

    /**
     * 핵심 변경 지점
     * - role/tone/style을 AI 지시에 강하게 반영
     * - action type을 명시하여 방향성을 제공
     * - experience level을 구체적 생성 지시로 변환
     * - tag를 의미적으로 통합하여 자연스러운 제약으로 반영
     */
    private String buildEnhancedRequest(InputRequestDto request) {
        StringBuilder section = new StringBuilder();

        // 1. User Input (PRIMARY)
        section.append("## User Input\n")
                .append("\"\"\"\n")
                .append(request.getInput())
                .append("\n\"\"\"\n\n")
                .append("**Task**: Rewrite this into a clear, actionable prompt body that breaks down the topic into specific, concrete sub-tasks.\n\n");

        // 2. Action Context — 작업 유형을 명시하여 AI가 방향성을 잡도록 함
        // null 체크: PromptRequestDto.toInputRequestDto()에서 기본값 제공하지만, 방어적 코딩
        var actionType = request.getActionType() != null 
                ? request.getActionType() 
                : EtcActionType.GENERAL_CONSULTATION;
        section.append("## Action Context\n")
                .append("The user wants to perform: **")
                .append(actionType.getDisplayNameEn())
                .append("**\n")
                .append("The generated prompt must be specifically structured for this type of task.\n\n");

        // 3. Role Context — AI가 역할을 인지하고 그에 맞는 프롬프트를 작성
        // null 체크: PromptRequestDto.toInputRequestDto()에서 기본값 제공하지만, 방어적 코딩
        var roleType = request.getRoleType() != null 
                ? request.getRoleType() 
                : EtcRoleType.GENERAL_CONSULTANT;
        section.append("## Role Context\n")
                .append("The prompt is for an AI acting as **")
                .append(roleType.getRoleNameEn())
                .append("** (").append(roleType.getDescriptionEn()).append(").\n")
                .append("Write instructions that naturally assume this role's expertise and perspective.\n\n");

        // 4. Tone & Style — 강한 지시로 변경
        // null 체크: PromptRequestDto.toInputRequestDto()에서 기본값 제공하지만, 방어적 코딩
        var tone = request.getTone() != null ? request.getTone() : ToneType.NEUTRAL;
        var style = request.getStyle() != null ? request.getStyle() : StyleType.NARRATIVE;
        section.append("## Tone & Style\n")
                .append("- **Tone**: ").append(tone.getGuidelineEn())
                .append(" — the prompt body must reflect this tone in its wording and phrasing.\n")
                .append("- **Style**: ").append(style.getGuidelineEn())
                .append(" — structure the prompt body to match this format.\n\n");

        // 5. Experience Level (구체적 생성 지시로 변환)
        if (request.getExperience() != null) {
            section.append("## Experience Level\n")
                    .append(buildExperienceDirective(request.getExperience()))
                    .append("\n");
        }

        // 6. Domain Context
        // null 체크: PromptRequestDto에서 @NotNull이지만, 방어적 코딩
        if (request.getPromptCategory() != null) {
            section.append("## Domain Context\n")
                    .append("**Category**: ").append(request.getPromptCategory().getDisplayName()).append("\n")
                    .append("**Guideline**: ").append(request.getPromptCategory().getGuidelineEn()).append("\n\n");
        }

        // 7. Tags (의미적 통합)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<String> normalizedTags = TagNormalizer.normalizeTags(request.getTags());
            if (!normalizedTags.isEmpty()) {
                section.append("## Key Themes\n")
                        .append(formatTagsAsConstraints(normalizedTags))
                        .append("\n\n");
            }
        }

        // 8. Output Language
        if (request.getLanguage() != null) {
            section.append("## Language\n")
                    .append("Generate the prompt body in ")
                    .append(request.getLanguage().getDescription())
                    .append(" (")
                    .append(request.getLanguage().getPromptToken())
                    .append(").\n\n");
        }

        // 9. Key Requirements
        section.append("## Requirements\n")
                .append("- Make it immediately actionable and model-agnostic\n")
                .append("- Use natural, original phrasing (avoid generic templates)\n")
                .append("- Reflect the specified tone and style naturally throughout\n")
                .append("- Break down the topic into 3-5 specific sub-areas rather than making a single vague request\n");

        return section.toString();
    }

    /**
     * Experience Level을 구체적인 생성 지시로 변환
     * (기존: 단순 라벨 → 개선: AI가 깊이/복잡도를 조절하도록 구체적 지시)
     */
    private String buildExperienceDirective(ExperienceLevel experience) {
        StringBuilder directive = new StringBuilder();
        directive.append("- **Level**: ").append(experience.getGuidelineEn()).append("\n");

        switch (experience) {
            case BEGINNER -> directive.append(
                    "  → The prompt must request simplified explanations, minimal jargon, "
                    + "step-by-step guidance, and basic real-world examples.\n");
            case INTERMEDIATE -> directive.append(
                    "  → The prompt must assume foundational knowledge, focus on practical nuances, "
                    + "common pitfalls, and actionable best practices.\n");
            case ADVANCED -> directive.append(
                    "  → The prompt must request in-depth analysis, optimization techniques, "
                    + "architectural trade-offs, and advanced design patterns.\n");
            case EXPERT -> directive.append(
                    "  → The prompt must demand cutting-edge insights, edge-case handling, "
                    + "performance benchmarks, and expert-level architectural decisions.\n");
            default -> directive.append(
                    "  → Adapt the depth and complexity to the user's experience level.\n");
        }

        return directive.toString();
    }

    /**
     * Tag를 의미적으로 통합하여 자연스러운 제약으로 변환
     * (기존: "Consider aspects related to: X" → 개선: 구체적 sub-topic으로 직조)
     * 
     * @param normalizedTags 이미 정규화된 태그 리스트 (비어있지 않음을 보장)
     */
    private String formatTagsAsConstraints(List<String> normalizedTags) {
        String tagList = String.join(", ", normalizedTags);
        return "The prompt must specifically address these themes: **" + tagList + "**.\n"
                + "Weave these concepts naturally into the prompt as concrete sub-topics or constraints — "
                + "do not list them as generic bullet points.";
    }

    /**
     * 도메인 결정 (DomainResolver 사용)
     */
    private TaskDomain resolveDomain(InputRequestDto request) {
        return domainResolver.resolveDomainSimple(request);
    }
}
