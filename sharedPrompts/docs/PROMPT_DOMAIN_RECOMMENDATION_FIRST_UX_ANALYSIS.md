# Prompt Domain — Recommendation-First UX Architecture Analysis

**Scope:** `src/main/java/org/example/sharedprompts/domain/prompt`  
**Principle:** *Engine recommends first, user confirms finally.*

---

## Executive Summary

The current prompt generation flow is **single-phase and engine-finalizing**: one `POST /prompts/generate` call performs semantic resolution (including intent fallback and role/action recommendation) and then immediately generates the final prompt. The backend **does** return rich metadata (`recommendation_hints`, `validation_warnings`, `semantic_resolution_summary`, `resolved_*`), but:

- **No per-axis source** — The response does not label which values are `userSelected`, `recommended`, or `fallbackApplied`. The frontend cannot reliably show “you chose this” vs “we suggested this” without parsing free-text hints.
- **No separate recommendation step** — Resolution and generation are coupled in `UnifiedPromptGenerationOrchestrator`. There is no “recommend only” endpoint; the only way to get recommendations is to generate, and generation uses whatever was resolved (including fallbacks) as final.
- **Silent finalization** — When intent is omitted (or command built without it), `SemanticResolutionService` applies `CategorySemanticProfile.getFallbackIntent()` and continues. Role/action are set from `SemanticRecommendationService` (first candidate) when not provided. Objective/outputNeeds come from `IntentDictionary` with no user confirmation. Validation warnings do not block; they only appear in metadata.

**Verdict:** Recommendation-first UX is **better** for this engine. The highest-impact refactor is **Phase 1 + Phase 2**: add explicit recommendation metadata (per-axis source) to the existing response, then introduce a dedicated **recommendation endpoint** that returns suggested axes without generating. After that, split the flow so that **confirmed generation** consumes only explicitly confirmed (or explicitly accepted default) axes.

---

## 1. Current Structure Analysis

### 1.1 Controllers

| Class | Package | Responsibility |
|------|---------|-----------------|
| **UnifiedPromptEngineController** | `adapter.in.web.controller` | Single endpoint `POST /prompts/generate`. Accepts polymorphic `UnifiedGeneratePromptRequest` (SIMPLE / EXTRACTION / ADVANCED), maps to `UnifiedGeneratePromptCommand`, calls `GenerateUnifiedPromptUseCase.generate(command)`, maps result to `UnifiedGeneratePromptResponse`. Async 60s timeout. |
| **PromptController** | `adapter.in.web.controller` | CRUD: list, my prompts, user prompts, get by id, PATCH, DELETE. No generation. |

**Finding:** Generation is concentrated in one controller and one endpoint. No dedicated recommendation or options endpoint.

### 1.2 DTO Structure

**Request (adapter.in.web.dto.request):**

- **UnifiedGeneratePromptRequest** (sealed): `request_type`, `input`, `tags`, `title`, `description`, `toCommand(userId)`.
- **SimpleGeneratePromptRequest**: `category`, `intent` (both `@NotNull`), `variant`, `input`, tone/style/language/experience, tags, title, description. No role/action; built as `SemanticSelection(category, intent, null, null)`.
- **AdvancedGeneratePromptRequest**: Same plus optional `role_type`, `action_type`, `json_schema`, `engine_mode`, `disable_quality_pipeline`.
- **ExtractionGeneratePromptRequest**: `input`, `json_schema` (required), language, tags, title, description. No category/intent; `SemanticSelection = null`.

**Response (adapter.in.web.dto.response):**

- **UnifiedGeneratePromptResponse**: `output`; engine mode/profile; `resolved_category`, `resolved_domain`, `objective`, `output_needs`, `resolved_intent`, `variant`, `resolved_role`, `resolved_action`; quality badges and verify/repair/schema flags; `semantic_profiles_applied`, `validation_warnings`, `recommendation_hints`, `semantic_resolution_summary`.

**Finding:** One response type mixes final prompt output and resolution metadata. No structural distinction between “user-selected”, “recommended”, or “fallback” for each axis.

### 1.3 Application Services / Use Cases

