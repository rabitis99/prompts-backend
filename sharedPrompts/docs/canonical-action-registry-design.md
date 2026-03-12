# Canonical Action Registry — Architecture Design

**Purpose:** Design the Canonical Action Registry for the prompt engine. This document defines the canonical action layer that sits between existing 314 ActionType values and prompt assembly/resolver/profile logic. Design and diagnosis only; no code implementation.

**Current scale:** 314 ActionType constants, 27 ActionType enum classes, stable keys required, Category and Intent under consolidation.

**Target model:** `existing ActionType values → canonical action mapping → prompt assembly / resolver / profile logic`. Many existing values may map to one canonical action; fine-grained distinctions move to metadata, profile, template, resolver, or output behavior.

---

## 1. Executive Summary

### 1.1 Why the Current 314 Actions Need Canonicalization

The system has **314 action constants** across **27 enum classes**. Growth is driven by **surface variants** (channel, format, purpose, document type, scope) rather than by **distinct system capabilities**. Without a canonical layer:

- **Resolution and profiles** must reason over 314 actions; intent→action and category→action maps are large, brittle, and incomplete (e.g. `DefaultCategorySemanticProfileRegistry` uses only Writing, Design, Development, Business, etc.—ContentCreation, Email, Recommendation, Social, Lifestyle actions are not in any profile).
- **Duplicate semantics** are first-class: EDITING, PROOFREADING, DOC_UPDATE, CONTENT_REVISION are four actions for “modify existing text”; CODE_ANALYSIS and CODEBASE_ANALYSIS differ only by scope; DEPLOYMENT_STRATEGY (Development) and DEPLOYMENT_STRATEGY_DEVOPS (DevOps) are one capability; CREATIVE_WRITING (Creative) and CREATIVE_WRITING_GEN (Writing) are the same; RISK_ASSESSMENT (Business) and RISK_ASSESSMENT_SECURITY (Cybersecurity) are one capability with domain context.
- **Explosion path** is unchecked: the next “natural” additions (LINKEDIN_POST, TIKTOK_SCRIPT, SLACK_MESSAGE, NOTION_PAGE, GRANT_PROPOSAL, RFP_RESPONSE) would be surfaces, not new capabilities. 314 → 500+ in a few quarters is likely without governance.
- **Stable keys** are in APIs and storage; we cannot delete or rename. The only safe path is **canonicalization**: keep all keys, introduce a **canonical action** set, and map existing keys to canonical for internal logic.

### 1.2 Key Risk Areas

| Risk | Manifestation |
|------|----------------|
| **Channel explosion** | INSTAGRAM_CAPTION, FACEBOOK_POST, X_POST, (future: LINKEDIN_POST, TIKTOK_SCRIPT) — one capability (short copy for a surface), many enums. |
| **Format explosion** | BLOG_WRITING, NEWSLETTER_WRITING, ARTICLE_WRITING, ESSAY_WRITING — long-form writing; format should be profile/template. |
| **Purpose explosion** | THANK_YOU_EMAIL, APOLOGY_EMAIL, REJECTION_EMAIL, CONFIRMATION_EMAIL — one capability (email composition), purpose = metadata/template. |
| **Document-type explosion** | RESUME_WRITING, COVER_LETTER, APPLICATION_WRITING, GRANT_WRITING, PROPOSAL_WRITING — structured document writing; document type = profile/template. |
| **Scope explosion** | CODE_ANALYSIS vs CODEBASE_ANALYSIS; DAILY_PLANNING, WEEKLY_PLANNING, MONTHLY_PLANNING — scope = parameter/metadata. |
| **Package-driven duplication** | Same capability split across writing, content_creation, email, social, business (e.g. “compose short/long text” and “compose message”) with no single semantic owner. |

### 1.3 Target Architectural Direction

- **Introduce a Canonical Action Registry:** a closed set of **canonical actions** (target **~55–85**), each representing a **reusable system capability**.
- **Map every existing ActionType key** to exactly one canonical action (direct or alias). Deserialization and APIs keep stable keys; resolvers, profiles, and prompt assembly use **canonical action**.
- **Move variants out of the action taxonomy:** channel, platform, format, purpose, tone, document subtype, scope → **metadata**, **profile id**, **template**, **output behavior**, **resolver parameter**, or **domain context**.
- **Governance:** no new ActionType (or canonical action) unless it passes strict criteria; prefer “canonical action + metadata” over new enum values.

---

## 2. Canonical Action Registry Design Goal

### 2.1 What a Canonical Action Is

- A **reusable system capability** that meaningfully changes **prompt assembly**, **evaluation criteria**, or **reasoning workflow**.
- **Broader than** a single surface (e.g. one platform or one document type) but **narrower than** an intent (e.g. GENERATE, ANALYZE).
- **Globally unique** in the semantic sense: one canonical action = one distinct capability across the entire engine.
- The **unit of resolution**: profiles and resolvers reason over canonical actions; concrete ActionType keys are mapped to canonical at resolution time.

### 2.2 What It Is Not

- **Not a UI label** — UI can still show concrete or localized names (e.g. “Thank-you email”) while the system uses canonical ACTION_MESSAGE_COMPOSITION + purpose=thank_you.
- **Not a channel or platform** — Instagram, X, Slack are metadata or output behavior, not canonical actions.
- **Not a document subtype** — resume, cover letter, grant proposal are template/profile, not separate canonical actions.
- **Not a purpose or tone** — thank-you, apology, rejection are metadata/template.
- **Not a scope-only distinction** — “single file” vs “codebase” is parameter/metadata; one canonical action (e.g. CODE_ANALYSIS) suffices.

### 2.3 Why This Layer Is Necessary

- **Stop explosion:** New surfaces (new platform, new document type) become metadata/template/profile instead of new actions.
- **Stable semantics:** Resolvers and profiles depend on a bounded set of capabilities (~55–85), not 314 and growing.
- **Backward compatibility:** Existing 314 keys remain valid; they map to canonical. No breaking renames or deletions.
- **Clear boundaries:** Enforces “capability vs variant” at design time via governance and mapping rules.

---

