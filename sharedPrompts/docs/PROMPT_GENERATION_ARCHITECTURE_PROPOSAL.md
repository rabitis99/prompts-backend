# Prompt Generation API — Architectural Design Proposal

## Executive Summary

This document proposes an architecture that keeps **user input simple** while ensuring **precise prompt generation** by separating UX, API input, internal semantic model, and composition pipeline—and by **never inventing major semantic meaning** inside the engine. The engine **composes** user-provided axes; it does **not guess** intent, role, or action when they are missing.

---

## 1. Core Semantic Axes Required for Prompt Generation

These are the minimal axes that, when **explicit or safely derived**, yield deterministic, high-quality prompts.

| Axis | Purpose | Drives |
|------|---------|--------|
| **Domain** | Problem space (design, dev, writing, data, etc.) | Guidelines, constraints, section ordering |
| **Intent** | What the user wants the model to do (generate, critique, extract, explain, etc.) | Objective, output shape, verification rubric |
| **Role** | Persona/expertise of the AI (optional but high-impact) | Role section, tone/authority |
| **Output format** | Free text vs structured vs JSON vs code | Output contract, decoding strategy |
| **Tone** | Neutral, formal, friendly, etc. | Wording in instructions |
| **Style** | Narrative, bullet, step-by-step, etc. | Structure hints in prompt |
| **Language** | Output locale | Guideline and role i18n |
| **Experience level** | Audience sophistication | Constraint strictness, length |

**Critical distinction:**

- **Domain** = *where* the task lives (DESIGN, DEVELOPMENT, WRITING, ANALYTICAL, …).
- **Intent** = *what* the user wants (GENERATE, REWRITE, EXTRACT, EXPLAIN, CRITIQUE, …).
- **Role** = *who* the AI is (optional; can be omitted and then **no role section** is composed).

The current system conflates “category” (often used as domain) with “intent,” and when role/action are missing it **defaults** them (e.g. `EtcRoleType.GENERAL_CONSULTANT`, `EtcActionType.GENERAL_CONSULTATION`), which is exactly the “internal inference” that produces wrong prompts (e.g. design → graphic designer / visual creation instead of critique or strategy).

---

## 2. Which Axes: Explicit vs Suggested vs Safe Inference

### 2.1 Principle: No Invention of Major Semantics

- **Explicit:** User or client selects a value; engine uses it as-is.
- **Suggested:** UI/API suggests a value (e.g. from intent or category); user can override; if not overridden, treat as explicit once chosen at UX layer.
- **Safe inference:** Only **formatting, normalization, or non-semantic defaults** (e.g. max length, default locale). **Never** infer intent, role, or action from category alone.

### 2.2 Recommended Classification

| Axis | Source | Rule |
|------|--------|------|
| **Domain** | Explicit (category/domain picker) or from **request_type** (EXTRACTION → ANALYTICAL) | If missing for SIMPLE/ADVANCED → **fail or ask**, do not default to GENERAL and proceed. |
| **Intent** | Explicit (generate, rewrite, extract, explain, critique, …) | If missing → **fail or ask**. No silent default to GENERATE. |
| **Role** | Explicit or **suggested by UI** from (domain + intent) | If missing → **omit role section** in PromptSpec; do not substitute a generic role. |
| **Action type** | Explicit (ADVANCED) or **derived from Intent** via fixed mapping (Intent → ActionType) | Only use a deterministic mapping table; no heuristics from free text. |
| **Output format** | Explicit (json_schema, output_needs) or from intent (EXTRACT → JSON) | EXTRACTION request_type forces JSON; otherwise from intent or explicit. |
| **Tone / Style / Language / Experience** | Explicit or safe defaults | Neutral, Narrative, KOREAN, INTERMEDIATE are **presentation defaults**, not semantic guesses. |

### 2.3 What the Engine Must Not Do

- Infer **role** from category (e.g. DESIGN → GRAPHIC_DESIGNER).
- Infer **action** from category (e.g. DESIGN → VISUAL_CREATION).
- Infer **intent** from input text (e.g. “improve this” → REWRITE).
- Use a “fallback” domain (e.g. GENERAL) and then proceed as if the user had chosen it.

Instead:

- **Missing required axis (domain or intent)** → validation error or structured “clarification” response (e.g. list of allowed intents/domains).
- **Missing role** → no role section; prompt is still valid.
- **Missing action type** → derive only from **intent** via a single, transparent mapping (e.g. Intent.DESIGN → DesignActionType.CREATE or a generic CREATIVE_GENERATE), and document that mapping.