| Layer | Class | Responsibility |
|-------|------|----------------|
| Port In | **GenerateUnifiedPromptUseCase** | `generate(UnifiedGeneratePromptCommand) → UnifiedGeneratePromptResult`. |
| Port In | **GeneratePromptUseCase** | `generate(GeneratePromptCommand, ConfirmedSemanticAxes) → GeneratePromptResult`. |
| Orchestration | **UnifiedPromptGenerationOrchestrator** | Implements unified use case: resolve (SemanticResolutionService) → toV2Command → GeneratePromptUseCase.generate → schema evaluation → metrics → build result. |
| Semantic | **SemanticResolutionService** | By RequestMode: EXTRACTION → fixed axes; SIMPLE/ADVANCED → category required, intent from profile fallback if null, validate, recommend role/action, IntentDictionary for objective/outputNeeds, build **ConfirmedSemanticAxes**. |
| Semantic | **SemanticRecommendationService** | For (category, intent, profile): recommend role/action (user-provided or first from profile), build **RecommendationResult** (candidates + hints). |
| Semantic | **SemanticValidationService** | Validate command vs profile: intent allowed/fit, discouraged tone/style, role/action compatibility, forbidden combinations → VALID / WARNING / INVALID. |
| Generate | **GeneratePromptService** | Implements GeneratePromptUseCase: `PromptSpecFactory.createFromConfirmedAxes(axes, input, jsonSchema)` → Solve → Verify → Repair; returns **GeneratePromptResult**. |

### 1.4 Domain Resolution / Routing / Factory / Spec

- **Resolution:** `SemanticResolutionService` is the only active resolver. Legacy `UnifiedRoutingFacade` (IntentDefaultsResolver → RoutingRuleEngine → DomainFinalizer → OutputContractPlanner) is deprecated and not used by the orchestrator.
- **Factory:** **PromptSpecFactory** — production path is only `createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema)`. Other overloads throw `UnsupportedOperationException`.
- **Spec generation:** `PromptSpec` is built inside the factory from axes + input + jsonSchema; sections (role, checklist, instruction, extra) and constraints/outputContract come from **ObjectiveProfile** (ObjectiveRegistry) and **CategorySemanticProfile**-driven axes.

### 1.5 Where Semantic Decisions Are Made

| Decision | Where | Behavior |
|----------|--------|----------|
| **Intent** | `SemanticResolutionService.resolve()` (SIMPLE/ADVANCED) | If `command.intent() == null` → `profile.getFallbackIntent()`; if still null → fail. Otherwise use command intent. |
| **Role / Action** | `SemanticRecommendationService.recommend()` | User-provided wins; else first of `profile.getRecommendedRolesForIntent(intent)` and `profile.getCompatibleActionsForIntent(intent)`. Result written into **ConfirmedSemanticAxes**. |
| **Objective / outputNeeds** | `SemanticResolutionService` + **IntentDictionary** | `IntentDictionary.getResolutionDefaults(intent)`; overridden to EXTRACTION + JSON_SCHEMA_REQUIRED if `command.jsonSchema()` non-blank. |
| **TaskDomain** | `SemanticResolutionService` | `profile.getBaseTaskDomain()` or `category.getDefaultDomain()`. |
| **Tone / Style** | **UnifiedGeneratePromptCommand** canonical constructor | Defaults: NEUTRAL, NARRATIVE (and language/experience). Passed through in resolution. |

### 1.6 Overloaded Responsibilities

- **UnifiedPromptGenerationOrchestrator:** Resolution + V2 command mapping + use case call + schema evaluation + metrics + result assembly. Single flow; could be split into “resolve” vs “generate” for recommendation-first.
- **SemanticResolutionService:** EXTRACTION branch, SIMPLE/ADVANCED branch, validation, recommendation, IntentDictionary, jsonSchema override, and ConfirmedSemanticAxes construction. Good candidate to expose a “recommend only” path that returns suggested axes + metadata without finalizing.
- **UnifiedGeneratePromptResponse:** Carries both final prompt and resolution metadata; no per-field source (user/recommended/fallback).

---

## 2. UX Risk Analysis

### 2.1 Automatic Fallback Intent

