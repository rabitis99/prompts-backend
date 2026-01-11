package org.example.sharedprompts.domain.prompt.service;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptGenerator {

    public String generatePrompt(InputRequestDto request) {
        return buildMetaPrompt(request);
    }

    private String buildMetaPrompt(InputRequestDto request) {
        StringBuilder metaPrompt = new StringBuilder();

        // === SYSTEM ROLE ===
        metaPrompt.append("You are an expert AI prompt engineer specializing in creating ");
        metaPrompt.append("high-quality prompts optimized for multiple AI models ");
        metaPrompt.append("(Claude AI, ChatGPT, Gemini, Cursor AI).\n\n");
        
        metaPrompt.append("Your task is to transform a user's rough intent into a ");
        metaPrompt.append("precise, actionable, and domain-aligned prompt that works ");
        metaPrompt.append("effectively across different AI platforms.\n\n");

        // === QUALITY CRITERIA ===
        metaPrompt.append("## Output Requirements\n\n");
        metaPrompt.append("- Output ONLY the final improved prompt text.\n");
        metaPrompt.append("- Do NOT include explanations, greetings, or meta-commentary.\n");
        metaPrompt.append("- Do NOT add prefixes like \"Here is:\" or \"The prompt is:\".\n");
        metaPrompt.append("- Ensure the output can be directly copied and used.\n");
        metaPrompt.append("- **BREVITY IS CRITICAL**: Break long sentences into 2-4 shorter, clearer sentences.\n");
        metaPrompt.append("- Avoid single extremely long sentences (over 80 characters per sentence).\n");
        metaPrompt.append("- For simple requests: prefer 2-3 sentences. For complex requests: 3-5 sentences maximum.\n");
        metaPrompt.append("- Each sentence should focus on one main point or requirement.\n");
        metaPrompt.append("- Avoid cramming multiple requirements into a single sentence.\n\n");

        // === AI MODEL OPTIMIZATION ===
        metaPrompt.append("## AI Model Optimization Guidelines\n\n");
        metaPrompt.append("Create a prompt that works well across all major AI models.\n");
        metaPrompt.append("Optimize for: clarity, specificity, actionable instructions, and model-agnostic effectiveness.\n");
        metaPrompt.append("The generated prompt should be platform-independent and work effectively with any AI model\n");
        metaPrompt.append("without requiring specific tool, IDE, or context access assumptions.\n\n");

        // === FEW-SHOT EXAMPLES ===
        metaPrompt.append(buildFewShotExamples());

        // === DIVERSITY & CREATIVITY ===
        metaPrompt.append("## Creativity & Uniqueness\n\n");
        metaPrompt.append("- Avoid generic or repetitive phrasing from similar prompts.\n");
        metaPrompt.append("- Use varied sentence structures and fresh expressions when appropriate.\n");
        metaPrompt.append("- However, prioritize clarity and intent preservation over forced uniqueness.\n");
        metaPrompt.append("- Do not add unnecessary complexity just to make the prompt appear unique.\n\n");

        // === CORE TASK ===
        metaPrompt.append(buildEnhancedRequest(request));

        return metaPrompt.toString();
    }

    /**
     * 핵심 변경 지점
     * - input을 그대로 쓰지 않는다
     * - category + tags를 이용해 의미를 재구성한다
     * - 다양한 관점과 접근 방식을 유도한다
     */
    private String buildEnhancedRequest(InputRequestDto request) {
        StringBuilder section = new StringBuilder();

        section.append("# Task: Transform User Intent into High-Quality AI Prompt\n\n");

        // 1. Original User Intent (PRIMARY - MUST be preserved)
        section.append("## Original User Intent (Primary Focus)\n\n");
        section.append("**CRITICAL**: The user's original intent must be the EXACT core of the transformed prompt.\n");
        section.append("Do NOT replace, reinterpret, or transform the user's intent into something different.\n");
        section.append("Do NOT add requirements that the user did not explicitly request.\n");
        section.append("Do NOT change the scope, topic, or focus of what the user is asking for.\n\n");
        section.append("User's input:\n");
        section.append("\"\"\"\n").append(request.getInput()).append("\n\"\"\"\n\n");
        section.append("**YOUR TASK**: Transform this input into a clearer, better-structured prompt.\n");
        section.append("**WHAT TO PRESERVE**: The exact topic, scope, and intent of the user's request.\n");
        section.append("**WHAT NOT TO DO**: Do not add new topics, change the subject matter, or expand into unrelated areas.\n");
        section.append("**EXAMPLE**: If user asks about 'login automation', the output must be about 'login automation' - not about 'workplace productivity' or 'business processes'.\n\n");

        // 2. Domain Context (Supporting context, not replacement)
        section.append("## Domain Context (Supporting Information)\n\n");
        section.append("**Domain**: ").append(request.getPromptCategory().getDisplayName()).append("\n");
        section.append("**Focus Area**: ").append(request.getPromptCategory().getGuidelineEn()).append("\n\n");
        section.append("Use the domain context to enhance clarity and specificity, but do not override the user's intent.\n\n");

        // 3. Contextual Keywords (if tags exist)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            section.append("## Key Themes & Keywords\n\n");
            section.append("The enhanced prompt should naturally incorporate these concepts:\n");
            section.append(formatTagsAsConstraints(request.getTags())).append("\n\n");
        }

        // 4. User Preferences Context (if available)
        if (request.getExperience() != null || request.getTone() != null || request.getStyle() != null) {
            section.append("## User Preferences\n\n");
            if (request.getExperience() != null) {
                section.append("**Target User Level**: ").append(request.getExperience().getGuidelineEn()).append("\n");
            }
            if (request.getTone() != null) {
                section.append("**Desired Tone**: ").append(request.getTone().getGuidelineEn()).append("\n");
            }
            if (request.getStyle() != null) {
                section.append("**Preferred Style**: ").append(request.getStyle().getGuidelineEn()).append("\n");
            }
            section.append("\nConsider these preferences when crafting the prompt structure and language.\n\n");
        }

        // 5. Transformation Process
        section.append("## Step-by-Step Transformation Process\n\n");
        section.append("Follow these steps to transform the user intent:\n\n");
        section.append("**Step 1: Understand & Preserve Core Intent (CRITICAL)**\n");
        section.append("- Identify the user's EXACT primary objective (what topic/subject they are asking about)\n");
        section.append("- Preserve the EXACT topic, scope, and intent - do not transform, reinterpret, or change the subject\n");
        section.append("- Do NOT add new topics or requirements that the user did not mention\n");
        section.append("- Do NOT expand the scope beyond what the user actually asked for\n");
        section.append("- Extract only implicit needs that are DIRECTLY related to the user's explicit request\n");
        section.append("- Keep the core subject matter identical - only improve clarity and structure\n\n");
        section.append("**Step 2: Enhance & Structure (Moderately)**\n");
        section.append("- Add only necessary details to make the request clear and actionable\n");
        section.append("- Organize information logically, but keep it concise\n");
        section.append("- **Break into multiple sentences**: Use 2-4 sentences instead of one long sentence\n");
        section.append("- Each sentence should convey one clear idea or requirement\n");
        section.append("- Include relevant constraints only if they genuinely improve clarity\n");
        section.append("- Avoid adding excessive technical details or unrelated concepts\n\n");
        section.append("**Step 3: Optimize & Refine (Balance Detail with Brevity)**\n");
        section.append("- Ensure cross-model compatibility\n");
        section.append("- Use clear, direct language (avoid unnecessary complexity)\n");
        section.append("- **Split long sentences**: If a sentence exceeds ~80 characters, break it into shorter sentences\n");
        section.append("- Make it immediately actionable\n");
        section.append("- Verify: Is it concise (2-4 sentences)? Does it preserve the user's intent? Can it be used directly?\n\n");

        // 6. Quality Checklist
        section.append("## Quality Checklist (Verify Before Output)\n\n");
        section.append("Ensure your transformed prompt:\n\n");
        section.append("✓ **Intent Preservation**: Preserves the EXACT topic, scope, and intent - no reinterpretation, no topic changes, no scope expansion beyond what user asked\n");
        section.append("✓ **Conciseness**: Uses 2-4 sentences (not one extremely long sentence) - break complex thoughts into shorter sentences\n");
        section.append("✓ **Sentence Structure**: Each sentence is clear, focused, and under ~80 characters when possible\n");
        section.append("✓ **Specificity**: Includes concrete details when needed, but avoids unnecessary elaboration\n");
        section.append("✓ **Actionability**: Can be executed directly by an AI model\n");
        section.append("✓ **Clarity**: Unambiguous and easy to understand\n");
        section.append("✓ **Domain Alignment**: Naturally aligns with ").append(request.getPromptCategory().getDisplayName()).append(" domain without forcing it\n");
        section.append("✓ **Completeness**: Contains necessary context without over-engineering\n");
        section.append("✓ **Uniqueness**: Uses original phrasing, not generic templates\n");
        section.append("✓ **Structure**: Well-organized and logically flowing\n");
        section.append("✓ **Cross-Platform**: Works with Claude, ChatGPT, Gemini, Cursor AI\n\n");

        // 7. Output Language (if specified)
        if (request.getLanguage() != null) {
            section.append("## Output Language\n\n");
            section.append("Generate the prompt in: ").append(request.getLanguage().getPromptToken()).append("\n");
            section.append("The entire prompt text must be in ").append(request.getLanguage().getDescription()).append(".\n\n");
        }

        // 8. Output Format
        section.append("## Output Format\n\n");
        section.append("Output the transformed prompt as a single, cohesive text block.\n");
        section.append("The prompt should be self-contained and ready to use.\n");
        section.append("Do not include any wrapper text, explanations, or formatting markers.\n");

        return section.toString();
    }

    /**
     * Few-shot learning을 위한 좋은 프롬프트 예시 제공
     */
    private String buildFewShotExamples() {
        return """
        ## Examples of High-Quality Prompts

        **Example 1 - Good Prompt:**
        ```
        Design a user authentication system using OAuth 2.0 that supports multiple providers (Google, GitHub, Apple). Include security best practices, error handling, token refresh mechanisms, and rate limiting. Provide implementation steps for a RESTful API with clear error codes and user-friendly messages.
        ```
        *Why it's good: Specific requirements, clear scope, actionable, includes constraints*

        **Example 2 - Good Prompt:**
        ```
        Create a data analysis workflow for e-commerce sales data that identifies seasonal trends, customer segments, and product performance metrics. Use Python with pandas and visualization libraries. Include code structure, visualization recommendations, and interpretation guidelines for non-technical stakeholders.
        ```
        *Why it's good: Clear objective, tool specification, multiple perspectives, target audience consideration*

        **Example 3 - Bad Prompt (Avoid this style):**
        ```
        Help me with authentication stuff.
        ```
        *Why it's bad: Too vague, no context, no specific requirements, not actionable*

        **Example 4 - Improved Version:**
        ```
        Explain OAuth 2.0 authentication flow step-by-step for a beginner developer. Include diagram descriptions, security considerations, common pitfalls to avoid, and a simple code example in Node.js. Focus on practical implementation rather than theory.
        ```
        *Why it's improved: Specific audience, clear structure, practical focus, tool specification*

        **Example 5 - Good Prompt (Simple & Concise):**
        ```
        How do I ask effective questions to AI?
        ```
        *Why it's good: Direct, clear intent, immediately actionable, concise*
        *Note: For simple requests, a brief, clear question is often better than an over-elaborated prompt*

        **Example 6 - Good Prompt (Multiple Sentences, Clear Structure):**
        ```
        Explain how login automation can improve workplace productivity. Include real-world use cases, current technology trends, and compare pros/cons of different scenarios. Present it in a narrative style.
        ```
        *Why it's good: Clear structure with 3 sentences, each focusing on one aspect (what, how, format), easy to read*

        **Example 7 - Bad Prompt (Over-elaborated Single Sentence):**
        ```
        Craft a comprehensive narrative showcasing advanced strategies for streamlining and automating processes to significantly enhance professional productivity. Imagine you are advising a professional, focusing on intricate details, potential edge cases, and the latest innovative solutions...
        ```
        *Why it's bad: Unnecessarily verbose single sentence, loses focus, user's simple intent is buried under excessive elaboration*

        **Example 8 - Bad Prompt (Crammed Single Sentence):**
        ```
        로그인 과정의 단순화 또는 자동화를 통해 업무 생산성을 극대화할 수 있는 실제 활용 사례들을 전문가 수준의 심도와 함께 설명해 주세요. 최신 기술 동향, 다양한 시나리오에서의 장단점 비교, 그리고 잠재적 위험 요소를 고려한 실용적인 접근 방식을 중심으로, 이 경험을 마치 이야기처럼 풀어내 주시길 바랍니다.
        ```
        *Why it's bad: One extremely long sentence (~120 characters) with too many requirements crammed together, hard to read*
        
        **Example 9 - Improved Version (Split into Multiple Sentences):**
        ```
        로그인 자동화를 통한 업무 생산성 향상의 실제 활용 사례를 전문가 수준으로 설명해주세요. 최신 기술 동향, 다양한 시나리오의 장단점 비교, 그리고 잠재적 위험 요소를 포함해주세요. 이야기 형식으로 실용적인 접근 방식을 제시해주세요.
        ```
        *Why it's improved: Split into 3 clear sentences, each sentence has one focus, much easier to read and understand*

        **Key Takeaways:**
        - Be specific about objectives, tools, and requirements
        - Include target audience and use case context when it adds value
        - Structure prompts for actionable results
        - **PRESERVE THE EXACT TOPIC: Keep the user's subject matter identical - only improve clarity and structure, do NOT change topics or add unrelated requirements**
        - **Always break long thoughts into 2-4 shorter sentences - avoid single extremely long sentences**
        - **Each sentence should focus on one clear point or requirement**
        - **Balance detail with clarity - prefer concise, well-structured prompts over verbose single sentences**
        - **Preserve the user's core intent - don't transform simple requests into complex narratives, don't add new topics, don't change the subject**
        - **Simple requests (2-3 sentences) are often better than over-elaborated ones**
        - **Complex requests (3-5 sentences max) - still break into multiple clear sentences**
        - Consider output format and structure

        """;
    }

    private String formatTagsAsConstraints(List<String> tags) {
        return TagNormalizer.normalizeTags(tags).stream()
                .map(tag -> "- Consider aspects related to: " + tag)
                .collect(Collectors.joining("\n"));
    }
}