---

## 3. Recommended Internal Model: PromptSpec (and ResolvedInput)

### 3.1 PromptSpec as “Fully Resolved” Spec

PromptSpec should represent **only fully resolved, non-null semantics** for axes that affect content. Optional axes (e.g. role) are either present or absent, not “defaulted to a generic value.”

Suggested shape (conceptually):

```text
PromptSpec
├── objective: PromptObjective          // from intent (+ request_type for EXTRACTION)
├── taskDomain: TaskDomain             // from category/domain only (no fallback to GENERAL)
├── role: Optional<RoleTypeInterface>  // present only if user or UI provided it
├── actionType: Optional<ActionType>   // present only if derived from intent or provided
├── outputContract: OutputContract     // from json_schema or intent
├── tone, style, locale, experienceLevel
├── rawInput, clarifiedInput
├── strategyBundle, rubric, constraints, sections, ...
```

Invariants:

- **taskDomain** is never a “fallback” value when the user sent a specific category; if we cannot resolve domain from input, we do not build a PromptSpec (or we return a validation error).
- **role** is `Optional`. If absent, the composition pipeline does **not** add a “You are a general consultant” section.
- **actionType** is optional; when present it comes from explicit user input or from a **single** Intent → ActionType mapping table.

### 3.2 ResolvedInput / Clarification Result

Before building PromptSpec, the pipeline produces a **ResolvedInput** (or equivalent) that carries:

- **domain:** TaskDomain (required; from category or request_type).
- **intent:** ActionIntent (required).
- **objective:** PromptObjective (from intent + request_type; no guessing).
- **role:** Optional (only if user or UI provided core_role/domain_role or role_type).
- **actionType:** Optional (only if user provided or from intent mapping).
- **outputContract:** From json_schema or intent.
- **tone, style, language, experience:** With safe defaults.

This makes “what was resolved” explicit and avoids hidden defaults inside PromptSpecFactory.

---

## 4. Recommended Input DTO Model

### 4.1 Layering

- **UX layer:** Presents few choices (e.g. category, intent, optional role). Can suggest role based on (category, intent) but user confirms.
- **API input (DTO):** Carries exactly what the client sends; no semantic defaults that change meaning.
- **Orchestration:** Validates required axes; maps intent → objective/action when applicable; **never** invents domain/role/action.

### 4.2 Request Types and Required Fields

**SIMPLE**

- **Required:** `request_type`, `input`, `category` (or `domain`), `intent`.
- **Optional:** `variant`, `tone`, `style`, `language`, `experience`, `title`, `description`, `tags`.
- **No role/action in DTO** → engine does not add role/action; optional “suggested role” could be a separate field the client can send when the UX suggests it.

**EXTRACTION**

- **Required:** `request_type`, `input`, `json_schema`.
- **Optional:** `language`, `title`, `description`, `tags`.
- **Implicit:** intent = EXTRACT, domain = ANALYTICAL, output = JSON; no need for category/intent in DTO.

**ADVANCED**

- **Required:** `request_type`, `input`, `category` (or `domain`), `intent`.
- **Optional:** All current fields including `action_type`, `role_type`, `core_role`, `domain_role`, `json_schema`, etc.
- When provided, they override any intent-based derivation.

### 4.3 Validation Rules (API / Orchestration)

- SIMPLE and ADVANCED: if `category` (or `domain`) is null → **400** with clear message (“category is required”).
- SIMPLE and ADVANCED: if `intent` is null → **400** (“intent is required”).
- Do **not** default category to ETC or intent to GENERATE in the API layer when they are missing; fail fast.

---

## 5. How to Avoid Incorrect Prompt Inference

### 5.1 Single Source of Meaning

- **Domain** → only from: (1) user-selected category/domain, or (2) request_type EXTRACTION → ANALYTICAL. Never from “action type” or “role” when those are missing.
- **Intent** → only from: (1) user-selected intent, or (2) request_type EXTRACTION → EXTRACT. Never from free-text input.
- **Role** → only from: (1) user/UI-provided role_type or (core_role + domain_role). If missing → omit role in spec.
- **Action type** → only from: (1) user-provided action_type (ADVANCED), or (2) deterministic Intent → ActionType table. Never from category alone.

### 5.2 Remove Silent Defaults That Change Meaning

