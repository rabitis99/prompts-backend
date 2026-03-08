package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * User intent taxonomy — branch selector under {@link PromptCategory}.
 *
 * <p>Represents <b>user intent</b> only (identity). Domain operations (e.g. code generation, UI design)
 * belong to {@link org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface}
 * and are resolved per category+intent via {@link org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile}.</p>
 *
 * <p>Resolution defaults (objective, output needs, response shape) live in
 * {@link org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary#getResolutionDefaults(ActionIntent)};
 * the enum does not carry policy.</p>
 */
public enum ActionIntent {

    // ─── CREATION ─────────────────────────────────────────────────────────
    CREATE,
    GENERATE,
    BRAINSTORM,

    // ─── MODIFICATION ───────────────────────────────────────────────────────
    REWRITE,
    EDIT,
    REFINE,
    IMPROVE,

    // ─── ANALYSIS ──────────────────────────────────────────────────────────
    ANALYZE,
    EVALUATE,
    COMPARE,
    CRITIQUE,
    DIAGNOSE,

    // ─── EXPLANATION ───────────────────────────────────────────────────────
    EXPLAIN,
    TEACH,
    SIMPLIFY,
    SUMMARIZE,
    OUTLINE,

    // ─── PLANNING ──────────────────────────────────────────────────────────
    PLAN,
    STRATEGIZE,
    PROPOSE,
    ORGANIZE,

    // ─── DECISION ─────────────────────────────────────────────────────────
    RECOMMEND,
    OPTIMIZE,
    DECIDE,

    // ─── RESEARCH ──────────────────────────────────────────────────────────
    INVESTIGATE,
    SYNTHESIZE,
    EXPLORE,

    // ─── EXTRACTION / CLASSIFICATION ────────────────────────────────────────
    EXTRACT,
    CLASSIFY
}