- **What the code does:** In `SemanticResolutionService.resolve()`, for SIMPLE/ADVANCED, when `intent == null`, intent is set to `profile.getFallbackIntent()` and `fallbackIntentUsed = true`. Resolution continues; hints may include “Fallback: …” from IntentDictionary.
- **UX risk:** The user never sees “we suggest intent X” before generation. The API contract (Simple/Advanced request) marks intent as `@NotNull`, but the resolution layer is built to accept null and finalize. Any path that omits intent (e.g. programmatic command) gets silent fallback.
- **Recommendation-first improvement:** Recommendation endpoint returns `recommended_intent` (and optional alternatives) with reason; generation endpoint accepts explicit `intent` (or “use recommended”). No silent fallback in the generation path when recommendation was shown.

### 2.2 Role / Action Auto-Assignment

- **What the code does:** `SemanticRecommendationService` sets `recommendedRole` / `recommendedAction` to user value or first profile candidate. These are written directly into **ConfirmedSemanticAxes** and used by **PromptSpecFactory**.
- **UX risk:** Frontend cannot show “we recommend Role A; you chose Role B” vs “we chose Role A for you” because the response only has `resolved_role`/`resolved_action` and text in `recommendation_hints`.
- **Recommendation-first improvement:** Recommendation response includes `role_candidates`, `action_candidates`, `default_role`, `default_action`, and per-axis `source: USER_PROVIDED | RECOMMENDED`. Confirmation request carries user’s final choices; generation uses only confirmed axes.

### 2.3 Hidden Semantic Resolution

- **What the code does:** Objective and outputNeeds are set from `IntentDictionary.getResolutionDefaults(intent)` (and jsonSchema override). No API field lets the user see or override these before generation.
- **UX risk:** “Objective” and “output needs” are important semantic axes but are invisible in the request and only appear in the response as resolved values. User cannot confirm or override.
- **Recommendation-first improvement:** Recommendation response exposes recommended objective/outputNeeds with short reasons; confirmation step allows override (or “use recommended”). Generation consumes only confirmed values.

### 2.4 Finalized Prompt Spec Before User Confirmation

- **What the code does:** As soon as resolution succeeds, `ConfirmedSemanticAxes` is built and passed to `GeneratePromptUseCase.generate()`. There is no second step where the user explicitly confirms or adjusts axes.
- **UX risk:** All semantic decisions (including fallback and recommendation) are final for that request. “Recommendation” is effectively “default we applied” rather than “suggestion to confirm.”
- **Recommendation-first improvement:** Two-phase flow: (A) recommend → return suggested axes + metadata; (B) confirm (with optional overrides) → generate. Only phase B calls `PromptSpecFactory.createFromConfirmedAxes` with user-confirmed (or explicitly accepted) axes.

### 2.5 Lack of Recommendation Metadata in Response

- **What the code does:** `UnifiedGeneratePromptResponse` has `resolved_*`, `recommendation_hints` (list of strings), `semantic_resolution_summary`, `validation_warnings`, `semantic_profiles_applied`. No structured “for intent, source=RECOMMENDED; for role, source=USER_PROVIDED.”
- **UX risk:** Frontend cannot reliably render “You selected X” vs “We suggested X” or “Fallback applied: X” without parsing hint text. Hard to build UIs that highlight overrides or recommendations.
- **Recommendation-first improvement:** Add structured recommendation metadata: e.g. `axis_sources: { intent: "FALLBACK" | "USER" | "RECOMMENDED", role: "...", action: "..." }` and/or a list of `{ axis, value, source, reason }`. Keep backward compatibility by adding optional fields.

### 2.6 Endpoint Mixes Recommendation and Final Generation

- **What the code does:** Single `POST /prompts/generate` does resolve + generate. No endpoint that only returns recommendations.
- **UX risk:** To show recommendations, the client must either (1) call generate and use response metadata (but prompt is already generated), or (2) duplicate resolution logic on the client. Neither supports “show recommendations → user adjusts → then generate.”
- **Recommendation-first improvement:** Add `POST /prompts/recommend` (or similar) that takes minimal input (category, optional intent, raw input) and returns recommended axes, candidates, reasons, and default selections. Existing `POST /prompts/generate` can later require explicit confirmation payload (or remain “legacy” one-shot with clear docs).

---

## 3. Recommendation-First API Design

### Step A — Recommendation

**Input (minimal):**