## 3. Current Action Taxonomy Risk Analysis

### 3.1 Major Duplication Patterns

| Pattern | Examples (current actions) | Count impact | Recommended treatment |
|---------|----------------------------|--------------|------------------------|
| **Channel/surface** | SOCIAL_MEDIA_POST, INSTAGRAM_CAPTION, FACEBOOK_POST, X_POST, COMMENT_WRITING | 5 → 1 canonical | SHORT_COPY or SOCIAL_MARKETING_COPY + channel/platform metadata |
| **Email purpose** | EMAIL_WRITING, BUSINESS_EMAIL, PERSONAL_EMAIL, THANK_YOU_EMAIL, APOLOGY_EMAIL, INQUIRY_EMAIL, INVITATION_EMAIL, FOLLOW_UP_EMAIL, REJECTION_EMAIL, CONFIRMATION_EMAIL | 10 → 1 | MESSAGE_OR_EMAIL_COMPOSITION + purpose/tone metadata or template |
| **Message/greeting** | MESSAGE_WRITING, TEXT_MESSAGE, WHATSAPP_MESSAGE, FOLLOW_UP_MESSAGE, BIRTHDAY_MESSAGE, ANNIVERSARY_MESSAGE, HOLIDAY_GREETING, SEASONAL_GREETING, CONGRATULATORY_MESSAGE, CONDOLENCE_MESSAGE, INVITATION_CARD, THANK_YOU_CARD, NETWORKING_MESSAGE, APPOINTMENT_SCHEDULING, REMINDER_MESSAGE | 15+ → 1–2 | MESSAGE_COMPOSITION (and optionally SHORT_COPY for cards/greetings) + purpose/channel metadata |
| **Long-form format** | ARTICLE_WRITING, ESSAY_WRITING, BLOG_WRITING, NEWSLETTER_WRITING, CONTENT_CREATION, PAPER_WRITING, COPYWRITING, VIDEO_SCRIPT, PODCAST_SCRIPT | 9+ → 1–2 | LONG_FORM_WRITING (and optionally SCRIPT_WRITING) + format as profile/template |
| **Edit/revise** | EDITING, PROOFREADING, DOC_UPDATE, CONTENT_REVISION | 4 → 1 | TEXT_REVISION + level (proofread vs full revision) as metadata |
| **Recommend** | RECOMMENDATION, BOOK_RECOMMENDATION, MOVIE_RECOMMENDATION, RESTAURANT_RECOMMENDATION, PRODUCT_REVIEW, GIFT_SELECTION | 6 → 1 | RECOMMEND + domain as metadata/profile |
| **Plan (domain)** | DAILY_PLANNING, WEEKLY_PLANNING, MONTHLY_PLANNING, SCHEDULE_PLANNING, CONTENT_PLANNING, LESSON_PLANNING, WORKOUT_PLANS, DIET_PLANNING, MEAL_PLANNING, BUDGET_PLANNING, TRAVEL_PLANNING, EVENT_PLANNING, PARTY_PLANNING, TIME_OFF_PLANNING, EXERCISE_PLANNING, NUTRITION_PLANNING, RECOVERY_PLANNING, SCALABILITY_PLANNING | 18+ → 2–3 | PLANNING (or SCHEDULE_PLANNING, CONTENT_OR_STRATEGY_PLANNING, PERSONAL_PLANNING) + domain as metadata |
| **Code analysis** | CODE_ANALYSIS, CODEBASE_ANALYSIS | 2 → 1 | CODE_ANALYSIS + scope (unit/codebase) as parameter |
| **Risk assessment** | RISK_ASSESSMENT (Business), RISK_ASSESSMENT_SECURITY (Cybersecurity) | 2 → 1 | RISK_ASSESSMENT + domain (business/security) as context/category |
| **Deployment strategy** | DEPLOYMENT_STRATEGY (Development), DEPLOYMENT_STRATEGY_DEVOPS (DevOps) | 2 → 1 | DEPLOYMENT_STRATEGY + context from category |
| **Creative writing** | CREATIVE_WRITING (Creative), CREATIVE_WRITING_GEN (Writing) | 2 → 1 | CREATIVE_WRITING (one canonical); one key alias |
| **Career documents** | RESUME_WRITING, COVER_LETTER, APPLICATION_WRITING | 3 → 1 | CAREER_DOCUMENT_WRITING or DOCUMENT_WRITING + document type as template/profile |

### 3.2 Package-Driven Distortion

- **content_creation:** ContentCreationActionType, EmailActionType, RecommendationActionType. Semantically, email and recommendation are “writing/communication”; they live under content_creation for historical reasons. Category redesign maps content_creation → WRITING; actions are still split by package.
- **writing vs content_creation:** WritingActionType has ARTICLE_WRITING, COPYWRITING, EDITING; ContentCreationActionType has BLOG_WRITING, CONTENT_CREATION, NEWSLETTER_WRITING. Same domain (writing), two enums; no single canonical “long-form writing” owner.
- **business:** BusinessActionType, CareerActionType, CustomerSupportActionType. Career (resume, cover letter) is document writing; Customer Support mixes writing (FAQ, docs), planning (onboarding), analysis. Package is “business,” taxonomy is mixed.
- **etc:** EtcActionType, HealthFitnessActionType, LifestyleActionType, SocialActionType. “Etc” is a catch-all; Social includes strategy (COMMUNITY_BUILDING) and message writing (BIRTHDAY_MESSAGE, FOLLOW_UP_MESSAGE). Actions don’t align with a single category; many are “plan” or “recommend” or “message” by capability.

### 3.3 Likely Overlap Clusters (with current action examples)

