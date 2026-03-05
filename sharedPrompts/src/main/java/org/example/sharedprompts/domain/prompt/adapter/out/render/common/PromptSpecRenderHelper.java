package org.example.sharedprompts.domain.prompt.adapter.out.render.common;

import org.example.sharedprompts.domain.prompt.domain.model.spec.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.value.strategy.PromptingStrategy;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PromptSpec 렌더링 시 공통적으로 사용하는 헬퍼.
 *
 * <p>비즈니스 규칙(전략·제약 조건·섹션 렌더링 등)은 모두 이 클래스에 모아
 * Repair/Initial 렌더러가 동일한 규칙을 사용하도록 보장한다.</p>
 */
public final class PromptSpecRenderHelper {

    private PromptSpecRenderHelper() {}

    /**
     * 전략 목록을 결정론적으로 정렬한 후, 한 줄당 하나의 전략 설명으로 렌더링한다.
     */
    public static String renderStrategies(Set<PromptingStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            return "";
        }

        return strategies.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(s -> "- **" + s.name() + "** [" + s.getTier() + "]: " + describeStrategy(s))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    /**
     * 섹션 목록을 구조 안내용 텍스트로 렌더링한다.
     */
    public static String renderSections(List<PromptSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (PromptSection section : sections) {
            if (section == null) {
                continue;
            }
            String content = section.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            sb.append("- Section <")
                    .append(section.getType().name())
                    .append(">\n")
                    .append("  ")
                    .append(content.trim())
                    .append("\n");
        }

        return sb.toString();
    }

    /**
     * 제약 조건을 공통 규칙에 따라 텍스트로 렌더링한다.
     */
    public static String renderConstraints(Constraints constraints) {
        if (constraints == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 하드 룰: 최종 출력 범위 및 품질
        sb.append("- The final answer must ONLY contain the core prompt body for the target AI model (no explanations, analysis, or commentary).\n");
        sb.append("- Do not include the meta section headers from this meta-prompt (e.g., [OBJECTIVE], [STRATEGIES]) or system-message style wrappers in the final answer.\n");
        sb.append("- Prefer precise, unambiguous wording and strictly follow the requested output structure.\n");
        sb.append("- Do not fabricate requirements or domain facts; base the prompt only on the specification and [USER INPUT].\n");
        sb.append("- Do not invent hidden assumptions. If assumptions are required, state them explicitly and keep them minimal.\n");
        sb.append("- If any information is uncertain or ambiguous, make that uncertainty explicit in the prompt instead of inventing details.\n");
        sb.append("- If the user's input is underspecified and clarification questions are allowed, include a brief \"clarify first\" instruction in the prompt body to request any missing details before proceeding.\n");
        sb.append("- If clarification is required, ask only the minimum number of questions (typically 1–3) necessary to proceed.\n");
        sb.append("- If the specification disables clarification questions, do not ask questions and instead proceed using only the available information.\n");
        sb.append("- A high-quality prompt should clearly reflect the user's intent, be concise and well organized, and steer the target AI toward accurate, useful responses.\n");
        sb.append("- When rules seem to conflict, follow this priority: (1) system-level policies and specification, (2) constraints, (3) output format details, (4) strategies.\n");

        // 단계별 추론
        if (constraints.isRequireStepByStep()) {
            sb.append("- When designing the prompt, ensure the instructions are logically ordered and complete, while keeping the final prompt concise.\n");
        }

        // 인용 / 근거
        if (constraints.isRequireCitations()) {
            sb.append("- Instruct the target AI to include citations or uncertainty markers when referencing facts.\n");
        }

        // 필수 키워드
        if (!constraints.getRequiredKeywords().isEmpty()) {
            String joined = String.join(", ", constraints.getRequiredKeywords());
            sb.append("- Required keywords: ").append(joined).append(".\n");
        }

        // 금지 키워드
        if (!constraints.getProhibitedKeywords().isEmpty()) {
            String joined = String.join(", ", constraints.getProhibitedKeywords());
            sb.append("- Do NOT use: ").append(joined).append(".\n");
        }

        // 길이 제한
        if (constraints.getMinLength() != null) {
            sb.append("- Min length: ").append(constraints.getMinLength()).append(" characters.\n");
        }
        if (constraints.getMaxLength() != null) {
            sb.append("- Max length: ").append(constraints.getMaxLength()).append(" characters.\n");
        }

        return sb.toString();
    }

    private static String describeStrategy(PromptingStrategy strategy) {
        if (strategy == null) {
            return "";
        }

        switch (strategy) {
            case CLARIFY_FIRST:
                return "Resolve ambiguity by restating what the user needs in your own words before shaping the prompt.";
            case STEP_BY_STEP:
                return "Think through the task step by step to design a logically ordered prompt.";
            case CHECKLIST_VERIFY:
                return "Use a mental checklist to ensure the prompt covers all key requirements and important edge cases.";
            case DECOMPOSITION:
                return "Break complex or multi-part tasks into smaller, ordered instructions within the prompt.";
            case CITE_OR_UNCERTAIN:
                return "Instruct the target AI to cite sources or state uncertainty instead of guessing when facts are unclear.";
            case REQUIRE_JUSTIFICATION:
                return "Require the target AI to briefly justify important decisions or conclusions when appropriate.";
            case FEW_SHOT_EXEMPLAR:
                return "Include a few short, targeted examples only when they make the task clearer for the target AI.";
            case CHAIN_OF_VERIFICATION:
                return "Encourage the target AI to verify its own output against the requirements before finalizing.";
            case EDGE_CASE_SCAN:
                return "Guide the target AI to consider edge cases and non‑happy‑path scenarios that matter for the user.";
            case SELF_CONSISTENCY:
                return "Have the target AI compare multiple internal candidates and keep the most consistent result.";
            case TREE_OF_THOUGHTS:
                return "Encourage the target AI to explore multiple solution paths briefly before choosing a final direction.";
            default:
                return "Apply this strategy as appropriate to improve reliability, coverage, and reasoning quality.";
        }
    }
}