- `category` (required for non-EXTRACTION)
- `intent` (optional; if omitted, engine suggests one)
- `raw_input` (optional; for future NL-based suggestions)
- `request_mode`: SIMPLE | ADVANCED | EXTRACTION

**Output:**

- **recommended_intent** + **intent_candidates** (with fit level and short reason); **intent_source**: `USER_PROVIDED` | `PROFILE_FALLBACK` | `RECOMMENDED`.
- **recommended_role** + **role_candidates**; **role_source**: `USER_PROVIDED` | `RECOMMENDED`.
- **recommended_action** + **action_candidates**; **action_source**: `USER_PROVIDED` | `RECOMMENDED`.
- **recommended_objective**, **recommended_output_needs** (and optional alternatives); **source** for each.
- **recommended_tone** / **recommended_style** (optional, from profile or defaults); **source**.
- **default_selection** per axis (what would be used if user confirms without change).
- **reasons** / **recommendation_hints** per axis (short, UI-friendly).
- **validation_warnings** (discouraged combinations, etc.) without blocking.
- **fallback_applied**: boolean or list of axes where fallback was used.

No prompt generation in this step; no LLM call.

### Step B — Confirmation

**Input:**

- Either a **recommendation_id** (from Step A) plus optional overrides, or full **confirmed axes** (intent, role, action, tone, style, objective, output_needs as needed).
- **input** (user text), **json_schema** if EXTRACTION/structured.
- Metadata: title, description, tags.

**Output:**

- Same as current **UnifiedGeneratePromptResponse** (output, resolved axes, badges, verify/repair/schema, metadata), with the guarantee that resolved axes equal confirmed axes (or explicitly accepted recommendations).

**Rule:** Final generation uses only axes that the user has confirmed (explicitly or by accepting defaults). No silent fallback inside the generation step when confirmation is explicit.

---

## 4. Controller Refactor Proposal

### 4.1 Current vs Proposed

- **Current:** One generation controller (`UnifiedPromptEngineController`) with one heavy endpoint; one CRUD controller (`PromptController`).
- **Proposed:** Split generation into recommendation vs confirmed generation; keep CRUD separate; optional metadata/options controller.

### 4.2 RecommendationController (new)

- **Responsibility:** Expose recommendation-only flow. No prompt generation, no LLM.
- **Why:** Enables “recommend first, confirm later” UX and clear separation of recommendation from generation.
- **Endpoint examples:**
  - `POST /prompts/recommend` — body: category, optional intent, optional request_mode, optional raw_input. Returns recommendation response DTO (see §5).
- **Request/response:** RecommendRequest (minimal), RecommendResponse (recommended axes, candidates, sources, reasons).
- **Use case:** New **RecommendPromptAxesUseCase** that calls SemanticResolutionService in “recommend only” mode (or a dedicated method that returns RecommendationResult + intent/objective/outputNeeds suggestions without building ConfirmedSemanticAxes for generation).

### 4.3 PromptGenerationController (rename/refactor of UnifiedPromptEngineController)

- **Responsibility:** Final prompt generation from confirmed input.
- **Why:** Single place for “generate from axes”; can later require confirmation payload.
- **Endpoint examples:**
  - `POST /prompts/generate` — (current) one-shot: body = UnifiedGeneratePromptRequest; behavior remains resolve + generate for backward compatibility.
  - Optional later: `POST /prompts/generate/confirmed` — body = ConfirmedAxesRequest (explicit axes + input + options); no resolution step, only generation. Use when client got recommendations and sent back confirmed choices.
- **Request/response:** Existing UnifiedGeneratePromptRequest/Response for one-shot; new ConfirmedAxesRequest and same UnifiedGeneratePromptResponse for confirmed path.
- **Use case:** GenerateUnifiedPromptUseCase (one-shot), and optionally GeneratePromptFromConfirmedAxesUseCase (confirmed path calling GeneratePromptUseCase with pre-confirmed axes).

### 4.4 PromptMetadataController (optional)

- **Responsibility:** List available options for dropdowns (categories, intents per category, roles/actions per intent, tone/style).
- **Why:** Frontend can build selection UIs without hardcoding enums.
- **Endpoint examples:**
  - `GET /prompts/options/categories` — list PromptCategory with labels.
  - `GET /prompts/options/intents?category=RESEARCH` — allowed intents for category, with fit level and fallback hint.
  - `GET /prompts/options/roles?category=RESEARCH&intent=ANALYZE` — role candidates.
  - `GET /prompts/options/actions?category=RESEARCH&intent=ANALYZE` — action candidates.