| Cluster | Current actions (examples) | Overlap type |
|--------|----------------------------|---------------|
| Compose short/long text | ARTICLE_WRITING, BLOG_WRITING, NEWSLETTER_WRITING, CONTENT_CREATION, SOCIAL_MEDIA_POST, INSTAGRAM_CAPTION, X_POST, FACEBOOK_POST | Channel/format; one capability |
| Compose message/email | EMAIL_WRITING, BUSINESS_EMAIL, …; MESSAGE_WRITING, TEXT_MESSAGE, WHATSAPP_MESSAGE, BIRTHDAY_MESSAGE, … | Purpose/channel; one capability |
| Edit/revise text | EDITING, PROOFREADING, DOC_UPDATE, CONTENT_REVISION | Scope/rigor; one capability |
| Recommend | RECOMMENDATION, BOOK_RECOMMENDATION, MOVIE_RECOMMENDATION, PRODUCT_REVIEW, GIFT_SELECTION | Domain; one capability |
| Plan | DAILY/WEEKLY/MONTHLY_PLANNING, SCHEDULE_PLANNING, CONTENT_PLANNING, LESSON_PLANNING, WORKOUT_PLANS, DIET_PLANNING, MEAL_PLANNING, BUDGET_PLANNING | Domain; one capability |
| Code analysis | CODE_ANALYSIS, CODEBASE_ANALYSIS | Scope; one capability + scope |

### 3.4 Naming Patterns Indicating Taxonomy Drift

- **Platform in name:** INSTAGRAM_CAPTION, FACEBOOK_POST, X_POST → should be one action + platform metadata.
- **Purpose in name:** THANK_YOU_EMAIL, APOLOGY_EMAIL, REJECTION_EMAIL → should be one action + purpose/template.
- **Document type in name:** RESUME_WRITING, COVER_LETTER, GRANT_WRITING → should be one action + document template/profile.
- **Scope in name:** CODE_ANALYSIS vs CODEBASE_ANALYSIS, DAILY_PLANNING vs WEEKLY_PLANNING → scope as parameter.
- **Domain suffix to disambiguate:** RISK_ASSESSMENT_SECURITY, DEPLOYMENT_STRATEGY_DEVOPS → same capability, domain from category or context.
- **Duplicate name across enums:** CREATIVE_WRITING (Creative) vs CREATIVE_WRITING_GEN (Writing) → one canonical, one alias.

### 3.5 Package Boundaries That Are Semantically Misleading

- **ContentCreationActionType** contains both “content” (BLOG_WRITING, NEWSLETTER_WRITING) and “social post” (INSTAGRAM_CAPTION, X_POST). The latter are short copy + channel; the former are long-form + format. Capability-wise they belong to different canonical clusters (LONG_FORM_WRITING vs SHORT_COPY).
- **SocialActionType** mixes **strategy/community** (COMMUNITY_BUILDING, EVENT_ORGANIZATION, FUNDRAISING) with **message writing** (BIRTHDAY_MESSAGE, FOLLOW_UP_MESSAGE, HOLIDAY_GREETING). Strategy actions map to PLANNING/STRATEGIZE-style canonical actions; message actions map to MESSAGE_COMPOSITION.
- **LifestyleActionType** mixes planning (TRAVEL_PLANNING, EVENT_PLANNING, PARTY_PLANNING), recommendation (GIFT_SELECTION), and advice (RELATIONSHIP_ADVICE, CONFLICT_RESOLUTION). These map to different canonical actions (PLANNING, RECOMMEND, CONSULTATION/ADVICE).

---

## 4. Canonicalization Criteria

### 4.1 Exact Rules: When Something Deserves a Canonical Action

1. **Distinct capability:** The action represents a system capability that is **not** achievable by “existing canonical action + metadata/template/profile/parameter.”
2. **Prompt assembly impact:** The action implies **meaningfully different** prompt structure, instructions, or constraints (e.g. CODE_GENERATION vs DOCUMENTATION).
3. **Evaluation/reasoning impact:** The action implies **different** success criteria or reasoning workflow (e.g. CODE_REVIEW vs CODE_GENERATION).
4. **Not only channel/format/purpose/scope:** If the only difference is channel, platform, format, document subtype, purpose, tone, or scope (daily vs monthly, single file vs codebase), it does **not** get a new canonical action.

### 4.2 Exact Rules: When Something Is Only Metadata / Profile / Template / Scope

- **Channel or platform:** Instagram, X, Facebook, Slack, WhatsApp, email as medium → **metadata** (e.g. `channel`, `platform`) or **output behavior / template**.
- **Document type or format:** Resume, cover letter, blog, newsletter, grant proposal → **profile id** or **template**, not new action.
- **Purpose or tone:** Thank-you, apology, rejection, confirmation → **tone type**, **template**, or **metadata**.
- **Scope:** Single file vs codebase, daily vs weekly vs monthly → **parameter** or **resolver logic** (e.g. `scope=unit|codebase`).
- **Domain only:** “Security” vs “business” risk assessment → **category** or **context/metadata**; one canonical RISK_ASSESSMENT.
- **Output shape/length:** Already modeled by **OutputBehaviorType** (LONG_FORM_WRITING, SHORT_COPY, MESSAGE_COMPOSITION, etc.). Do not duplicate as actions.

---

## 5. Proposed Canonical Action Clusters

Below is a **strongly-defined target cluster set**. Each cluster has one canonical action (or at most two where capability genuinely differs). Current actions are grouped as **canonical** (remain as the canonical representative) or **legacy alias / concrete variant** (map to the canonical).

### 5.1 Writing & Content — Long-Form and Short Copy

