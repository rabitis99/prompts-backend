package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.application.semantic.resolution.SemanticResolutionService;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Formal Intent Dictionary: canonical meaning, usage guidance, and resolution defaults for every {@link ActionIntent}.
 * Reduces semantic drift by making intent semantics explicit and reviewable.
 *
 * <p>Resolution defaults (objective, output needs, response shape) are provided here so that production
 * resolution does not rely on enum getters for semantic branching. Compatibility and intent-only defaults
 * belong in the dictionary/profile layer, not in the enum identity.</p>
 */
public final class IntentDictionary {

    private static final Map<ActionIntent, IntentDefinition> DEFINITIONS;
    /** Resolution defaults per intent (objective, output needs, response shape). Canonical source for semantic resolution. */
    private static final Map<ActionIntent, IntentResolutionDefaults> RESOLUTION_DEFAULTS;

    static {
        Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
        Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
        defineCreation(defs);
        defineModification(defs);
        defineAnalysis(defs);
        defineExplanation(defs);
        definePlanning(defs);
        defineDecision(defs);
        defineResearch(defs);
        defineExtraction(defs);
        defineResolutionDefaults(res);
        EnumSet<ActionIntent> allIntents = EnumSet.allOf(ActionIntent.class);
        if (!defs.keySet().containsAll(allIntents) || !res.keySet().containsAll(allIntents)) {
            EnumSet<ActionIntent> missingDefinitions = EnumSet.copyOf(allIntents);
            missingDefinitions.removeAll(defs.keySet());
            EnumSet<ActionIntent> missingDefaults = EnumSet.copyOf(allIntents);
            missingDefaults.removeAll(res.keySet());
            throw new IllegalStateException(
                    "IntentDictionary is incomplete. missingDefinitions=" + missingDefinitions
                            + ", missingDefaults=" + missingDefaults
            );
        }
        DEFINITIONS = Collections.unmodifiableMap(new HashMap<>(defs));
        RESOLUTION_DEFAULTS = Collections.unmodifiableMap(new HashMap<>(res));
    }

    /**
     * Resolution defaults owned by the dictionary; no dependency on {@link ActionIntent} enum fields.
     * Single source of truth for intent → (objective, output needs, response shape).
     */
    private static void defineResolutionDefaults(Map<ActionIntent, IntentResolutionDefaults> res) {
        putResolutionDefault(res, ActionIntent.CREATE, PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.NARRATIVE);
        putResolutionDefault(res, ActionIntent.GENERATE, PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.NARRATIVE);
        putResolutionDefault(res, ActionIntent.BRAINSTORM, PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.REWRITE, PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.EDIT, PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.REFINE, PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.IMPROVE, PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.ANALYZE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.EVALUATE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.COMPARE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.CRITIQUE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.DIAGNOSE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STEP_BY_STEP);
        putResolutionDefault(res, ActionIntent.EXPLAIN, PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STEP_BY_STEP);
        putResolutionDefault(res, ActionIntent.TEACH, PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STEP_BY_STEP);
        putResolutionDefault(res, ActionIntent.SIMPLIFY, PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.SUMMARIZE, PromptObjective.FACTUAL, OutputNeeds.BULLET_LIST_REQUIRED, ResponseShape.CONCISE);
        putResolutionDefault(res, ActionIntent.OUTLINE, PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.PLAN, PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STEP_BY_STEP);
        putResolutionDefault(res, ActionIntent.STRATEGIZE, PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.PROPOSE, PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.ORGANIZE, PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.RECOMMEND, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.OPTIMIZE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.DECIDE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.INVESTIGATE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.SYNTHESIZE, PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.EXPLORE, PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.EXTRACT, PromptObjective.EXTRACTION, OutputNeeds.JSON_REQUIRED, ResponseShape.STRUCTURED);
        putResolutionDefault(res, ActionIntent.CLASSIFY, PromptObjective.EXTRACTION, OutputNeeds.JSON_REQUIRED, ResponseShape.STRUCTURED);
    }

    private static void putResolutionDefault(Map<ActionIntent, IntentResolutionDefaults> res, ActionIntent intent, PromptObjective objective, OutputNeeds outputNeeds, ResponseShape responseShape) {
        res.put(intent, new IntentResolutionDefaults(objective, outputNeeds, responseShape));
    }

    private IntentDictionary() {}