- **Use case:** ListAvailableOptionsUseCase (reads from CategorySemanticProfileRegistry, IntentDictionary, etc.).

### 4.5 PromptController (existing)

- **Responsibility:** CRUD only (list, get, update, delete). No change.
- **Why:** Separation of “manage saved prompts” from “generate/recommend.”

---

## 5. DTO Refactor Proposal

### 5.1 Recommendation Request

- **RecommendRequest** (new): `category` (optional for EXTRACTION), `intent` (optional), `request_mode` (SIMPLE | ADVANCED | EXTRACTION), `raw_input` (optional), optional `role_type`/`action_type` for “validate and recommend rest.”

### 5.2 Recommendation Response

- **RecommendResponse** (new): Structured per-axis recommendation.
  - **intent:** `recommended`, `candidates` (value + fit + reason), `source` (USER_PROVIDED | PROFILE_FALLBACK | RECOMMENDED).
  - **role:** `recommended`, `candidates`, `source` (USER_PROVIDED | RECOMMENDED).
  - **action:** same.
  - **objective / output_needs:** recommended + source.
  - **tone / style:** optional recommended + source.
  - **default_selection:** snapshot of “what we would use if you confirm as-is.”
  - **reasons** / **recommendation_hints**, **validation_warnings**, **fallback_applied** (list of axis names).

Design so the frontend can show “Recommended: X (reason)” and “You selected: Y” and send back only overrides in confirmation.

### 5.3 Confirmed Generation Request

- **ConfirmedGeneratePromptRequest** (new, for optional confirmed flow): Explicit axes — `intent`, `role_type`, `action_type`, `tone`, `style`, `objective`, `output_needs` (as needed); plus `input`, `json_schema`, title, description, tags. No category→intent resolution; all axes are final.
- **Existing UnifiedGeneratePromptRequest** remains for one-shot flow; document that it performs resolution and that some axes may be recommended/fallback.

### 5.4 Final Prompt Response

- **UnifiedGeneratePromptResponse** (extend): Add optional **axis_sources** (map or list of `{ axis, value, source }`) and optional **recommendation_id** when generation was preceded by recommend. Keep all existing fields so current clients stay valid.

### 5.5 Separation of Concerns

- **RecommendRequest / RecommendResponse:** Only for recommendation step; no `output`, no quality badges.
- **UnifiedGeneratePromptRequest / UnifiedGeneratePromptResponse:** One-shot resolve+generate; add optional recommendation metadata.
- **ConfirmedGeneratePromptRequest:** Only for confirmed flow; backend does not resolve semantics, only generates from provided axes.

---

## 6. Application Service / Use Case Refactor

### 6.1 New Use Cases

- **RecommendPromptAxesUseCase:** Input: minimal (category, optional intent, request_mode, optional raw_input). Output: recommendation DTO (suggested axes, candidates, sources, reasons). Implementation: call SemanticResolutionService in a way that returns suggestions without building “final” ConfirmedSemanticAxes for generation, or add `resolveForRecommendation()` that returns a recommendation view (intent/role/action/objective/outputNeeds + sources + hints). No call to GeneratePromptUseCase.
- **GeneratePromptFromConfirmedAxesUseCase** (optional): Input: confirmed axes + input + options. Builds ConfirmedSemanticAxes from request, calls GeneratePromptUseCase.generate(v2Command, axes). No SemanticResolutionService.
- **ListAvailableOptionsUseCase:** Input: category, optional intent. Output: allowed intents, role candidates, action candidates, etc. Implementation: read from CategorySemanticProfileRegistry, IntentDictionary; return DTOs for dropdowns.

### 6.2 What Stays in Domain

- **PromptSpecFactory.createFromConfirmedAxes:** Single entry point for building PromptSpec from axes. No change to signature; only callers may pass “recommendation-confirmed” axes.
- **CategorySemanticProfile**, **IntentDictionary**, **ConfirmedSemanticAxes**, **RecommendationResult:** Keep; extend ConfirmedSemanticAxes or add a “RecommendationView” if we need to expose per-axis source in domain.