| Canonical action | Description | Current actions (canonical = keep as representative; others = alias/variant) |
|------------------|-------------|---------------------------------------------------------------------------------|
| **LONG_FORM_WRITING** | Produce long-form text (article, blog, newsletter, content, essay, paper, copy) | CONTENT_CREATION (canonical), ARTICLE_WRITING, ESSAY_WRITING, BLOG_WRITING, NEWSLETTER_WRITING, COPYWRITING, PAPER_WRITING, GRANT_WRITING, PROPOSAL_WRITING, REPORT_WRITING — format/document type → profile/template |
| **SHORT_COPY** | Produce short copy, captions, posts, comments, headlines | SOCIAL_MEDIA_POST (canonical), INSTAGRAM_CAPTION, FACEBOOK_POST, X_POST, COMMENT_WRITING, INVITATION_CARD, THANK_YOU_CARD, HOLIDAY_GREETING, SEASONAL_GREETING — channel/platform → metadata |
| **SCRIPT_OR_MEDIA_WRITING** | Scripts and multimedia narrative content | VIDEO_SCRIPT, PODCAST_SCRIPT, MULTIMEDIA_PRODUCTION — format → metadata |
| **MESSAGE_COMPOSITION** | Compose message or email (any purpose/channel) | EMAIL_WRITING (canonical), BUSINESS_EMAIL, PERSONAL_EMAIL, THANK_YOU_EMAIL, APOLOGY_EMAIL, INQUIRY_EMAIL, INVITATION_EMAIL, FOLLOW_UP_EMAIL, REJECTION_EMAIL, CONFIRMATION_EMAIL, MESSAGE_WRITING, TEXT_MESSAGE, WHATSAPP_MESSAGE, FOLLOW_UP_MESSAGE, BIRTHDAY_MESSAGE, ANNIVERSARY_MESSAGE, CONGRATULATORY_MESSAGE, CONDOLENCE_MESSAGE, APPOINTMENT_SCHEDULING, REMINDER_MESSAGE, NETWORKING_MESSAGE — purpose/channel → metadata/template |
| **TEXT_REVISION** | Modify, edit, proofread, or revise existing text | EDITING (canonical), PROOFREADING, DOC_UPDATE, CONTENT_REVISION, CONTENT_OPTIMIZATION — level (proofread vs full revision) → metadata |
| **TECHNICAL_WRITING** | Technical documentation and explanatory text | TECHNICAL_WRITING, DOCUMENTATION — keep as-is or merge DOCUMENTATION here |
| **TRANSLATION** | Translate content | TRANSLATION — canonical as-is |
| **CREATIVE_WRITING** | Original creative/narrative writing | CREATIVE_WRITING (CreativeActionType — canonical), CREATIVE_WRITING_GEN (Writing — alias) |
| **LETTER_WRITING** | Compose letter (formal or personal) | LETTER_WRITING, PERSONAL_LETTER, BUSINESS_LETTER — could be MESSAGE_COMPOSITION + format=letter; or keep one canonical LETTER_WRITING |
| **CAREER_DOCUMENT_WRITING** | Structured career docs (resume, cover letter, application) | RESUME_WRITING (canonical), COVER_LETTER, APPLICATION_WRITING — document type → template/profile |
| **REVIEW_OR_FEEDBACK_WRITING** | Write review, testimonial, feedback, complaint | REVIEW_WRITING, TESTIMONIAL_WRITING, FEEDBACK_WRITING, COMPLAINT_WRITING — purpose → metadata |

### 5.2 Recommendation & Consultation

| Canonical action | Description | Current actions |
|------------------|-------------|-----------------|
| **RECOMMEND** | Recommend with reasoning (any domain) | RECOMMENDATION (Etc), BOOK_RECOMMENDATION, MOVIE_RECOMMENDATION, RESTAURANT_RECOMMENDATION, PRODUCT_REVIEW, GIFT_SELECTION — domain → metadata/profile |
| **EXPLANATION** | Explain concept, process, or topic | EXPLANATION, TEACHING_METHOD, EXPLAINABLE_AI — as-is or small cluster |
| **ADVICE_OR_GUIDANCE** | General advice, guidance, consultation | ADVICE, GUIDANCE, GENERAL_CONSULTATION, RECIPE_CREATION, COOKING_TIPS, RELATIONSHIP_ADVICE, CONFLICT_RESOLUTION, EDUCATION_CONSULTATION, DECORATION_IDEAS, HOBBY_EXPLORATION — domain → metadata |

### 5.3 Planning & Strategy

| Canonical action | Description | Current actions |
|------------------|-------------|-----------------|
| **PLANNING** | Produce a plan (schedule, content, lesson, travel, event, budget, meal, workout, etc.) | SCHEDULE_PLANNING (canonical), DAILY_PLANNING, WEEKLY_PLANNING, MONTHLY_PLANNING, CONTENT_PLANNING, LESSON_PLANNING, TRAVEL_PLANNING, EVENT_PLANNING, PARTY_PLANNING, TIME_OFF_PLANNING, BUDGET_PLANNING, MEAL_PLANNING, WORKOUT_PLANS, DIET_PLANNING, EXERCISE_PLANNING, NUTRITION_PLANNING, RECOVERY_PLANNING, SCALABILITY_PLANNING, CURRICULUM_DESIGN, EDUCATIONAL_STRATEGY — domain/scope → metadata |
| **STRATEGY** | High-level strategy (business, marketing, deployment, security, etc.) | BUSINESS_STRATEGY, MARKETING_STRATEGY, DEPLOYMENT_STRATEGY (Development), DEPLOYMENT_STRATEGY_DEVOPS (DevOps), SECURITY_POLICY, ARCHITECTURE_DESIGN, SYSTEM_DESIGN, TECH_STACK_SELECTION — domain from category |
| **PROJECT_OR_BUSINESS_PLAN** | Project management, business plan, stakeholder management | PROJECT_MANAGEMENT, BUSINESS_PLAN_DEVELOPMENT, STAKEHOLDER_MANAGEMENT |

### 5.4 Analysis & Evaluation

| Canonical action | Description | Current actions |
|------------------|-------------|-----------------|
| **DATA_ANALYSIS** | Analyze data, statistics, trends, patterns, BI | DATA_ANALYSIS, STATISTICAL_ANALYSIS, INSIGHT_EXTRACTION, TREND_ANALYSIS, PATTERN_RECOGNITION, PREDICTIVE_ANALYSIS, COMPARATIVE_ANALYSIS, ROOT_CAUSE_ANALYSIS, BUSINESS_INTELLIGENCE |
| **CODE_ANALYSIS** | Analyze code (single unit or codebase) | CODE_ANALYSIS (canonical), CODEBASE_ANALYSIS — scope → parameter/metadata |
| **RISK_ASSESSMENT** | Assess risk (business or security) | RISK_ASSESSMENT (Business), RISK_ASSESSMENT_SECURITY (Cybersecurity) — domain → category/context |
| **EVALUATION_OR_AUDIT** | Evaluate, audit, review merit | CODE_REVIEW, MODEL_EVALUATION, SECURITY_AUDIT, CONTRACT_REVIEW, COMPLIANCE_MANAGEMENT — domain from category |
| **MARKET_OR_CUSTOMER_ANALYSIS** | Market research, customer analysis | MARKET_RESEARCH, CUSTOMER_ANALYSIS, CUSTOMER_FEEDBACK_ANALYSIS |