    /**
     * Resolution defaults for the given intent. Use this in semantic resolution; defaults are owned by the dictionary,
     * not by the {@link ActionIntent} enum.
     */
    public static IntentResolutionDefaults getResolutionDefaults(ActionIntent intent) {
        IntentResolutionDefaults d = RESOLUTION_DEFAULTS.get(intent);
        if (d == null) {
            throw new IllegalArgumentException("No resolution defaults for: " + intent);
        }
        return d;
    }

    public static Optional<IntentDefinition> get(ActionIntent intent) {
        return Optional.ofNullable(DEFINITIONS.get(intent));
    }

    public static IntentDefinition getOrThrow(ActionIntent intent) {
        IntentDefinition d = DEFINITIONS.get(intent);
        if (d == null) {
            throw new IllegalArgumentException("No IntentDefinition for: " + intent);
        }
        return d;
    }

    /**
     * Intent-only resolution defaults: objective, output needs, response shape.
     * Used by {@link SemanticResolutionService}.
     */
    public record IntentResolutionDefaults(
            PromptObjective defaultObjective,
            OutputNeeds preferredOutputNeeds,
            ResponseShape defaultResponseShape
    ) {}

    // ─── CREATION ─────────────────────────────────────────────────────────

    private static void defineCreation(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.CREATE,
                "Author new artifact from scratch; emphasis on original composition and ownership.",
                "User wants to author something new (document, design, piece of content) where originality and structure matter.",
                "When the goal is to produce output quickly from a spec (use GENERATE) or to list ideas without a single artifact (use BRAINSTORM).",
                "CREATE = original authored composition; GENERATE = produce requested output (may be template-driven); BRAINSTORM = ideation, multiple options.",
                "Narrative or structured new artifact; single coherent output.",
                List.of(PromptCategory.DESIGN, PromptCategory.WRITING, PromptCategory.CONTENT, PromptCategory.CREATIVE),
                List.of("UI_DESIGN", "UX_DESIGN", "ARTICLE_WRITING", "CONTENT_CREATION"),
                List.of("UI_UX_DESIGNER", "CONTENT_WRITER", "CREATIVE_DIRECTOR"),
                "In CONTENT: prefer CREATE for long-form authored pieces; GENERATE for quick posts or templated output."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.GENERATE,
                "Produce requested output from a description or spec; emphasis on fulfilling the request efficiently.",
                "User wants the system to produce something (code, copy, design, list) that matches a given description or template.",
                "When the user explicitly wants original authorship or heavy creative ownership (use CREATE).",
                "GENERATE = produce to spec; CREATE = author from scratch with stronger creative control.",
                "Narrative or structured output matching the request; may be more template- or spec-driven than CREATE.",
                List.of(PromptCategory.DEVELOPMENT, PromptCategory.CONTENT, PromptCategory.MARKETING, PromptCategory.CREATIVE),
                List.of("CODE_GENERATION", "CONTENT_CREATION", "CONTENT_MARKETING"),
                List.of("FULL_STACK_DEVELOPER", "CONTENT_CREATOR", "DIGITAL_MARKETER"),
                "Default fallback for many categories when intent is unspecified."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.BRAINSTORM,
                "Generate multiple ideas, options, or directions without committing to a single artifact.",
                "User wants ideation, alternatives, or exploratory options rather than one final deliverable.",
                "When the user wants a single concrete output (use CREATE or GENERATE).",
                "BRAINSTORM = many ideas; CREATE/GENERATE = one deliverable.",
                "Structured list or short descriptions of options; no single narrative.",
                List.of(PromptCategory.CREATIVE, PromptCategory.BUSINESS, PromptCategory.DESIGN),
                List.of("IDEA_GENERATION", "CREATIVE_WRITING"),
                List.of("CREATIVE_DIRECTOR", "MARKETING_STRATEGIST"),
                "Often pairs with CREATIVE or BUSINESS; less common in DEVELOPMENT for code."
        ));
    }

    // ─── MODIFICATION ─────────────────────────────────────────────────────

    private static void defineModification(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.REWRITE,
                "Substantially restructure or re-express existing content; different form or voice.",
                "User wants the same message or content in a new structure, tone, or format (e.g. formal→casual, long→short).",
                "When only local corrections or light edits are needed (use EDIT) or polish without restructuring (use REFINE).",
                "REWRITE = substantial restructuring/re-expression; EDIT = correctness and local fixes; REFINE = polish; IMPROVE = broader clarity/effectiveness.",
                "Structured or narrative output in the new form; length may change.",
                List.of(PromptCategory.WRITING, PromptCategory.CONTENT, PromptCategory.CREATIVE),
                List.of("EDITING", "CONTENT_REVISION", "CREATIVE_WRITING"),
                List.of("EDITOR", "COPYWRITER", "CONTENT_STRATEGIST"),
                "WRITING category: REWRITE and EDIT must be clearly distinguished in profiles."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.EDIT,
                "Fix correctness, grammar, or local issues; preserve structure and voice.",
                "User wants corrections, fixes, or small changes without changing the overall structure or tone.",
                "When substantial restructuring or re-expression is needed (use REWRITE) or quality polish (use REFINE).",
                "EDIT = correctness/local fixes; REWRITE = re-expression; REFINE = polish; IMPROVE = broader enhancement.",
                "Same structure as input with corrections; minimal structural change.",
                List.of(PromptCategory.WRITING, PromptCategory.CONTENT),
                List.of("EDITING", "PROOFREADING", "DOC_UPDATE"),
                List.of("EDITOR", "CONTENT_WRITER"),
                "Preferred for copy-editing and proofreading flows."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.REFINE,
                "Polish and elevate quality of existing content; improve clarity and style without major restructuring.",
                "User wants to improve flow, word choice, and quality while keeping the same message and structure.",
                "When structural changes are needed (use REWRITE) or only typo/grammar fixes (use EDIT).",
                "REFINE = polish and quality; EDIT = correctness; REWRITE = restructure; IMPROVE = broader effectiveness.",
                "Same structure, higher quality; more polished language.",
                List.of(PromptCategory.WRITING, PromptCategory.DEVELOPMENT),
                List.of("EDITING", "CODE_MODIFICATION"),
                List.of("EDITOR", "COPYWRITER"),
                "DEVELOPMENT: REFINE for code quality and readability improvements."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.IMPROVE,
                "Broader enhancement of clarity, effectiveness, or impact; may include structure and content.",
                "User wants overall improvement (readability, impact, clarity) without specifying exact type of change.",
                "When the need is narrowly correctness (EDIT), polish only (REFINE), or full re-expression (REWRITE).",
                "IMPROVE = broad enhancement; REFINE = polish; EDIT = fixes; REWRITE = re-express.",
                "Improved version; may include structural tweaks and content adjustments.",
                List.of(PromptCategory.WRITING, PromptCategory.DEVELOPMENT, PromptCategory.CONTENT),
                List.of("EDITING", "CODE_MODIFICATION", "CONTENT_OPTIMIZATION"),
                List.of("EDITOR", "CONTENT_WRITER", "BACKEND_DEVELOPER"),
                "General-purpose improvement intent across writing and development."
        ));
    }

    // ─── ANALYSIS ──────────────────────────────────────────────────────────

    private static void defineAnalysis(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.ANALYZE,
                "Break down a subject into components, patterns, or factors; systematic examination.",
                "User wants understanding of parts, causes, or patterns in data, text, or a system.",
                "When the goal is judgment of merit (use EVALUATE), comparison (use COMPARE), or fault-finding (use DIAGNOSE).",
                "ANALYZE = break down and examine; EVALUATE = judge merit; CRITIQUE = judge with standards; DIAGNOSE = find cause of problem.",
                "Structured analysis; components, factors, or patterns.",
                List.of(PromptCategory.ANALYSIS, PromptCategory.RESEARCH, PromptCategory.BUSINESS),
                List.of("DATA_ANALYSIS", "LITERATURE_REVIEW", "ROOT_CAUSE_ANALYSIS"),
                List.of("GENERAL_CONSULTANT", "RESEARCHER", "BUSINESS_ANALYST_BUSINESS"),
                "Core intent for ANALYSIS and RESEARCH categories."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.EVALUATE,
                "Judge merit, quality, or suitability against criteria or goals.",
                "User wants an assessment of how good, suitable, or effective something is.",
                "When the goal is only to break down or describe (use ANALYZE) or to compare two things (use COMPARE).",
                "EVALUATE = judge merit; ANALYZE = examine components; CRITIQUE = evaluate against explicit standards; COMPARE = compare alternatives.",
                "Structured evaluation with criteria and judgment.",
                List.of(PromptCategory.DESIGN, PromptCategory.DEVELOPMENT, PromptCategory.RESEARCH),
                List.of("CODE_REVIEW", "UX_DESIGN", "COMPARATIVE_ANALYSIS"),
                List.of("UI_UX_DESIGNER", "PRODUCT_DESIGNER", "RESEARCHER"),
                "DESIGN: preferred for design review and critique flows."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.COMPARE,
                "Place two or more items side by side; highlight similarities and differences.",
                "User wants a direct comparison of options, versions, or alternatives.",
                "When the goal is single-item analysis (ANALYZE) or a merit judgment (EVALUATE).",
                "COMPARE = side-by-side comparison; ANALYZE = single subject breakdown; EVALUATE = merit judgment.",
                "Structured comparison; pros/cons or dimension-based.",
                List.of(PromptCategory.ANALYSIS, PromptCategory.BUSINESS, PromptCategory.RESEARCH),
                List.of("COMPARATIVE_ANALYSIS", "DATA_ANALYSIS"),
                List.of("BUSINESS_ANALYST_BUSINESS", "RESEARCHER"),
                "Fits ANALYSIS and RESEARCH; often used with DECIDE or RECOMMEND."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.CRITIQUE,
                "Judge against explicit standards or criteria; constructive criticism.",
                "User wants feedback that evaluates against defined standards (e.g. design principles, style guide).",
                "When the goal is neutral analysis (ANALYZE) or open-ended evaluation without standards (EVALUATE).",
                "CRITIQUE = judge against standards; EVALUATE = general merit; ANALYZE = break down without judgment.",
                "Structured critique with criteria and suggestions.",
                List.of(PromptCategory.DESIGN, PromptCategory.WRITING, PromptCategory.RESEARCH),
                List.of("UI_DESIGN", "UX_DESIGN", "EDITING"),
                List.of("UI_UX_DESIGNER", "PRODUCT_DESIGNER", "EDITOR"),
                "DESIGN: preferred for design review; DIAGNOSE only in narrow debug/critique contexts."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.DIAGNOSE,
                "Identify cause of a problem or failure; root-cause or fault-finding.",
                "User has a problem (bug, failure, issue) and wants to understand why it occurs.",
                "When the goal is general analysis (ANALYZE), merit evaluation (EVALUATE), or design critique (CRITIQUE).",
                "DIAGNOSE = find cause of problem; ANALYZE = general breakdown; CRITIQUE = judge against standards.",
                "Step-by-step or structured diagnosis; cause-and-effect.",
                List.of(PromptCategory.DEVELOPMENT, PromptCategory.ANALYSIS),
                List.of("DEBUGGING", "CODE_ANALYSIS", "ROOT_CAUSE_ANALYSIS"),
                List.of("BACKEND_DEVELOPER", "FRONTEND_DEVELOPER"),
                "DESIGN: discouraged unless critique/debug context is explicit (e.g. usability bug)."
        ));
    }

    // ─── EXPLANATION ───────────────────────────────────────────────────────

    private static void defineExplanation(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.EXPLAIN,
                "Make a topic or process understandable; clarify how or why.",
                "User wants to understand something (concept, process, system) in a clear way.",
                "When the goal is to teach a curriculum (use TEACH), shorten (use SIMPLIFY), or condense (use SUMMARIZE).",
                "EXPLAIN = clarify how/why; TEACH = instructional sequence; SIMPLIFY = reduce complexity; SUMMARIZE = condense.",
                "Step-by-step or structured explanation; pedagogical tone optional.",
                List.of(PromptCategory.EDUCATION, PromptCategory.STUDY, PromptCategory.DEVELOPMENT, PromptCategory.RESEARCH),
                List.of("DOCUMENTATION", "TEACHING_METHOD", "RESEARCH_DESIGN"),
                List.of("EDUCATOR", "RESEARCH_METHODOLOGIST", "FULL_STACK_DEVELOPER"),
                "Core intent for EDUCATION and STUDY."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.TEACH,
                "Instruct in a learning sequence; curriculum or lesson oriented.",
                "User wants to learn a topic in a structured, pedagogical way.",
                "When the goal is a one-off explanation (EXPLAIN) or simplification of existing content (SIMPLIFY).",
                "TEACH = instructional sequence; EXPLAIN = single explanation; SIMPLIFY = reduce complexity of given content.",
                "Step-by-step lesson; may include examples and exercises.",
                List.of(PromptCategory.EDUCATION, PromptCategory.STUDY),
                List.of("TEACHING_METHOD", "LESSON_PLANNING", "EXAM_PREPARATION"),
                List.of("EDUCATOR", "CURRICULUM_DESIGNER", "TUTOR"),
                "Prefer EXPLAIN for one-off clarification; TEACH for learning paths."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.SIMPLIFY,
                "Reduce complexity of given content or concept; make more accessible.",
                "User has complex material and wants it made easier to understand.",
                "When the goal is to condense length (use SUMMARIZE) or to explain from scratch (use EXPLAIN).",
                "SIMPLIFY = reduce complexity; SUMMARIZE = shorten; EXPLAIN = clarify how/why.",
                "Simpler version of the same content; structure may be reorganized.",
                List.of(PromptCategory.EDUCATION, PromptCategory.STUDY, PromptCategory.WRITING),
                List.of("MATERIAL_CREATION", "KNOWLEDGE_ORGANIZATION"),
                List.of("EDUCATOR", "TECHNICAL_WRITER"),
                "Often used for technical or academic content simplification."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.SUMMARIZE,
                "Condense content to key points; preserve meaning, reduce length.",
                "User wants a shorter version that captures the main points.",
                "When the goal is to simplify complexity (SIMPLIFY) or to explain (EXPLAIN).",
                "SUMMARIZE = condense; SIMPLIFY = reduce complexity; OUTLINE = structure only.",
                "Bullet list or concise narrative; minimal new content.",
                List.of(PromptCategory.RESEARCH, PromptCategory.WRITING, PromptCategory.STUDY, PromptCategory.BUSINESS),
                List.of("LITERATURE_REVIEW", "NOTE_TAKING", "ARTICLE_WRITING"),
                List.of("RESEARCHER", "EDITOR", "STUDY_COACH"),
                "Output often bullet-list; OutputNeeds.BULLET_LIST_REQUIRED in intent."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.OUTLINE,
                "Produce structure or skeleton of content; headings and order without full body.",
                "User wants a plan or skeleton (headings, sections) before or instead of full content.",
                "When the goal is full content (CREATE/GENERATE) or a summary of existing content (SUMMARIZE).",
                "OUTLINE = structure only; PLAN = plan of work; SUMMARIZE = condense existing.",
                "Structured list of sections or steps; no full narrative.",
                List.of(PromptCategory.WRITING, PromptCategory.CONTENT, PromptCategory.BUSINESS),
                List.of("CONTENT_PLANNING", "ARTICLE_WRITING"),
                List.of("CONTENT_WRITER", "PROJECT_MANAGER"),
                "WRITING: preferred for structuring before writing."
        ));
    }

    // ─── PLANNING ─────────────────────────────────────────────────────────

    private static void definePlanning(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.PLAN,
                "Produce a sequence of steps or phases to achieve a goal.",
                "User wants a plan: what to do, in what order, to reach an outcome.",
                "When the goal is strategy (STRATEGIZE), a proposal (PROPOSE), or organization of existing items (ORGANIZE).",
                "PLAN = steps/phases; STRATEGIZE = high-level approach; PROPOSE = suggest option; ORGANIZE = arrange existing.",
                "Step-by-step or phased plan.",
                List.of(PromptCategory.PRODUCTIVITY, PromptCategory.BUSINESS, PromptCategory.EDUCATION, PromptCategory.DEVELOPMENT),
                List.of("SCHEDULE_PLANNING", "BUSINESS_PLAN_DEVELOPMENT", "LESSON_PLANNING", "ARCHITECTURE_DESIGN"),
                List.of("PRODUCTIVITY_EXPERT", "PROJECT_MANAGER", "CURRICULUM_DESIGNER"),
                "Fallback for PRODUCTIVITY when intent missing."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.STRATEGIZE,
                "Define high-level approach, direction, or strategy; not a detailed step list.",
                "User wants strategic guidance: direction, principles, or approach rather than a task list.",
                "When the goal is a concrete plan (PLAN) or a specific proposal (PROPOSE).",
                "STRATEGIZE = high-level approach; PLAN = step sequence; PROPOSE = concrete suggestion.",
                "Structured strategy; principles and priorities.",
                List.of(PromptCategory.BUSINESS, PromptCategory.MARKETING),
                List.of("BUSINESS_STRATEGY", "MARKETING_STRATEGY"),
                List.of("BUSINESS_CONSULTANT", "MARKETING_STRATEGIST"),
                "Often used with BUSINESS and MARKETING."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.PROPOSE,
                "Suggest a concrete option, solution, or recommendation for a decision.",
                "User wants a specific proposal: what to do or choose, with rationale.",
                "When the goal is open strategy (STRATEGIZE) or a neutral plan (PLAN).",
                "PROPOSE = suggest option; RECOMMEND = recommend with reasoning; PLAN = steps.",
                "Structured proposal with rationale.",
                List.of(PromptCategory.BUSINESS, PromptCategory.DEVELOPMENT),
                List.of("PROPOSAL_WRITING", "SYSTEM_DESIGN", "BUSINESS_PLAN_DEVELOPMENT"),
                List.of("BUSINESS_CONSULTANT", "CLOUD_ARCHITECT"),
                "BUSINESS: preferred for proposals and option presentation."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.ORGANIZE,
                "Arrange existing items into structure, order, or categories.",
                "User has existing items (ideas, tasks, content) and wants them ordered or grouped.",
                "When the goal is to create new content (CREATE/GENERATE) or to plan from scratch (PLAN).",
                "ORGANIZE = arrange existing; PLAN = define steps; OUTLINE = structure for content.",
                "Structured list or taxonomy.",
                List.of(PromptCategory.PRODUCTIVITY, PromptCategory.STUDY, PromptCategory.CONTENT),
                List.of("TASK_AUTOMATION", "KNOWLEDGE_ORGANIZATION", "CONTENT_PLANNING"),
                List.of("PRODUCTIVITY_EXPERT", "STUDY_COACH"),
                "Less common than PLAN; fits productivity and study."
        ));
    }

    // ─── DECISION ────────────────────────────────────────────────────────

    private static void defineDecision(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.RECOMMEND,
                "Recommend one or more options with reasoning; support decision-making.",
                "User wants a recommendation: what to choose or do, with justification.",
                "When the goal is only to compare (COMPARE) or to propose without ranking (PROPOSE).",
                "RECOMMEND = recommend with reasoning; PROPOSE = suggest option; DECIDE = make the decision explicit.",
                "Structured recommendation with rationale.",
                List.of(PromptCategory.BUSINESS, PromptCategory.DESIGN, PromptCategory.DEVELOPMENT),
                List.of("RISK_ASSESSMENT", "PRODUCT_DESIGN", "CODE_REVIEW"),
                List.of("BUSINESS_CONSULTANT", "PRODUCT_DESIGNER"),
                "DESIGN: allowed but weaker than CREATE/EVALUATE/CRITIQUE."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.OPTIMIZE,
                "Improve an existing solution against stated criteria (speed, cost, clarity).",
                "User has something that works and wants it improved for specific dimensions.",
                "When the goal is to fix broken (DIAGNOSE) or to evaluate only (EVALUATE).",
                "OPTIMIZE = improve against criteria; IMPROVE = general enhancement; DIAGNOSE = find cause of failure.",
                "Structured optimization suggestions; before/after or metrics.",
                List.of(PromptCategory.DEVELOPMENT, PromptCategory.BUSINESS, PromptCategory.PRODUCTIVITY),
                List.of("CODE_MODIFICATION", "BUSINESS_STRATEGY", "TASK_AUTOMATION"),
                List.of("FULL_STACK_DEVELOPER", "BUSINESS_ANALYST_BUSINESS"),
                "Fits technical and business optimization."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.DECIDE,
                "Make or support a binary or multi-option decision; choose among alternatives.",
                "User wants to decide between options or get support for a decision.",
                "When the goal is only to recommend (RECOMMEND) or to analyze (ANALYZE).",
                "DECIDE = choose; RECOMMEND = recommend with reasoning; PROPOSE = suggest option.",
                "Structured decision with criteria and choice.",
                List.of(PromptCategory.BUSINESS),
                List.of("RISK_ASSESSMENT", "CONTRACT_REVIEW"),
                List.of("BUSINESS_CONSULTANT", "BUSINESS_ANALYST_BUSINESS"),
                "BUSINESS: preferred for decision-support flows."
        ));
    }

    // ─── RESEARCH ─────────────────────────────────────────────────────────

    private static void defineResearch(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.INVESTIGATE,
                "Systematically look into a question or topic; gather and examine evidence.",
                "User wants to investigate a question: what is known, what are sources, what are findings.",
                "When the goal is to synthesize into one view (SYNTHESIZE) or to explore freely (EXPLORE).",
                "INVESTIGATE = systematic inquiry; SYNTHESIZE = combine into one view; EXPLORE = open exploration.",
                "Structured findings; may include sources and caveats.",
                List.of(PromptCategory.RESEARCH, PromptCategory.ANALYSIS),
                List.of("LITERATURE_REVIEW", "DATA_INTERPRETATION", "RESEARCH_DESIGN"),
                List.of("RESEARCHER", "RESEARCH_METHODOLOGIST"),
                "RESEARCH: preferred for literature and evidence-based inquiry."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.SYNTHESIZE,
                "Combine multiple sources or views into a single coherent position or summary.",
                "User has multiple inputs (papers, opinions, data) and wants them synthesized.",
                "When the goal is to investigate (INVESTIGATE) or to explore (EXPLORE) without synthesis.",
                "SYNTHESIZE = combine into one; INVESTIGATE = look into; EXPLORE = open exploration.",
                "Structured synthesis; integrated view with sources.",
                List.of(PromptCategory.RESEARCH, PromptCategory.ANALYSIS),
                List.of("LITERATURE_REVIEW", "STATISTICAL_MODELING", "DATA_INTERPRETATION"),
                List.of("RESEARCHER", "ACADEMIC_WRITER"),
                "RESEARCH: preferred for literature review and evidence synthesis; PERSUASIVE tone discouraged unless explicit."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.EXPLORE,
                "Open-ended exploration of ideas, options, or possibilities; less structured than investigate.",
                "User wants to explore a space of ideas or options without a fixed question.",
                "When the goal is systematic investigation (INVESTIGATE) or synthesis (SYNTHESIZE).",
                "EXPLORE = open exploration; INVESTIGATE = systematic; SYNTHESIZE = combine into one.",
                "Structured exploration; multiple angles or options.",
                List.of(PromptCategory.RESEARCH, PromptCategory.CREATIVE),
                List.of("IDEA_GENERATION", "LITERATURE_REVIEW"),
                List.of("RESEARCHER", "CREATIVE_DIRECTOR"),
                "Less formal than INVESTIGATE; fits creative and research."
        ));
    }

    // ─── EXTRACTION ────────────────────────────────────────────────────────

    private static void defineExtraction(Map<ActionIntent, IntentDefinition> defs) {
        put(defs, new IntentDefinition(
                ActionIntent.EXTRACT,
                "Pull structured data or entities from unstructured input; output in specified schema (e.g. JSON).",
                "User wants to extract specific fields, entities, or facts from text or content.",
                "When the goal is to classify into categories only (use CLASSIFY) or to summarize (use SUMMARIZE).",
                "EXTRACT = structured extraction to schema; CLASSIFY = assign categories/labels.",
                "Structured output (e.g. JSON); schema-driven.",
                List.of(PromptCategory.EXTRACTION, PromptCategory.ANALYSIS, PromptCategory.RESEARCH),
                List.of("DATA_ANALYSIS"),
                List.of("GENERAL_CONSULTANT"),
                "EXTRACTION request_mode forces intent=EXTRACT; category=EXTRACTION."
        ));
        put(defs, new IntentDefinition(
                ActionIntent.CLASSIFY,
                "Assign category, label, or tag to input; classification or categorization.",
                "User wants to classify content into predefined categories or labels.",
                "When the goal is to extract multiple fields (use EXTRACT) or to summarize (use SUMMARIZE).",
                "CLASSIFY = assign category; EXTRACT = extract fields to schema.",
                "Structured labels or categories.",
                List.of(PromptCategory.ANALYSIS),
                List.of("DATA_ANALYSIS"),
                List.of("GENERAL_CONSULTANT"),
                "Fits ANALYSIS and extraction-style flows."
        ));
    }

    private static void put(Map<ActionIntent, IntentDefinition> map, IntentDefinition def) {
        map.put(def.intent(), def);
    }
}
