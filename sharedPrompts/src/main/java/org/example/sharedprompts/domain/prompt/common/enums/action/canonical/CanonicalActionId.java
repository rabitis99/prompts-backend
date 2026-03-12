package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

/**
 * Canonical action identifiers: a bounded set of reusable system capabilities.
 * Each represents a distinct capability for prompt assembly, evaluation, and resolution.
 * Existing {@link org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface}
 * values map to exactly one canonical action; channel/format/purpose/scope variants
 * are handled by metadata, profile, or template—not by new enum constants.
 *
 * <p>Governance: prefer "canonical + metadata" over adding new ActionType or new CanonicalActionId.
 * See docs/canonical-action-registry-design.md and package governance notes.</p>
 */
public enum CanonicalActionId {
    // Writing & content — long-form and short copy
    LONG_FORM_WRITING,
    SHORT_COPY,
    SCRIPT_OR_MEDIA_WRITING,
    MESSAGE_COMPOSITION,
    TEXT_REVISION,
    TECHNICAL_WRITING,
    TRANSLATION,
    CREATIVE_WRITING,
    LETTER_WRITING,
    CAREER_DOCUMENT_WRITING,
    REVIEW_OR_FEEDBACK_WRITING,
    // Recommendation & consultation
    RECOMMEND,
    EXPLANATION,
    ADVICE_OR_GUIDANCE,
    // Planning & strategy
    PLANNING,
    STRATEGY,
    PROJECT_OR_BUSINESS_PLAN,
    // Analysis & evaluation
    DATA_ANALYSIS,
    CODE_ANALYSIS,
    RISK_ASSESSMENT,
    EVALUATION_OR_AUDIT,
    MARKET_OR_CUSTOMER_ANALYSIS,
    // Code & development
    CODE_GENERATION,
    CODE_MODIFICATION,
    DEBUGGING,
    SECURITY_IMPLEMENTATION,
    THREAT_OR_INCIDENT,
    INFRASTRUCTURE_AS_CODE,
    CLOUD_OPERATIONS,
    PERFORMANCE_OPTIMIZATION,
    // AI/ML, research, education, design, business, support, lifestyle
    ML_MODEL_LIFECYCLE,
    RESEARCH_METHODOLOGY,
    EDUCATION_DESIGN,
    DESIGN,
    CREATIVE_CONCEPT,
    PRESENTATION_OR_REPORT,
    FINANCIAL_ANALYSIS,
    CUSTOMER_SUPPORT_OPERATIONS,
    MARKETING_STRATEGY_AND_EXECUTION,
    PRODUCTIVITY_AND_PERSONAL,
    HEALTH_FITNESS,
    SOCIAL_OR_COMMUNITY,
    PROBLEM_SOLVING,
    INTERVIEW_PREPARATION,
    SHOPPING
}