### 5.5 Code & Development

| Canonical action | Description | Current actions |
|------------------|-------------|-----------------|
| **CODE_GENERATION** | Generate code, tests, or implementation | CODE_GENERATION, TEST_GENERATION, ALGORITHM_IMPLEMENTATION, DATA_STRUCTURE_DESIGN, API_DESIGN, CONCURRENT_PROGRAMMING |
| **CODE_MODIFICATION** | Modify, refactor, optimize code | CODE_MODIFICATION, REFACTORING, CODE_OPTIMIZATION, SYNTAX_OPTIMIZATION, LEGACY_CODE_MAINTENANCE |
| **DEBUGGING** | Diagnose and fix defects | DEBUGGING |
| **SECURITY_IMPLEMENTATION** | Implement or configure security | SECURITY_IMPLEMENTATION, DATA_ENCRYPTION, ACCESS_CONTROL, IDENTITY_MANAGEMENT, NETWORK_SECURITY, APPLICATION_SECURITY, FIREWALL_CONFIGURATION, VULNERABILITY_SCANNING, PENETRATION_TESTING |
| **THREAT_OR_INCIDENT** | Threat analysis, incident response, malware analysis | THREAT_ANALYSIS, INCIDENT_RESPONSE, MALWARE_ANALYSIS, SECURITY_MONITORING |
| **INFRASTRUCTURE_AS_CODE** | IaC, CI/CD, containers, config | CI_CD_PIPELINE, AUTOMATED_TESTING, INTEGRATION_TESTING, INFRASTRUCTURE_AS_CODE, CONTINUOUS_INTEGRATION, CONTINUOUS_DEPLOYMENT, CONTINUOUS_DELIVERY, CONTAINERIZATION, DOCKER_SETUP, KUBERNETES_ORCHESTRATION, CONFIGURATION_MANAGEMENT, VERSION_CONTROL |
| **CLOUD_OPERATIONS** | Cloud deploy, migrate, monitor, cost, backup, scaling | CLOUD_DEPLOYMENT, CLOUD_SECURITY, CLOUD_MONITORING, CLOUD_MIGRATION, AWS_ARCHITECTURE, AZURE_SETUP, GCP_CONFIGURATION, CONTAINER_ORCHESTRATION, SERVERLESS_ARCHITECTURE, CLOUD_COST_OPTIMIZATION, DISASTER_RECOVERY, CLOUD_BACKUP, LOAD_BALANCING, AUTO_SCALING, CLOUD_NETWORKING |
| **PERFORMANCE_OPTIMIZATION** | Performance tuning and optimization | PERFORMANCE_OPTIMIZATION, PERFORMANCE_MONITORING, LOG_MANAGEMENT, MONITORING_ALERTING, INFRASTRUCTURE_MONITORING |

### 5.6 AI/ML, Research, Education, Design, Business, Support, Lifestyle

