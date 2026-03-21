# Category / Intent / Action Structure Extraction

Extraction from codebase. Source: real code only.

---

# Category List

## Canonical (from PromptCategory.java)
- PRODUCTIVITY
- BUSINESS
- MARKETING
- CUSTOMER_SUPPORT
- DEVELOPMENT
- DATA_ANALYSIS
- RESEARCH
- EDUCATION
- WRITING
- DESIGN
- LEGAL
- EXTRACTION
- ETC

## Legacy (deserialization only; canonical() maps to above)
- SALES (→ MARKETING)
- OPERATIONS (→ BUSINESS)
- CONTENT_CREATION (→ WRITING)
- ANALYSIS (→ DATA_ANALYSIS)

## Non-UI
- CREATIVE (no canonical mapping; has seed)

---

# Intent List

From ActionIntent.java:
- CREATE
- GENERATE
- BRAINSTORM
- REWRITE
- EDIT
- REFINE
- IMPROVE
- ANALYZE
- EVALUATE
- COMPARE
- CRITIQUE
- DIAGNOSE
- EXPLAIN
- TEACH
- SIMPLIFY
- SUMMARIZE
- OUTLINE
- PLAN
- STRATEGIZE
- PROPOSE
- ORGANIZE
- RECOMMEND
- OPTIMIZE
- DECIDE
- INVESTIGATE
- SYNTHESIZE
- EXPLORE
- EXTRACT
- CLASSIFY

---

# Action List

Actions are ActionType enum constants. Source: action-type-domain.properties + enum definitions. Format: `ACTION.<CATEGORY>.<NAME>` (stable key).

Full list extracted from action-type-domain.properties (319 entries). Grouped by ActionType enum class:

## ProductivityActionType
- ACTION.PRODUCTIVITY.WORKFLOW_OPTIMIZATION (ActionGroup: PERSONAL_PRODUCTIVITY)
- ACTION.PRODUCTIVITY.TIME_MANAGEMENT (ActionGroup: PERSONAL_PRODUCTIVITY)
- ACTION.PRODUCTIVITY.SCHEDULE_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.DAILY_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.WEEKLY_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.MONTHLY_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.TASK_AUTOMATION (ActionGroup: PERSONAL_PRODUCTIVITY)
- ACTION.PRODUCTIVITY.SHOPPING_LIST (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.MEAL_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.BUDGET_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.HOUSEHOLD_MANAGEMENT (ActionGroup: PERSONAL_PRODUCTIVITY)
- ACTION.PRODUCTIVITY.EFFICIENCY_ANALYSIS (ActionGroup: DATA_ANALYSIS)
- ACTION.PRODUCTIVITY.PRODUCTIVITY_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.PRODUCTIVITY.PROCESS_IMPROVEMENT (ActionGroup: PERSONAL_PRODUCTIVITY)
- ACTION.PRODUCTIVITY.RESOURCE_OPTIMIZATION (ActionGroup: PERSONAL_PRODUCTIVITY)

## DevelopmentActionType
- ACTION.DEVELOPMENT.ARCHITECTURE_DESIGN (ActionGroup: PROJECT_OR_BUSINESS_PLANNING)
- ACTION.DEVELOPMENT.CODEBASE_ANALYSIS (ActionGroup: CODE_ANALYSIS)
- ACTION.DEVELOPMENT.DEPLOYMENT_STRATEGY (ActionGroup: DELIVERY_AUTOMATION)
- ACTION.DEVELOPMENT.DOCUMENTATION (ActionGroup: TECHNICAL_WRITING)
- ACTION.DEVELOPMENT.PERFORMANCE_OPTIMIZATION (ActionGroup: PERFORMANCE_OPTIMIZATION)
- ACTION.DEVELOPMENT.SCALABILITY_PLANNING (ActionGroup: GENERAL_PLANNING)
- ACTION.DEVELOPMENT.SECURITY_IMPLEMENTATION (ActionGroup: SECURITY_IMPLEMENTATION)
- ACTION.DEVELOPMENT.SYSTEM_DESIGN (ActionGroup: PROJECT_OR_BUSINESS_PLANNING)
- ACTION.DEVELOPMENT.TECH_STACK_SELECTION (ActionGroup: GENERAL_PLANNING)

## CloudServicesActionType, DevOpsActionType, CybersecurityActionType, CodingActionType, ProgrammingActionType, AiMlActionType
(Full enum constants in DefaultActionTypeCatalog; ActionGroup per constant from each enum.)

## AnalysisActionType
- ACTION.ANALYSIS.BUSINESS_INTELLIGENCE, COMPARATIVE_ANALYSIS, DATA_ANALYSIS, INSIGHT_EXTRACTION, PATTERN_RECOGNITION, PREDICTIVE_ANALYSIS, ROOT_CAUSE_ANALYSIS, STATISTICAL_ANALYSIS, TREND_ANALYSIS

## MarketingActionType, ContentCreationActionType, CreativeActionType, EducationActionType, ResearchActionType
## BusinessActionType, CustomerSupportActionType, EmailActionType, DesignActionType, WritingActionType
## EtcActionType, HealthFitnessActionType, SocialActionType, CareerActionType, LifestyleActionType
## PersonalDevelopmentActionType, RecommendationActionType, ShoppingActionType

*Full Action list: 319 entries in action-type-domain.properties. Each maps to an ActionGroup via ActionTypeInterface.getActionGroup().*

---

# ActionGroup List

From ActionGroup.java:
- LONG_FORM_WRITING, SHORT_COPY, SCRIPT_OR_MEDIA_WRITING, MESSAGE_COMPOSITION, TEXT_REVISION, TECHNICAL_WRITING
- TRANSLATION
- CREATIVE_WRITING, LETTER_WRITING, CAREER_DOCUMENT_WRITING, REVIEW_OR_FEEDBACK_WRITING
- RECOMMENDATION, EXPLANATION, GUIDANCE
- GENERAL_PLANNING, STRATEGY, PROJECT_OR_BUSINESS_PLANNING
- DATA_ANALYSIS, CODE_ANALYSIS, RISK_ASSESSMENT, EVALUATION_OR_AUDIT
- CODE_GENERATION, CODE_MODIFICATION, DEBUGGING
- SECURITY_IMPLEMENTATION, THREAT_OR_INCIDENT_RESPONSE, DELIVERY_AUTOMATION, INFRASTRUCTURE_AS_CODE, OBSERVABILITY, PERFORMANCE_OPTIMIZATION
- RESEARCH_METHODOLOGY, EDUCATION_DESIGN, GENERAL_DESIGN, CREATIVE_CONCEPT, PRESENTATION_OR_REPORT, FINANCIAL_ANALYSIS
- FAQ_AND_KNOWLEDGE_BASE, TICKET_HANDLING, CUSTOMER_ONBOARDING, COMPLAINT_RESPONSE
- MARKETING_STRATEGY, MARKETING_EXECUTION
- WORKOUT_PLANNING, NUTRITION_GUIDANCE, MENTAL_WELLNESS, HEALTH_TRACKING
- SOCIAL_OR_COMMUNITY, PERSONAL_PRODUCTIVITY
- INTERVIEW_PREPARATION
- SHOPPING
- CLOUD_DEPLOYMENT, CLOUD_PLATFORM_ARCHITECTURE, CLOUD_SECURITY, CLOUD_NETWORKING, CLOUD_COST_OPTIMIZATION, DISASTER_RECOVERY, CAPACITY_SCALING
- MODEL_TRAINING, MODEL_EVALUATION, MODEL_DEPLOYMENT, DATA_PREPARATION_FOR_ML

---

# Category → Intent Mapping

Source: CategorySemanticProfileSeed (allowedIntents + actionsByIntent keys). Each seed defines `allowedIntents` and `actions` map keys.

## PRODUCTIVITY
Source: ProductivitySemanticProfileSeed
- PLAN
- GENERATE
- ANALYZE

## BUSINESS
Source: BusinessSemanticProfileSeed
- PLAN
- PROPOSE
- DECIDE
- ANALYZE
- GENERATE
- SUMMARIZE

## MARKETING
Source: MarketingSemanticProfileSeed
- GENERATE
- PLAN
- ANALYZE
- REWRITE

## CUSTOMER_SUPPORT
Source: CustomerSupportSemanticProfileSeed
- GENERATE
- EXPLAIN
- PLAN
- SUMMARIZE
- ANALYZE

## DEVELOPMENT
Source: DevelopmentSemanticProfileSeed
- GENERATE
- DIAGNOSE
- EXPLAIN
- EVALUATE
- PLAN
- REFINE
- IMPROVE

## DATA_ANALYSIS
Source: DataAnalysisSemanticProfileSeed
- ANALYZE
- EVALUATE
- EXTRACT
- SUMMARIZE
- CLASSIFY

## RESEARCH
Source: ResearchSemanticProfileSeed
- INVESTIGATE
- ANALYZE
- SYNTHESIZE
- EXPLAIN
- SUMMARIZE
- GENERATE
- EVALUATE
- EXTRACT

## EDUCATION
Source: EducationSemanticProfileSeed
- EXPLAIN
- PLAN
- GENERATE
- SUMMARIZE

## WRITING
Source: WritingSemanticProfileSeed
- CREATE
- GENERATE
- REWRITE
- EDIT
- REFINE
- IMPROVE
- OUTLINE
- SUMMARIZE
- EXPLAIN
- PLAN

## DESIGN
Source: DesignSemanticProfileSeed
- GENERATE
- CREATE
- EVALUATE
- CRITIQUE
- EXPLAIN
- PLAN
- REWRITE
- RECOMMEND
- DIAGNOSE

## LEGAL
Source: LegalSemanticProfileSeed
- EVALUATE
- GENERATE
- EXPLAIN
- SUMMARIZE

## CREATIVE
Source: CreativeSemanticProfileSeed
- GENERATE
- REWRITE
- EXPLAIN
- PLAN

## ETC
Source: EtcSemanticProfileSeed
- All 29 intents allowed (GENERATE, EXPLAIN, PLAN, ANALYZE, REWRITE, SUMMARIZE, EVALUATE, EXTRACT, CLASSIFY, DECIDE, DIAGNOSE, CREATE, EDIT, REFINE, IMPROVE, COMPARE, CRITIQUE, TEACH, SIMPLIFY, OUTLINE, STRATEGIZE, PROPOSE, ORGANIZE, RECOMMEND, OPTIMIZE, INVESTIGATE, SYNTHESIZE, EXPLORE, BRAINSTORM)

## EXTRACTION
- **unclear** — EXTRACTION does NOT have a CategorySemanticProfileSeed. Explicitly excluded in DefaultCategorySemanticProfileSeedSource. No profile in registry.

---

# Intent → Action Mapping

Intent → Action is **context-dependent**: defined per (Category, Intent) in CategorySemanticProfileSeed.actionsByIntent.

Standalone Intent → Action (without category) exists only as:
- **representativeActionHints** in IntentDefinition (hints like "CODE_GENERATION", "CONTENT_CREATION") — not canonical ActionType keys
- **Indirect** from aggregating all seeds

Below: per-Intent actions aggregated from all seeds (source: seed class name).

## PLAN
- ProductivityActionType: SCHEDULE_PLANNING, TASK_AUTOMATION
- BusinessActionType: BUSINESS_PLAN_DEVELOPMENT, PROJECT_MANAGEMENT, BUSINESS_STRATEGY
- MarketingActionType: MARKETING_STRATEGY, SOCIAL_MEDIA_STRATEGY
- CustomerSupportActionType: CUSTOMER_ONBOARDING, SUPPORT_TRAINING
- DevelopmentActionType: ARCHITECTURE_DESIGN, SYSTEM_DESIGN
- EducationActionType: LESSON_PLANNING, CURRICULUM_DESIGN
- DesignActionType: WIREFRAMING, PRODUCT_DESIGN, DESIGN_DOC
- WritingActionType: ARTICLE_WRITING (via OUTLINE; PLAN in allowed only in Writing)

## GENERATE
- ProductivityActionType: (none in actions map; GENERATE in allowed)
- BusinessActionType: PROPOSAL_WRITING, REPORT_WRITING, PRESENTATION_PREPARATION
- MarketingActionType: CONTENT_MARKETING, AD_CAMPAIGN
- CustomerSupportActionType: FAQ_CREATION, SUPPORT_DOCUMENTATION
- CodingActionType: CODE_GENERATION, CODE_MODIFICATION, TEST_GENERATION
- ResearchActionType: PAPER_WRITING, HYPOTHESIS_FORMULATION
- EducationActionType: ASSESSMENT_DESIGN, MATERIAL_CREATION
- DesignActionType: GRAPHIC_DESIGN, UI_DESIGN, VISUAL_IDENTITY
- LegalSemanticProfileSeed: CONTRACT_REVIEW, PROPOSAL_WRITING
- CreativeActionType: IDEA_GENERATION, CREATIVE_WRITING
- WritingActionType: ARTICLE_WRITING, CREATIVE_WRITING_GEN, COPYWRITING

## ANALYZE
- ProductivityActionType: (allowed only)
- BusinessActionType: FINANCIAL_ANALYSIS, RISK_ASSESSMENT
- MarketingActionType: (allowed only)
- CustomerSupportActionType: (allowed only)
- DataAnalysis: AnalysisActionType: DATA_ANALYSIS, COMPARATIVE_ANALYSIS, ROOT_CAUSE_ANALYSIS
- ResearchActionType: DATA_INTERPRETATION, LITERATURE_REVIEW, STATISTICAL_MODELING

## PROPOSE
- BusinessActionType: PROPOSAL_WRITING, BUSINESS_STRATEGY

## DECIDE
- BusinessActionType: RISK_ASSESSMENT, CONTRACT_REVIEW

## CREATE
- DesignActionType: UI_DESIGN, UX_DESIGN, WIREFRAMING, PROTOTYPING
- WritingActionType: ARTICLE_WRITING, CREATIVE_WRITING_GEN

## REWRITE
- MarketingActionType: (allowed only)
- WritingActionType: EDITING, PROOFREADING, DOC_UPDATE
- DesignActionType: GRAPHIC_DESIGN, VISUAL_IDENTITY
- CreativeActionType: CREATIVE_WRITING

## EDIT, REFINE, IMPROVE
- WritingActionType: EDITING, PROOFREADING, DOC_UPDATE

## OUTLINE
- WritingActionType: ARTICLE_WRITING

## SUMMARIZE
- BusinessActionType: (allowed only)
- CustomerSupportActionType: (allowed only)
- ResearchActionType: LITERATURE_REVIEW, PAPER_WRITING
- EducationActionType: (allowed only)
- LegalSemanticProfileSeed: (allowed only)
- WritingActionType: EDITING, ARTICLE_WRITING

## EXPLAIN
- CustomerSupportActionType: SUPPORT_DOCUMENTATION, KNOWLEDGE_BASE_MANAGEMENT
- DevelopmentActionType: DOCUMENTATION, CODEBASE_ANALYSIS
- ResearchActionType: RESEARCH_DESIGN, METHODOLOGY_DEVELOPMENT
- EducationActionType: TEACHING_METHOD, LESSON_PLANNING
- DesignActionType: DESIGN_DOC, UI_DESIGN, UX_DESIGN
- LegalSemanticProfileSeed: CONTRACT_REVIEW
- WritingActionType: TECHNICAL_WRITING, DOC_UPDATE

## EVALUATE
- BusinessActionType: CONTRACT_REVIEW, RISK_ASSESSMENT
- DevelopmentActionType: CODE_REVIEW, CODEBASE_ANALYSIS
- DesignActionType: UI_DESIGN, UX_DESIGN, PRODUCT_DESIGN
- DataAnalysis: AnalysisActionType: COMPARATIVE_ANALYSIS
- LegalSemanticProfileSeed: CONTRACT_REVIEW, RISK_ASSESSMENT

## DIAGNOSE
- DevelopmentActionType: DEBUGGING, CODE_ANALYSIS
- DesignActionType: (DISCOURAGED)

## EXTRACT
- DataAnalysis: AnalysisActionType: DATA_ANALYSIS
- ResearchActionType: (allowed only)

## CLASSIFY
- DataAnalysis: (allowed only)

## INVESTIGATE, SYNTHESIZE
- ResearchActionType: DATA_INTERPRETATION, LITERATURE_REVIEW, STATISTICAL_MODELING

## CRITIQUE, RECOMMEND
- DesignActionType: UI_DESIGN, UX_DESIGN, PRODUCT_DESIGN, DESIGN_DOC

---

# Category → Intent → Action Mapping

Source: CategorySemanticProfileSeed.actionsByIntent. Each entry: (Category, Intent) → List<ActionTypeInterface>.

## PRODUCTIVITY
Source: ProductivitySemanticProfileSeed

### PLAN
- ProductivityActionType.SCHEDULE_PLANNING (ActionGroup: GENERAL_PLANNING)
- ProductivityActionType.TASK_AUTOMATION (ActionGroup: PERSONAL_PRODUCTIVITY)

### GENERATE
- (no actions in seed; intent allowed only)

### ANALYZE
- (no actions in seed; intent allowed only)

## BUSINESS
Source: BusinessSemanticProfileSeed

### PLAN
- BusinessActionType.BUSINESS_PLAN_DEVELOPMENT
- BusinessActionType.PROJECT_MANAGEMENT
- BusinessActionType.BUSINESS_STRATEGY

### PROPOSE
- BusinessActionType.PROPOSAL_WRITING
- BusinessActionType.BUSINESS_STRATEGY

### DECIDE
- BusinessActionType.RISK_ASSESSMENT
- BusinessActionType.CONTRACT_REVIEW

### ANALYZE
- BusinessActionType.FINANCIAL_ANALYSIS
- BusinessActionType.RISK_ASSESSMENT

### GENERATE
- BusinessActionType.PROPOSAL_WRITING
- BusinessActionType.REPORT_WRITING
- BusinessActionType.PRESENTATION_PREPARATION

### SUMMARIZE
- (allowed only)

## MARKETING
Source: MarketingSemanticProfileSeed

### GENERATE
- MarketingActionType.CONTENT_MARKETING
- MarketingActionType.AD_CAMPAIGN

### PLAN
- MarketingActionType.MARKETING_STRATEGY
- MarketingActionType.SOCIAL_MEDIA_STRATEGY

### ANALYZE, REWRITE
- (allowed only)

## CUSTOMER_SUPPORT
Source: CustomerSupportSemanticProfileSeed

### GENERATE
- CustomerSupportActionType.FAQ_CREATION
- CustomerSupportActionType.SUPPORT_DOCUMENTATION

### EXPLAIN
- CustomerSupportActionType.SUPPORT_DOCUMENTATION
- CustomerSupportActionType.KNOWLEDGE_BASE_MANAGEMENT

### PLAN
- CustomerSupportActionType.CUSTOMER_ONBOARDING
- CustomerSupportActionType.SUPPORT_TRAINING

### SUMMARIZE, ANALYZE
- (allowed only)

## DEVELOPMENT
Source: DevelopmentSemanticProfileSeed

### GENERATE
- CodingActionType.CODE_GENERATION
- CodingActionType.CODE_MODIFICATION
- CodingActionType.TEST_GENERATION

### DIAGNOSE
- CodingActionType.DEBUGGING
- CodingActionType.CODE_ANALYSIS

### EXPLAIN
- DevelopmentActionType.DOCUMENTATION
- DevelopmentActionType.CODEBASE_ANALYSIS

### EVALUATE
- CodingActionType.CODE_REVIEW
- DevelopmentActionType.CODEBASE_ANALYSIS

### IMPROVE
- CodingActionType.CODE_MODIFICATION
- DevelopmentActionType.CODEBASE_ANALYSIS

### PLAN
- DevelopmentActionType.ARCHITECTURE_DESIGN
- DevelopmentActionType.SYSTEM_DESIGN

### REFINE
- (allowed only)

## DATA_ANALYSIS
Source: DataAnalysisSemanticProfileSeed

### ANALYZE
- AnalysisActionType.DATA_ANALYSIS
- AnalysisActionType.COMPARATIVE_ANALYSIS
- AnalysisActionType.ROOT_CAUSE_ANALYSIS

### EVALUATE
- AnalysisActionType.COMPARATIVE_ANALYSIS

### EXTRACT
- AnalysisActionType.DATA_ANALYSIS

### SUMMARIZE, CLASSIFY
- (allowed only)

## RESEARCH
Source: ResearchSemanticProfileSeed

### INVESTIGATE
- ResearchActionType.DATA_INTERPRETATION
- ResearchActionType.LITERATURE_REVIEW

### ANALYZE
- ResearchActionType.DATA_INTERPRETATION
- ResearchActionType.LITERATURE_REVIEW
- ResearchActionType.STATISTICAL_MODELING

### SYNTHESIZE
- ResearchActionType.LITERATURE_REVIEW
- ResearchActionType.STATISTICAL_MODELING

### SUMMARIZE
- ResearchActionType.LITERATURE_REVIEW
- ResearchActionType.PAPER_WRITING

### EXPLAIN
- ResearchActionType.RESEARCH_DESIGN
- ResearchActionType.METHODOLOGY_DEVELOPMENT

### GENERATE
- ResearchActionType.PAPER_WRITING
- ResearchActionType.HYPOTHESIS_FORMULATION

### EVALUATE, EXTRACT
- (allowed only)

## EDUCATION
Source: EducationSemanticProfileSeed

### EXPLAIN
- EducationActionType.TEACHING_METHOD
- EducationActionType.LESSON_PLANNING

### PLAN
- EducationActionType.LESSON_PLANNING
- EducationActionType.CURRICULUM_DESIGN

### GENERATE
- EducationActionType.ASSESSMENT_DESIGN
- EducationActionType.MATERIAL_CREATION

### SUMMARIZE
- (allowed only)

## WRITING
Source: WritingSemanticProfileSeed

### CREATE
- WritingActionType.ARTICLE_WRITING
- WritingActionType.CREATIVE_WRITING_GEN

### GENERATE
- WritingActionType.ARTICLE_WRITING
- WritingActionType.CREATIVE_WRITING_GEN
- WritingActionType.COPYWRITING

### REWRITE
- WritingActionType.EDITING
- WritingActionType.PROOFREADING
- WritingActionType.DOC_UPDATE

### EDIT
- WritingActionType.EDITING
- WritingActionType.PROOFREADING

### REFINE
- WritingActionType.EDITING
- WritingActionType.DOC_UPDATE

### IMPROVE
- WritingActionType.EDITING
- WritingActionType.DOC_UPDATE

### OUTLINE
- WritingActionType.ARTICLE_WRITING

### SUMMARIZE
- WritingActionType.EDITING
- WritingActionType.ARTICLE_WRITING

### EXPLAIN
- WritingActionType.TECHNICAL_WRITING
- WritingActionType.DOC_UPDATE

### PLAN
- (allowed only)

## DESIGN
Source: DesignSemanticProfileSeed

### GENERATE
- DesignActionType.GRAPHIC_DESIGN
- DesignActionType.UI_DESIGN
- DesignActionType.VISUAL_IDENTITY

### CREATE
- DesignActionType.UI_DESIGN
- DesignActionType.UX_DESIGN
- DesignActionType.WIREFRAMING
- DesignActionType.PROTOTYPING

### EVALUATE
- DesignActionType.UI_DESIGN
- DesignActionType.UX_DESIGN
- DesignActionType.PRODUCT_DESIGN

### CRITIQUE
- DesignActionType.UI_DESIGN
- DesignActionType.UX_DESIGN
- DesignActionType.PRODUCT_DESIGN

### EXPLAIN
- DesignActionType.DESIGN_DOC
- DesignActionType.UI_DESIGN
- DesignActionType.UX_DESIGN

### PLAN
- DesignActionType.WIREFRAMING
- DesignActionType.PRODUCT_DESIGN
- DesignActionType.DESIGN_DOC

### REWRITE
- DesignActionType.GRAPHIC_DESIGN
- DesignActionType.VISUAL_IDENTITY

### RECOMMEND
- DesignActionType.PRODUCT_DESIGN
- DesignActionType.DESIGN_DOC

### DIAGNOSE
- (DISCOURAGED)

## LEGAL
Source: LegalSemanticProfileSeed

### EVALUATE
- LegalActionType.CONTRACT_REVIEW
- LegalActionType.RISK_ASSESSMENT

### GENERATE
- LegalActionType.CONTRACT_REVIEW
- LegalActionType.LEGAL_PROPOSAL_DRAFTING

### EXPLAIN
- LegalActionType.CONTRACT_REVIEW

### SUMMARIZE
- LegalActionType.CONTRACT_REVIEW
- LegalActionType.LEGAL_REPORT_OR_MEMO

## CREATIVE
Source: CreativeSemanticProfileSeed

### GENERATE
- CreativeActionType.IDEA_GENERATION
- CreativeActionType.CREATIVE_WRITING

### REWRITE
- CreativeActionType.CREATIVE_WRITING

### EXPLAIN, PLAN
- (allowed only)

## ETC
Source: EtcSemanticProfileSeed

- **No Intent → Action mappings** — `actions` map is empty. All intents allowed; no concrete actions bound to any intent.

## EXTRACTION

- **unclear** — No CategorySemanticProfileSeed. System-only category; no profile in DefaultCategorySemanticProfileRegistry.

---

# CompatibilityPolicySource Override

DefaultCompatibilityPolicySource uses empty map. No (Category, Intent) → ActionGroup overrides by default.
When non-empty, overrides action-derived groups for (category, intent) in DefaultCategorySemanticProfileRegistry.prepare().

---

# Source Summary

## Category source
- `PromptCategory.java` — enum, canonical(), isUiSelectable(), canonicalSemanticProfileCategories()
- `CategorySemanticProfileSeedDefinitions.java` — defaultDefinitions(), canonicalProfileCategories()

## Intent source
- `ActionIntent.java` — enum (29 intents)
- `IntentDefinition.java` — record with representativeCategories, representativeActionHints (hints only)
- `IntentDictionary.java` — definitions + resolution defaults aggregation
- `IntentDefinitionDataSource.java` — aggregates IntentDefinitionEntriesProvider, IntentResolutionDefaultsEntriesProvider

## Action source
- ActionType enums (28 classes in DefaultActionTypeCatalog): ProductivityActionType, DevelopmentActionType, CloudServicesActionType, DevOpsActionType, CybersecurityActionType, CodingActionType, ProgrammingActionType, AiMlActionType, AnalysisActionType, MarketingActionType, ContentCreationActionType, CreativeActionType, EducationActionType, LegalActionType, ResearchActionType, BusinessActionType, CustomerSupportActionType, EmailActionType, DesignActionType, WritingActionType, EtcActionType, HealthFitnessActionType, SocialActionType, CareerActionType, LifestyleActionType, PersonalDevelopmentActionType, RecommendationActionType, ShoppingActionType
- `ActionGroup.java` — enum (capability groups)
- `ActionTypeInterface.java` — key(), getActionGroup()
- `DefaultActionTypeCatalog.java` — enum class list
- `action-type-domain.properties` — TaskDomain per action (metadata)
- `action-type-output-behavior.properties` — OutputBehaviorType per action (metadata)

## Mapping source
- **Category → Intent**: `CategorySemanticProfileSeed.allowedIntents` (each *SemanticProfileSeed)
- **Category → Intent → Action**: `CategorySemanticProfileSeed.actionsByIntent` (each *SemanticProfileSeed)
- **Intent → Action (indirect)**: aggregated from seeds; IntentDefinition.representativeActionHints (hints, not canonical)
- **Action → ActionGroup**: `ActionTypeInterface.getActionGroup()` on each ActionType enum constant
- `DefaultCategorySemanticProfileRegistry` — builds profiles from seed; applies CompatibilityPolicySource override when non-empty
- `DefaultCategorySemanticProfileSeedSource` — indexes seed definitions from CategorySemanticProfileSeedDefinitions