- In **GeneratePromptCommand** (or equivalent), do **not** set `actionType = EtcActionType.GENERAL_CONSULTATION` or `roleType = EtcRoleType.GENERAL_CONSULTANT` when the values are null. Instead, pass null and let the pipeline treat “no role” and “no action” as such (omit role section; derive action only from intent if needed).
- In **DomainResolver**, do not return `TaskDomain.GENERAL` as a fallback when category is DESIGN or DEVELOPMENT; either resolve from category or return an error/ambiguity.

### 5.3 Use Routing Decision in the Pipeline

Currently, **UnifiedPromptGenerationOrchestrator** builds a **RoutingDecision** (objective, finalDomain, coreRole, domainRole) but **does not** pass these into **GeneratePromptCommand**. So the actual generation uses raw command fields (often null) and then applies the above defaults. Fix: **toV2Command** (or the new equivalent) must pass:

- **decision.finalDomain()** into the command/spec path so that domain is the one the routing layer resolved (from category + intent affinity), not from a later fallback.
- **decision.objective()** so that the objective is the one chosen by intent/rules, not re-resolved from (domain, defaulted action).
- **decision.coreRole() / decision.domainRole()** (or a single resolved RoleTypeInterface) when the user did not provide role—only if the product decision is “intent may suggest a default role”; otherwise leave role null and omit role section.

Then **GeneratePromptService.clarify()** should build PromptSpec from this **resolved** command (or from a ResolvedInput produced by a dedicated resolver pipeline), not from raw command + DomainResolver again with null actionType.

---

## 6. Recommended Resolver Pipeline

A clear pipeline separates “resolve semantics” from “compose prompt.”

### 6.1 Pipeline Stages

```text
Request (DTO)
    → ValidateRequiredAxes
    → IntentResolver (intent + request_type → objective, output shape)
    → DomainResolver (category + request_type → taskDomain; no fallback)
    → RoleResolver (explicit role only; or null)
    → ActionResolver (explicit action_type or intent → action mapping)
    → OutputContractResolver (json_schema or intent → output contract)
    → StylePolicy (tone, style, language, experience; safe defaults)
    → ResolvedInput
    → PromptSpecFactory.create(ResolvedInput)
    → PromptSpec
    → PromptRenderer
    → Solve → Verify → Repair
```

### 6.2 Stage Responsibilities

- **ValidateRequiredAxes:** For SIMPLE/ADVANCED: category and intent required; return 400 if missing. For EXTRACTION: json_schema required.
- **IntentResolver:** request_type + intent → objective, OutputNeeds, ResponseShape. EXTRACTION forces EXTRACT + JSON. No inference from text.
- **DomainResolver:** category → TaskDomain; request_type EXTRACTION → ANALYTICAL. If category is null for SIMPLE/ADVANCED → error. No GENERAL fallback when category is set.
- **RoleResolver:** If command has role_type or (core_role + domain_role) → resolve to RoleTypeInterface. Else → null (no role section).
- **ActionResolver:** If command has action_type → use it. Else if intent has a single canonical action type → use it. Else → null (objective still from intent).
- **OutputContractResolver:** json_schema present → JSON contract; else from intent (EXTRACT → JSON, etc.).
- **StylePolicy:** tone, style, language, experience with safe, non-semantic defaults.

### 6.3 Flow Into GeneratePromptService

- **Clarify** receives a **ResolvedInput** (or a command that already carries resolved domain, objective, role, action). It no longer calls DomainResolver with a possibly null actionType; it uses the resolved domain and optional role/action.
- PromptSpecFactory.create(ResolvedInput) builds PromptSpec with optional role (null → no role section), required domain/objective, and optional actionType.

This keeps “resolution” in one place and “composition” in another, and avoids duplicate or contradictory resolution in the service.

---

## 7. Examples: How the System Handles Each Case

### 7.1 Design Request (e.g. “design critique”)

- **User (SIMPLE):** category = DESIGN, intent = EVALUATE (or CRITIQUE if added), input = “Review this landing page layout.”
- **ValidateRequiredAxes:** category and intent present → OK.
- **IntentResolver:** EVALUATE → objective REASONING, structured output, analyst-style intent metadata.
- **DomainResolver:** DESIGN → TaskDomain.CREATIVE.
- **RoleResolver:** No role in DTO → null → **no role section** (or UX could suggest “Design Critic” and user sends role_type; then role section is added).
- **ActionResolver:** EVALUATE → optional mapping to a design evaluation action type if defined; else null.
- **PromptSpec:** domain CREATIVE, objective REASONING, no role, user input “Review this landing page layout.” Guidelines from CREATIVE + objective.
- **Result:** Prompt asks for a structured evaluation/critique without forcing “You are a graphic designer” or “visual creation.”