| Canonical action | Description | Current actions (representative) |
|------------------|-------------|----------------------------------|
| **ML_MODEL_LIFECYCLE** | Train, evaluate, tune, deploy, monitor models | MODEL_TRAINING, PREDICTION, MODEL_EVALUATION, HYPERPARAMETER_TUNING, FEATURE_ENGINEERING, MODEL_SELECTION, DEEP_LEARNING, NEURAL_NETWORK_DESIGN, TRANSFER_LEARNING, REINFORCEMENT_LEARNING, NLP, COMPUTER_VISION, MODEL_DEPLOYMENT, MODEL_MONITORING, A_B_TESTING, DATA_LABELING, MODEL_OPTIMIZATION, EXPLAINABLE_AI, DATA_PREPROCESSING |
| **RESEARCH_METHODOLOGY** | Research design, methodology, experiment, literature, hypothesis | RESEARCH_DESIGN, PAPER_WRITING, METHODOLOGY_DEVELOPMENT, EXPERIMENT_DESIGN, DATA_INTERPRETATION, LITERATURE_REVIEW, HYPOTHESIS_FORMULATION, STATISTICAL_MODELING |
| **EDUCATION_DESIGN** | Curriculum, materials, assessment, lesson design | CURRICULUM_DESIGN, MATERIAL_CREATION, TEACHING_METHOD, LEARNER_ANALYSIS, ASSESSMENT_DESIGN, INTERACTIVE_CONTENT, LESSON_PLANNING |
| **DESIGN** | UI/UX, graphic, product, wireframe, prototype, visual identity | UI_DESIGN, UX_DESIGN, GRAPHIC_DESIGN, PRODUCT_DESIGN, DESIGN_DOC, WIREFRAMING, PROTOTYPING, VISUAL_IDENTITY, INTERACTION_DESIGN, RESPONSIVE_DESIGN |
| **CREATIVE_CONCEPT** | Ideas, storytelling, character, world-building, visual creation | IDEA_GENERATION, STORYTELLING, CONCEPT_DEVELOPMENT, VISUAL_CREATION, CHARACTER_DEVELOPMENT, WORLD_BUILDING, ARTISTIC_DESIGN |
| **PRESENTATION_OR_REPORT** | Prepare presentation or report | PRESENTATION_PREPARATION, REPORT_WRITING (if not under LONG_FORM_WRITING) |
| **FINANCIAL_ANALYSIS** | Financial analysis | FINANCIAL_ANALYSIS |
| **CUSTOMER_SUPPORT_OPERATIONS** | Tickets, KB, FAQ, onboarding, training, retention | TICKET_CREATION, TICKET_RESOLUTION, KNOWLEDGE_BASE_MANAGEMENT, LIVE_CHAT_SUPPORT, CUSTOMER_SERVICE, FAQ_CREATION, SUPPORT_DOCUMENTATION, CUSTOMER_ONBOARDING, CUSTOMER_RETENTION, COMPLAINT_HANDLING, CUSTOMER_FEEDBACK_ANALYSIS, SUPPORT_TRAINING, REMOTE_SUPPORT, CUSTOMER_SATISFACTION |
| **MARKETING_STRATEGY_AND_EXECUTION** | Marketing, branding, campaigns, SEO, conversion | MARKETING_STRATEGY, BRANDING, AD_CAMPAIGN, MARKET_RESEARCH, CUSTOMER_ANALYSIS, SEO_OPTIMIZATION, SOCIAL_MEDIA_STRATEGY, CONTENT_MARKETING, INFLUENCER_MARKETING, CONVERSION_OPTIMIZATION |
| **PRODUCTIVITY_AND_PERSONAL** | Workflow, time, tasks, goals, habits, stress, decisions | WORKFLOW_OPTIMIZATION, TIME_MANAGEMENT, TASK_AUTOMATION, EFFICIENCY_ANALYSIS, PRODUCTIVITY_PLANNING, PROCESS_IMPROVEMENT, RESOURCE_OPTIMIZATION, GOAL_SETTING, HABIT_FORMATION, SELF_IMPROVEMENT, STRESS_MANAGEMENT, MOTIVATION, DECISION_MAKING, HOME_ORGANIZATION, HOUSEHOLD_MANAGEMENT |
| **HEALTH_FITNESS** | Workout, nutrition, health management (aggregate or split by sub-capability) | WORKOUT_PLANS, NUTRITION_TRACKING, MEDICAL_RECORD_MANAGEMENT, FITNESS_GOAL_SETTING, EXERCISE_PLANNING, DIET_PLANNING, WEIGHT_MANAGEMENT, CARDIO_TRAINING, STRENGTH_TRAINING, FLEXIBILITY_TRAINING, RECOVERY_PLANNING, NUTRITION_PLANNING, MEAL_PREP, SUPPLEMENT_GUIDANCE, HEALTH_SCREENING, CHRONIC_DISEASE_MANAGEMENT, MENTAL_HEALTH, SLEEP_OPTIMIZATION, STRESS_MANAGEMENT_HEALTH, FITNESS_TRACKING, WORKOUT_FORM_CORRECTION |
| **SOCIAL_OR_COMMUNITY** | Community, events, advocacy, collaboration (strategy/execution, not message writing) | COMMUNITY_ENGAGEMENT, EVENT_ORGANIZATION, SOCIAL_CAUSE_SUPPORT, VOLUNTEER_COORDINATION, FUNDRAISING, COMMUNITY_OUTREACH, SOCIAL_MEDIA_ENGAGEMENT, NETWORKING_EVENT, COMMUNITY_BUILDING, SOCIAL_ACTIVISM, ADVOCACY, PUBLIC_SPEAKING, SOCIAL_IMPACT, COLLABORATION, PARTNERSHIP_BUILDING, CROSS_CULTURAL_COMMUNICATION |
| **PROBLEM_SOLVING** | General problem solving, information research | PROBLEM_SOLVING, INFORMATION_RESEARCH |
| **INTERVIEW_PREPARATION** | Interview prep | INTERVIEW_PREPARATION |
| **SHOPPING** | Comparison, negotiation | COMPARISON_SHOPPING, PRICE_NEGOTIATION |

(Some clusters above can be split or merged to land in the **55–85** canonical range; e.g. HEALTH_FITNESS could be one canonical or several (WORKOUT_PLANNING, NUTRITION_PLANNING, HEALTH_MANAGEMENT) depending on prompt-assembly differentiation.)

### 5.6 Summary: Canonical vs Legacy

- **Canonical:** The chosen representative for each cluster (e.g. EDITING for TEXT_REVISION, CODE_ANALYSIS for CODE_ANALYSIS, RECOMMENDATION for RECOMMEND). These are the ids used in the Canonical Action Registry.
- **Legacy/alias:** All other current ActionType values that map to that canonical. They keep their **stable key** for deserialization and API; resolution and profiles use the **canonical action id** only.

---

## 6. Mapping Strategy

### 6.1 Existing ActionType → Canonical Action

- **Direct canonical:** The action *is* the canonical representative (e.g. EDITING → TEXT_REVISION; CODE_ANALYSIS → CODE_ANALYSIS). Mapping: key → same canonical id.
- **Alias to canonical:** The action is a variant; it maps to one canonical (e.g. PROOFREADING → TEXT_REVISION; CODEBASE_ANALYSIS → CODE_ANALYSIS; THANK_YOU_EMAIL → MESSAGE_COMPOSITION; CREATIVE_WRITING_GEN → CREATIVE_WRITING).
- **Variant resolved by metadata/template/profile:** The concrete action key is still stored and deserialized; the resolver produces (canonical_action_id, metadata). E.g. RESUME_WRITING → CAREER_DOCUMENT_WRITING + template=resume; INSTAGRAM_CAPTION → SHORT_COPY + platform=instagram.

### 6.2 Implementation Approach (Conceptual)

- **Registry contract:** `CanonicalActionRegistry.resolve(ActionTypeInterface action) → CanonicalActionId` (and optionally metadata/template hints).
- **Stable keys unchanged:** All 314 keys remain valid; no renames or deletions.
- **Profile/resolver:** Profiles and resolvers store and compare by **canonical action id**; they do not branch on 314 concrete keys. New surfaces (e.g. “LinkedIn post”) add a new concrete action only if governance approves; otherwise they are “SHORT_COPY + platform=linkedin” via metadata/template.

---

## 7. Boundary Matrix

Examples of **current action → canonical action → what becomes metadata / profile / template / scope / output behavior**.

