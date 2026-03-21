package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentExplanationIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.EXPLAIN,
                        new IntentDefinition(
                                ActionIntent.EXPLAIN,
                                "Make a topic or process understandable; clarify how or why.",
                                "User wants to understand something (concept, process, system) in a clear way.",
                                "When the goal is to teach a curriculum (use TEACH), shorten (use SIMPLIFY), or condense (use SUMMARIZE).",
                                "EXPLAIN = clarify how/why; TEACH = instructional sequence; SIMPLIFY = reduce complexity; SUMMARIZE = condense.",
                                "Step-by-step or structured explanation; pedagogical tone optional.",
                                List.of(PromptCategory.EDUCATION, PromptCategory.DEVELOPMENT, PromptCategory.RESEARCH),
                                List.of("DOCUMENTATION", "TEACHING_METHOD", "RESEARCH_DESIGN"),
                                List.of("EDUCATOR", "RESEARCH_METHODOLOGIST", "FULL_STACK_DEVELOPER"),
                                "Core intent for EDUCATION and STUDY."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.TEACH,
                        new IntentDefinition(
                                ActionIntent.TEACH,
                                "Instruct in a learning sequence; curriculum or lesson oriented.",
                                "User wants to learn a topic in a structured, pedagogical way.",
                                "When the goal is a one-off explanation (EXPLAIN) or simplification of existing content (SIMPLIFY).",
                                "TEACH = instructional sequence; EXPLAIN = single explanation; SIMPLIFY = reduce complexity of given content.",
                                "Step-by-step lesson; may include examples and exercises.",
                                List.of(PromptCategory.EDUCATION),
                                List.of("TEACHING_METHOD", "LESSON_PLANNING", "EXAM_PREPARATION"),
                                List.of("EDUCATOR", "CURRICULUM_DESIGNER", "TUTOR"),
                                "Prefer EXPLAIN for one-off clarification; TEACH for learning paths."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.SIMPLIFY,
                        new IntentDefinition(
                                ActionIntent.SIMPLIFY,
                                "Reduce complexity of given content or concept; make more accessible.",
                                "User has complex material and wants it made easier to understand.",
                                "When the goal is to condense length (use SUMMARIZE) or to explain from scratch (use EXPLAIN).",
                                "SIMPLIFY = reduce complexity; SUMMARIZE = shorten; EXPLAIN = clarify how/why.",
                                "Simpler version of the same content; structure may be reorganized.",
                                List.of(PromptCategory.EDUCATION, PromptCategory.WRITING),
                                List.of("MATERIAL_CREATION", "KNOWLEDGE_ORGANIZATION"),
                                List.of("EDUCATOR", "TECHNICAL_WRITER"),
                                "Often used for technical or academic content simplification."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.SUMMARIZE,
                        new IntentDefinition(
                                ActionIntent.SUMMARIZE,
                                "Condense content to key points; preserve meaning, reduce length.",
                                "User wants a shorter version that captures the main points.",
                                "When the goal is to simplify complexity (SIMPLIFY) or to explain (EXPLAIN).",
                                "SUMMARIZE = condense; SIMPLIFY = reduce complexity; OUTLINE = structure only.",
                                "Bullet list or concise narrative; minimal new content.",
                                List.of(PromptCategory.RESEARCH, PromptCategory.WRITING, PromptCategory.EDUCATION, PromptCategory.BUSINESS),
                                List.of("LITERATURE_REVIEW", "NOTE_TAKING", "ARTICLE_WRITING"),
                                List.of("RESEARCHER", "EDITOR", "STUDY_COACH"),
                                "Output often bullet-list; OutputNeeds.BULLET_LIST_REQUIRED in intent."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.OUTLINE,
                        new IntentDefinition(
                                ActionIntent.OUTLINE,
                                "Produce structure or skeleton of content; headings and order without full body.",
                                "User wants a plan or skeleton (headings, sections) before or instead of full content.",
                                "When the goal is full content (CREATE/GENERATE) or a summary of existing content (SUMMARIZE).",
                                "OUTLINE = structure only; PLAN = plan of work; SUMMARIZE = condense existing.",
                                "Structured list of sections or steps; no full narrative.",
                                List.of(PromptCategory.WRITING, PromptCategory.CONTENT_CREATION, PromptCategory.BUSINESS),
                                List.of("CONTENT_PLANNING", "ARTICLE_WRITING"),
                                List.of("CONTENT_WRITER", "PROJECT_MANAGER"),
                                "WRITING: preferred for structuring before writing."
                        )
                )
        );
    }
}