### 7.2 Development Request

- **User (SIMPLE):** category = DEVELOPMENT, intent = CODE, input = “Implement a retry with exponential backoff.”
- **IntentResolver:** CODE → CODE objective, code output.
- **DomainResolver:** DEVELOPMENT → TaskDomain.TECHNICAL.
- **RoleResolver:** null → no role section (or UX suggests Technical Expert and user sends it).
- **PromptSpec:** TECHNICAL, CODE objective, code output contract.
- **Result:** Code-oriented prompt without inventing a specific dev role if user did not choose one.

### 7.3 Writing Request

- **User (SIMPLE):** category = WRITING, intent = REWRITE, input = “Make this more concise.”
- **IntentResolver:** REWRITE → CREATIVE, free-form, editor default role in metadata only if we use it for suggestion.
- **DomainResolver:** WRITING → TaskDomain.CREATIVE.
- **RoleResolver:** null → no role section.
- **PromptSpec:** CREATIVE, REWRITE objective, narrative/structured as per intent.
- **Result:** Rewrite-focused prompt; no default “copywriter” unless user or UI sent it.

### 7.4 Data Extraction Request

- **User (EXTRACTION):** request_type = EXTRACTION, input = “…”, json_schema = “…”.
- **ValidateRequiredAxes:** json_schema present → OK.
- **IntentResolver:** EXTRACTION → EXTRACT, JSON required.
- **DomainResolver:** EXTRACTION → ANALYTICAL.
- **RoleResolver:** null.
- **OutputContractResolver:** json_schema → JSON contract.
- **PromptSpec:** ANALYTICAL, EXTRACTION objective, JSON output. No role section.
- **Result:** Extraction prompt with constrained decoding; no extra semantics inferred.

---

## 8. Trade-offs and Recommendations

### 8.1 Trade-offs

| Approach | Pro | Con |
|----------|-----|-----|
| **Require category + intent for SIMPLE** | No wrong domain/objective from defaults | Slightly more required fields in API |
| **Optional role (null = no role section)** | No wrong persona; simpler prompts when role irrelevant | Some prompts may feel “generic” until UX suggests roles |
| **Intent → action mapping table** | Single, auditable derivation; no guessing from category | One more mapping to maintain |
| **Routing decision fed into generation** | Pipeline uses same domain/objective/role as routing | Tighter coupling between orchestrator and command/spec |

### 8.2 Recommended Design Decisions

1. **Require category and intent** for SIMPLE and ADVANCED; return 400 when missing.
2. **Do not default role or action** when null; **omit role** and derive action only from intent via a **documented mapping**.
3. **Use RoutingDecision (or ResolvedInput) in the generate pipeline** so that domain, objective, and optional role come from the resolver pipeline, not from a second resolution with fallbacks.
4. **Keep PromptSpec.role Optional** and build the role section only when role is present.
5. **Document** the Intent → Objective and Intent → ActionType (when used) mappings so that “simple input” still yields deterministic behavior.
6. **UX:** For “simple” flows, suggest (domain, intent) and optionally suggest a role based on (domain, intent); when user confirms, send those as explicit fields so the engine does not have to infer.

---

## 9. Summary

- **Core semantic axes:** Domain, Intent, Role (optional), Output format, Tone, Style, Language, Experience.
- **Explicit:** Domain, Intent (for SIMPLE/ADVANCED). **Suggested by UI:** Role. **Safe inference:** Tone, style, language, experience. **Deterministic derivation:** Action from Intent table; Objective from Intent + request_type.
- **PromptSpec:** Required domain/objective; optional role/action; no “general consultant” or “general consultation” when null.
- **Input DTO:** Require category + intent where needed; no semantic defaults in DTO layer.
- **Avoid incorrect inference:** Single source of meaning per axis; no role/action from category; use routing decision in generation; validate required axes and fail fast.
- **Resolver pipeline:** ValidateRequiredAxes → Intent → Domain → Role → Action → OutputContract → StylePolicy → ResolvedInput → PromptSpec → Render → Solve/Verify/Repair.

The engine **composes** user- and UI-provided semantics and **never invents** intent, role, or action from category or free text alone. That keeps the UX simple while keeping generated prompts accurate and predictable.