| Current action | Canonical action | Metadata | Profile / template | Scope / parameter | Output behavior |
|----------------|------------------|----------|--------------------|--------------------|-----------------|
| THANK_YOU_EMAIL | MESSAGE_COMPOSITION | purpose=thank_you | email_template | — | MESSAGE_COMPOSITION (existing) |
| INSTAGRAM_CAPTION | SHORT_COPY | platform=instagram | — | — | SHORT_COPY |
| RESUME_WRITING | CAREER_DOCUMENT_WRITING | — | document_type=resume | — | LONG_FORM_WRITING |
| CODEBASE_ANALYSIS | CODE_ANALYSIS | — | — | scope=codebase | ANALYTICAL_REPORT |
| PROOFREADING | TEXT_REVISION | level=proofread | — | — | (same as EDITING) |
| DEPLOYMENT_STRATEGY_DEVOPS | DEPLOYMENT_STRATEGY / STRATEGY | — | — | context=devops (from category) | CODE_IMPLEMENTATION vs STRATEGIC_PLAN |
| WEEKLY_PLANNING | PLANNING | scope=weekly | — | scope=weekly | STRATEGIC_PLAN |
| BOOK_RECOMMENDATION | RECOMMEND | domain=book | — | — | RECOMMENDATION_LIST |
| RISK_ASSESSMENT_SECURITY | RISK_ASSESSMENT | domain=security (or from category) | — | — | ANALYTICAL_REPORT |
| CREATIVE_WRITING_GEN | CREATIVE_WRITING | — | — | — | LONG_FORM_WRITING |

---

## 8. Naming Rules

### 8.1 Canonical Actions

- **Verb or verb-noun:** e.g. EDITING, CODE_ANALYSIS, RECOMMEND, PLANNING, MESSAGE_COMPOSITION.
- **Capability, not channel/format:** MESSAGE_COMPOSITION not EMAIL_WRITING; SHORT_COPY not INSTAGRAM_CAPTION; PLANNING not WEEKLY_PLANNING.
- **Globally unique:** One canonical id per capability across the engine.
- **Stable key:** Canonical ids should have a stable key scheme (e.g. `CANONICAL.LONG_FORM_WRITING` or reuse an existing representative key as the canonical id).

### 8.2 Future Concrete Actions (if they must exist)

- Same as current: key must be globally unique; prefer capability-based name.
- **Must not** be only channel/platform/format/purpose/scope; those go to metadata/template/profile.

### 8.3 Alias / Deprecated Actions

- **Do not rename.** Keep existing key(); document as “alias for canonical X” in registry and docs.
- **Naming:** Existing names stay; optional `@Deprecated` in code with “Maps to canonical X.” No new “alias” naming convention beyond mapping table.

---

## 9. Governance Rules

### 9.1 Checklist Before Adding a New ActionType

- [ ] **Synonym check:** Is there an existing action (or canonical action) with the same or nearly same capability? If yes, use it + metadata/template.
- [ ] **Channel/format check:** Does the new action differ only by channel, format, or output surface? If yes, add metadata/output behavior/template, not action.
- [ ] **Subcase check:** Is this a subcase of an existing action (e.g. “apology email” under “email composition”)? If yes, use parameters/template/profile.
- [ ] **Capability statement:** Can you state in one sentence the **distinct system capability** that this action introduces? If it’s “write X for Y,” prefer canonical action + X,Y as context.
- [ ] **Resolver/prompt impact:** Does this action require **different** prompt assembly, evaluation, or resolver behavior that cannot be achieved by existing canonical + metadata? If no, don’t add.
- [ ] **Canonical mapping:** If added, which canonical action does it map to? (Usually itself if it is a new capability; otherwise it must map to an existing canonical.)
- [ ] **Naming:** Does the name follow the convention (capability, not channel/format)? Is the key globally unique?

### 9.2 Checklist Before Adding a New Canonical Action

- [ ] **Distinct capability:** No existing canonical action + metadata/template/profile can express this.
- [ ] **Prompt/evaluation impact:** Meaningfully different prompt assembly or evaluation criteria.
- [ ] **Not only variant:** The new candidate is not only a channel, format, purpose, document type, or scope variant of an existing canonical.
- [ ] **Globally unique:** The capability is not already covered by another canonical action.

### 9.3 Examples: Valid vs Invalid Additions

| Proposed addition | Valid? | Reason |
|-------------------|--------|--------|
| LINKEDIN_POST | No | Channel/surface; use SHORT_COPY + platform=linkedin. |
| SLACK_MESSAGE | No | Channel; use MESSAGE_COMPOSITION + channel=slack. |
| RFP_RESPONSE | No | Document type; use LONG_FORM_WRITING or PROPOSAL_WRITING equivalent + template=rfp. |
| CODE_ANALYSIS (second enum) | No | Duplicate; use CODE_ANALYSIS + scope. |
| New action: “Explain code to junior” | Depends | If “explain” is same as EXPLAIN + context, use EXPLANATION + domain; if it implies different prompt flow, could be EXPLAIN_CODE or similar canonical (single canonical for “explain code”). |
| New canonical: SECURE_CODE_REVIEW | Depends | If it only adds “security” lens to CODE_REVIEW, use CODE_REVIEW + metadata security=true; if it implies different checklist and prompts, could be valid. |

---

## 10. Recommended Target Size

### 10.1 Recommended Canonical Action Count Range

- **Target: 55–85 canonical actions.**

### 10.2 Rationale

- **314 existing actions** collapse into clusters that are largely channel/format/purpose/scope variants. Experience from the clusters above: writing/message/short copy/long form → ~10–12 canonical; planning → 2–3; recommendation → 1; analysis → 4–6; code/DevOps/cloud/security → ~12–18; AI/ML → 1–2; research, education, design, business, support, lifestyle → ~15–25. Total lands in **~55–85**.
- **Upper bound (~85):** Allows splitting where prompt assembly or evaluation truly differs (e.g. separate canonical for SECURITY_AUDIT vs CODE_REVIEW if needed).
- **Lower bound (~55):** Prevents over-consolidation where distinct capabilities (e.g. TRANSLATION vs CREATIVE_WRITING vs MESSAGE_COMPOSITION) are merged.
- **Governance:** Keeping to this range requires strict “no new action/canonical without passing checklist” and “variant → metadata/template” discipline.

---

## 11. Migration-Safe Refactor Strategy

### 11.1 Introduce Canonical Actions Without Breaking Stable Keys