### 6.3 What Moves to Application

- **Recommendation-only flow:** New use case and possibly a method on SemanticResolutionService that returns “recommendation” (suggested axes + candidates + sources) without committing to ConfirmedSemanticAxes for generation. Validation and recommendation services stay; only the “build axes and then generate” coupling is split.
- **Explicit “confirmed” generation path:** Application builds ConfirmedSemanticAxes from ConfirmedGeneratePromptRequest and calls GeneratePromptUseCase; no resolution step.

### 6.4 What Stays Out of Controller

- Resolution logic, validation, recommendation logic: remain in application/domain services. Controllers only map request → command/query and call use cases.
- Building of PromptSpec: remains in PromptSpecFactory; controller never touches it.

### 6.5 What Should No Longer Happen Automatically in Deep Factories

- **PromptSpecFactory:** Already clean — it only builds from ConfirmedSemanticAxes. No change.
- **SemanticResolutionService:** Should not be the only path to “final” axes when recommendation-first is used. In the confirmed flow, axes come from the user’s confirmation payload; resolution is used only in recommend step or in legacy one-shot generate.

---

## 7. Domain Model Refactor Implications

### 7.1 Current Semantic Order

Effectively: **PromptCategory** → **ActionIntent** → (RoleType, ActionType) → ToneType / StyleType → OutputContract (objective, outputNeeds, jsonSchema) → **PromptSpec**. This is already respected: resolution builds ConfirmedSemanticAxes in that order; PromptSpecFactory consumes axes only.

### 7.2 Recommendation Candidates

- **Intent:** Profile fallback + allowed intents with fit level (PREFERRED / DISCOURAGED etc.) → candidates.
- **Role / Action:** From CategorySemanticProfile.getRecommendedRolesForIntent / getCompatibleActionsForIntent → candidates.
- **Objective / outputNeeds:** From IntentDictionary.getResolutionDefaults(intent) → recommended; optional override when jsonSchema present.
- **Tone / Style:** Profile discouraged lists + command defaults → recommended default; optional alternatives.

All of these can be exposed as “recommendation” with a clear default and optional list of alternatives; no need to change domain model, only the way resolution result is returned (recommendation view vs confirmed axes).

### 7.3 What Should Be Finalized Only After Explicit Confirmation

- For recommendation-first flow: intent, role, action, objective, output_needs, and optionally tone/style. Finalization = “user confirmed (or accepted default).”
- ConfirmedSemanticAxes should be built from confirmed values only when implementing the confirmed-generation path. In one-shot flow, current behavior can remain but should be documented as “engine may apply fallbacks and recommendations.”

### 7.4 PromptSpecFactory.createFromConfirmedAxes as Only Final Composition Entry Point

- **Conclusion:** Keep it as the only production entry point. No new factory method that infers semantics from category or raw input. For confirmed flow, the application layer builds ConfirmedSemanticAxes from the confirmation request and calls the factory. Domain stays agnostic to “user confirmed” vs “engine resolved”; it only receives axes.

---

## 8. Concrete Refactor Plan

### Phase 1: Add Recommendation Metadata Without Breaking Old API

- **Scope:** Response and port model only.
- **Tasks:** Add optional `axis_sources` (e.g. `Map<String, String>` axis → USER_PROVIDED | RECOMMENDED | FALLBACK) to **UnifiedGeneratePromptResult** and **UnifiedGeneratePromptResponse**. In SemanticResolutionService (and orchestrator), populate these from resolution (intent: fallbackIntentUsed ? FALLBACK : USER_PROVIDED; role/action: from RecommendationResult — user provided vs recommended). Keep existing fields; new fields optional.
- **Benefit:** Frontend can show “you chose / we suggested” without new endpoints.
- **Migration risk:** Low; additive only.
- **Compatibility:** Old clients ignore new fields.

### Phase 2: Separate Recommendation Endpoint

- **Scope:** New use case, controller, DTOs.
- **Tasks:** Implement **RecommendPromptAxesUseCase** (delegate to SemanticResolutionService.recommendOnly or new method returning recommendation view). Add **RecommendRequest** / **RecommendResponse** DTOs. Add **RecommendationController** with `POST /prompts/recommend`. No generation, no LLM.
- **Benefit:** Enables two-phase UX: recommend → show UI → confirm → generate.
- **Migration risk:** Low; additive. Existing generate unchanged.
- **Compatibility:** No change to existing endpoints.