- **Do not rename or delete** any existing ActionType constant or its `key()`.
- **Add** a separate concept: **CanonicalActionId** (or reuse a single “representative” key per cluster as the canonical id).
- **Add** a **CanonicalActionRegistry** (or extend ActionTypeCatalog) that:
  - For each existing ActionType key, returns the **canonical action id** (and optional metadata/template hints).
  - Deserialization continues to accept all 314 keys; no change to ActionTypeDeserializer contract.

### 11.2 Preserve Deserialization Compatibility

- **ActionTypeDeserializer:** Unchanged; still accepts all current keys and returns the corresponding ActionTypeInterface enum constant.
- **After resolution:** When building ConfirmedSemanticAxes or when the profile/resolver runs, resolve `actionType.key()` → canonical action id via the registry. Downstream logic (profile lookup, prompt assembly) uses **canonical action id** only; the original key can still be stored for audit/API response if needed.

### 11.3 Support Legacy Actions During Transition

- **Phase 1:** Define the canonical set and the mapping table (existing key → canonical id). Implement the registry; no change to profiles yet.
- **Phase 2:** Profiles and resolvers start using canonical id for intent→action and category→action logic; legacy keys still accepted and mapped on read.
- **Phase 3:** New features and UI can prefer showing canonical action labels where appropriate while still sending/store legacy keys for compatibility. Deprecation markers in code/docs only; no removal.

### 11.4 How Resolver/Profile Logic Should Consume Canonical Actions

- **CategorySemanticProfile:** Store and match by **canonical action id** in the intent→actions map. When the request supplies an action (by stable key), resolve key → canonical id, then check membership in the profile’s action set.
- **Resolver:** After resolving category and intent, resolve action (if present) to canonical id; use canonical id for fallback and validation. Prompt assembly uses canonical id (and metadata/template from context) to select behavior and output shape.
- **OutputBehaviorType:** Can remain on the concrete ActionType enum; or optionally be defined per canonical action in the registry. Recommendation: keep on concrete enum for backward compatibility; registry can expose “default OutputBehaviorType for canonical id” derived from the representative action.

---

## 12. Final Recommendation

### 12.1 Best Canonical Action Architecture Pattern for This Codebase

- **Introduce a Canonical Action Registry** as the single place that maps every existing ActionType key to a **canonical action id** (target **55–85** canonical actions).
- **Keep all 314 stable keys**; do not delete or rename. Map them to canonical for **semantic resolution and profile logic** only.
- **Treat canonical action as the capability layer:** prompt assembly, evaluation, and resolver behavior are keyed by canonical action (and category, intent, metadata). Concrete action keys are preserved for API and deserialization.
- **Move variants out of the taxonomy:** channel, platform, format, purpose, tone, document type, scope → **metadata**, **profile**, **template**, **output behavior**, or **resolver parameter**. Enforce via governance checklists before adding any new ActionType or canonical action.
- **Implement migration in three phases:** (1) registry + mapping table, (2) profiles/resolvers use canonical id, (3) optional UI/defaults prefer canonical while keeping legacy keys. This preserves backward compatibility and stops action explosion without losing capability coverage.

This design is concrete enough to become the basis for a real refactor: the clusters and mapping strategy above can be turned into a mapping table and registry implementation, and the governance checklists can be added to contribution guidelines and PR review.

---

## 13. Implementation Summary (refactor applied)

- **CanonicalActionId** enum and **CanonicalActionRegistry** (with DefaultCanonicalActionRegistry) were added. Every ActionType key maps to one canonical action via ActionToCanonicalMapping.
- **Backward compatibility:** ActionTypeDeserializer and all stable keys unchanged. Validation and recommendation use canonical equality (sameCanonicalCapability) so that e.g. PROOFREADING and EDITING both match profile TEXT_REVISION.
- **ObjectiveMappingRegistry** resolves by canonical: explicit mappings are stored by CanonicalActionId, so all actions that map to the same canonical share the same objective.
- **ConfirmedSemanticAxes** exposes canonical via `canonicalActionId(CanonicalActionRegistry)` for prompt assembly.
- **Governance:** See `action.canonical` package-info and this document. New ActionType must be registered in ActionTypeCatalog and in ActionToCanonicalMapping; prefer canonical + metadata over new constants.

---

## 14. Canonical-first internal execution (phase 2)

Internal engine flow is **canonical-first**: external compatibility stays concrete (request/API/deserialization); internal semantic decisions use `CanonicalActionId`.

### 14.1 Execution model

```
incoming request
  → existing ActionType (stable external value)
  → CanonicalActionRegistry.toCanonical(...)
  → CanonicalActionId
  → profile lookup / validation / prompt assembly / objective / output behavior
```

- **Profile registry:** `DefaultCategorySemanticProfileRegistry` builds profiles with both concrete action lists (for API/response) and **canonical capability maps** per intent. Compatibility checks use `getCompatibleCanonicalActionsForIntent(intent)`; validation and recommendation compare by canonical containment.
- **Validation / recommendation:** Action–intent compatibility is determined by whether the request action’s canonical id is in the profile’s compatible canonical set for that intent. Concrete lists remain for returning recommendation candidates.
- **Prompt assembly:** `RuleContext` carries optional `CanonicalActionId`; `PromptSpecFactory.createFromConfirmedAxes` resolves canonical from `ConfirmedSemanticAxes.canonicalActionId(registry)` and passes it into `RuleContext`. Downstream (e.g. guideline applicability, future assembly branches) should branch on `canonicalActionId()` where capability is the discriminator.
- **Objective resolution:** Already canonical: `ObjectiveMappingRegistry` stores and resolves by `CanonicalActionId`; `findByActionType` converts to canonical first.

### 14.2 Adding new actions

1. Add the concrete enum and register in `ActionTypeCatalog` and `ActionToCanonicalMapping` (map to an existing or new `CanonicalActionId`).
2. If the capability is already covered by an existing canonical, map to that id; profile compatibility is then automatic for any intent that lists that canonical.
3. Do not add new branching on concrete `ActionType` in prompt assembly or profile logic; use canonical (and category/intent/metadata) instead.