### Phase 3: Reduce Controller Responsibility

- **Scope:** Split generation controller if desired; optional options endpoint.
- **Tasks:** Rename or group: e.g. keep UnifiedPromptEngineController for one-shot generate; add RecommendationController (Phase 2). Optionally add PromptOptionsController with ListAvailableOptionsUseCase for GET options.
- **Benefit:** Clear API surface per concern.
- **Migration risk:** Low if only adding; medium if renaming URLs (then need redirects or versioning).
- **Compatibility:** Preserve existing `/prompts/generate` URL and behavior.

### Phase 4: Move Hidden Semantic Finalization Out of One-Shot Path (Optional)

- **Scope:** SemanticResolutionService and one-shot flow.
- **Tasks:** Where possible, make one-shot generate use “recommendation” semantics internally and then treat “no overrides” as “use recommended as confirmed.” Alternatively, document current one-shot as “legacy: may apply fallbacks” and encourage clients to use recommend + confirmed generate for full control.
- **Benefit:** Clearer mental model: resolution = recommendation; generation = apply confirmed.
- **Migration risk:** Medium if behavior of one-shot changes; avoid changing behavior in Phase 4, only document or add a query param “require_explicit_intent” that fails when intent would be fallback.
- **Compatibility:** Default one-shot behavior unchanged unless explicitly opted.

### Phase 5: Make Final Generation Depend Only on Confirmed Axes

- **Scope:** New confirmed-generation endpoint and use case.
- **Tasks:** Add **ConfirmedGeneratePromptRequest** and **GeneratePromptFromConfirmedAxesUseCase**. New endpoint (e.g. `POST /prompts/generate/confirmed`) that builds ConfirmedSemanticAxes from request and calls GeneratePromptUseCase. No resolution step.
- **Benefit:** Full “recommend → confirm → generate” flow with no silent fallback in the confirmed step.
- **Migration risk:** Low for new endpoint; clients adopt when ready.
- **Compatibility:** Existing generate remains; new endpoint for confirmation-based flow.

---

## 9. Final Recommendation

### 9.1 Is recommendation-first UX better than automatic confirmation for this engine?

**Yes.** The engine already has rich recommendation and validation (profiles, IntentDictionary, SemanticRecommendationService, SemanticValidationService). Exposing that as a clear “recommend first” step and making final generation depend on confirmed axes improves transparency, control, and trust. Automatic confirmation is acceptable for a “quick generate” path, but it should be explicit and documented; the primary flow should be recommendation-first where possible.

### 9.2 Should the controller layer be split to reflect that?

**Yes.** At least: (1) **RecommendationController** for `POST /prompts/recommend`, (2) **PromptGenerationController** (current unified + optional confirmed endpoint). Optional: **PromptOptionsController** for listing categories/intents/roles/actions. This matches the UX flow and keeps each controller focused.

### 9.3 Which exact refactor is most important first?

**Phase 1 (axis_sources in response) + Phase 2 (recommendation endpoint).** Phase 1 gives immediate value to the frontend without new flows. Phase 2 enables the two-phase UX and sets the pattern for confirmation-based generation. Together they deliver “engine recommends first, user confirms finally” without breaking existing clients.

### 9.4 What should remain automatic, and what should become user-confirmable?

- **Remain automatic (or default-only):** Tone/style/language/experience when not provided (defaults in command); taskDomain from profile/category; engine mode/profile for quality pipeline. These can stay as defaults as long as they are visible in recommendation/response.
- **Become user-confirmable (or explicitly “use recommended”):** Intent (no silent profile fallback in confirmed path), role, action, objective, output_needs. In the recommendation step, engine suggests; in the confirmation step, user sends back chosen values (or “use recommended”). Generation then uses only those.
- **Special:** EXTRACTION mode can remain one-shot (intent=EXTRACT, category=ETC fixed) with no recommendation step, or optionally still return a minimal “recommendation” that states the fixed axes so the response shape is consistent.

---

*Document generated from analysis of `src/main/java/org/example/sharedprompts/domain/prompt`. Class and package names refer to the current codebase.*
