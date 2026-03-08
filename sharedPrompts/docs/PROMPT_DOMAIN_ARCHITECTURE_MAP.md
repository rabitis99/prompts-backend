# Prompt Domain — Architecture Map

Structured map of `src/main/java/org/example/sharedprompts/domain/prompt` for architecture analysis. All paths are relative to the prompt domain root unless noted.

---

## 1. Package and Key Classes Overview

### 1.1 Adapter In (Web)

| Package | Key classes | Role |
|---------|-------------|------|
| `adapter.in.web.controller` | **UnifiedPromptEngineController**, **PromptController** | HTTP entry points |
| `adapter.in.web.dto.request` | **UnifiedGeneratePromptRequest** (sealed), **SimpleGeneratePromptRequest**, **AdvancedGeneratePromptRequest**, **ExtractionGeneratePromptRequest** | Request DTOs; polymorphic by `request_type` |
| `adapter.in.web.dto.response` | **UnifiedGeneratePromptResponse**, **BadgeDto** | Response DTOs |
| `adapter.in.web.mapper` | **UnifiedPromptResponseMapper**, **PromptWebMapper** | Request→Command, Result→Response mapping |
| `adapter.in.web.exception` | **PromptExceptionHandler** | `@RestControllerAdvice` for prompt domain |

### 1.2 Adapter Out

| Package | Key classes | Role |
|---------|-------------|------|
| `adapter.out.render` | **PromptSpecRendererAdapter** | PromptSpec → meta-prompt string |
| `adapter.out.render.repair` | **RepairPromptRenderer** | Repair meta-prompt for verify-fail path |
| `adapter.out.render.section` | **RoleContextRenderer**, ObjectiveSectionRenderer, StrategySectionRenderer, ConstraintsSectionRenderer, OutputContractRenderer, UserInputRenderer | Section-level rendering |
| `adapter.out.persistence` | **SavePromptVersionAdapter**, **PromptPersistenceAdapter** | Save prompt version / CRUD |

### 1.3 Application — Ports (In)

| Package | Key classes | Role |
|---------|-------------|------|
| `application.port.in` | **GenerateUnifiedPromptUseCase**, **GeneratePromptUseCase** | Unified and V2 use-case ports |
| `application.port.in.command` | **UnifiedGeneratePromptCommand**, **GeneratePromptCommand**, UpdatePromptCommand, DeletePromptCommand | Commands |
| `application.port.in.command.normalization` | **SemanticSelection**, **ExpressionOptions**, **OutputOptions** | Normalized request groupings for command building |
| `application.port.in.query` | **UnifiedGeneratePromptResult**, **GeneratePromptResult**, SearchPromptsQuery, PromptPageResult, PromptDetailView, PromptSummaryView | Query results / DTOs |

### 1.4 Application — Ports (Out)

| Package | Key classes | Role |
|---------|-------------|------|
| `application.port.out.llm` | **LLMClientPort**, ConstrainedDecodingPort | LLM solve/repair |
| `application.port.out.render` | **PromptSpecRendererPort** | Render spec to string |
| `application.port.out.persistence` | **SavePromptVersionPort**, **PromptCommandPort**, **PromptQueryPort**, PromptSearchQuery | Persistence |
| `application.port.out.identity` | ValidateUserPort | User validation |

### 1.5 Application — Services

| Package | Key classes | Role |
|---------|-------------|------|
| `application.service.orchestration` | **UnifiedPromptGenerationOrchestrator**, **SchemaContractEvaluator**, DomainResolutionService, PromptServiceImpl, PromptUpdateValidator, PromptNotificationService | Orchestration, schema evaluation, CRUD |
| `application.service.orchestration.legacy` | **UnifiedRoutingFacade**, **IntentDefaultsResolver**, **RoutingRuleEngine**, **DomainFinalizer**, **OutputContractPlanner**, **IntentDefaults**, **FinalDomainDecision**, **ContractDecision**, **UnifiedRoutingPolicy** | Deprecated routing chain; not used by active pipeline |
| `application.service.semantic` | **SemanticResolutionService**, **SemanticRecommendationService**, **SemanticValidationService** | Category-aware resolution, recommendation, validation |
| `application.service.generate` | **GeneratePromptService** | Implements GeneratePromptUseCase; 4-step pipeline (Solve → Verify → Repair) |
| `application.service.guideline` | **AbstractGuidelineRenderer**, **KoreanGuidelineRenderer**, **EnglishGuidelineRenderer**, **JapaneseGuidelineRenderer** | Locale-specific guideline text |
| `application.service.descriptor` | **DefaultRoleDescriptorProvider** | Implements RoleDescriptorPort for role names/descriptions |

### 1.6 Application — Exception

| Package | Key classes | Role |
|---------|-------------|------|
| `application.exception` | **SemanticResolutionException**, PromptAccessDeniedException, UnsupportedQualityPipelineOptionException | Domain exceptions |

### 1.7 Domain

| Package | Key classes | Role |
|---------|-------------|------|
| `domain.model.spec` | **PromptSpec**, PromptSection, Constraints, ContentSandbox | Spec model |
| `domain.model.contract` | OutputContract | Output contract (e.g. JSON schema) |
| `domain.model.result` | VerifyResult, QualityRubric | Verify phase result |
| `domain.service.spec` | **PromptSpecFactory**, PromptSpecValidator | Build and validate PromptSpec |
| `domain.service.badge` | BadgeResolver | Map verify/repair outcome to QualityBadge |
| `domain.resolutions` | **ObjectiveResolver**, DomainResolverPort, ResolvedDomain, ResolutionSource | Objective/domain resolution (used by legacy and config) |
| `domain.descriptor` | **RoleDescriptorPort** | Role name/description by locale |
| `domain.semantic` | **ConfirmedSemanticAxes**, **CategorySemanticProfile**, **CategorySemanticProfileRegistry**, **IntentDictionary**, IntentDefinition, IntentResolutionDefaults, RecommendationResult, SemanticValidationResult, SemanticFitLevel, FallbackCandidate, SemanticWarning, etc. | Semantic axes, profiles, intent dictionary |
| `domain.semantic.impl` | DefaultCategorySemanticProfile, DefaultCategorySemanticProfileRegistry | Default profile implementations |
| `domain.objective` | ObjectiveProfile, ObjectiveRegistry | Objective-driven spec building |
| `domain.value.objective` | PromptObjective (domain) | Domain objective value |
| `entity` | **Prompt** | JPA entity (persistence) |

### 1.8 Common (Enums / Shared)

| Package | Key classes | Role |
|---------|-------------|------|
| `common.enums` | **PromptCategory**, **ActionIntent**, **PromptObjective** (API), **ToneType**, **StyleType**, **RequestMode**, **RequestType**, **EngineMode**, **EngineProfile**, **OutputNeeds**, **ExperienceLevel**, **LanguageType**, **TaskDomain**, ResponseShape, **ToneStyleNormalizer** | Shared enums and normalizers |
| `common.enums.action` | **ActionTypeInterface** + implementations | Action types |
| `common.enums.role` | **RoleTypeInterface** + (e.g. CybersecurityRoleType, EducationRoleType, EtcRoleType, HealthFitnessRoleType, ResearchRoleType, CoreRoleType, DomainRoleType) | Role types |
| `common.guideline` | GuidelineBundle, GuidelineBundleBuilder, RuleContext, GuidelineRule | Guideline rules and bundles |

### 1.9 Infrastructure

| Package | Key classes | Role |
|---------|-------------|------|
| `infrastructure.config` | **PromptDomainConfig**, **ResolutionConfig** | Bean wiring, resolution config |
| `infrastructure.persistence` | CustomPromptRepositoryImpl | Repository implementation |

---

## 2. Flow Tracing: Unified Prompt Generation

### 2.1 HTTP entry point

- **Controller:** `adapter.in.web.controller.UnifiedPromptEngineController`
- **Endpoint:** `POST /prompts/generate`
- **Request body:** Polymorphic `UnifiedGeneratePromptRequest` (discriminator `request_type`: SIMPLE | EXTRACTION | ADVANCED).
- **Auth:** `@CurrentUser AuthUser`; null → `ApiException(ErrorCode.UNAUTHORIZED)`.
- **Async:** `WebAsyncTask` (timeout 60s); on timeout/error returns fail response.

### 2.2 Request DTO → Command mapping

- **Mapping:** Each request type implements `UnifiedGeneratePromptRequest.toCommand(Long userId)`:
  - **SimpleGeneratePromptRequest** (`request_type=SIMPLE`): builds `SemanticSelection(category, intent, null, null)`, `ExpressionOptions(tone, style, language, experience)`, `OutputOptions(null, null)` → `UnifiedGeneratePromptCommand.fromNormalized(..., RequestMode.SIMPLE, ...)`.
  - **AdvancedGeneratePromptRequest** (`request_type=ADVANCED`): includes optional `roleType`, `actionType`, `jsonSchema`, `engineMode`, `disableQualityPipeline` → `SemanticSelection(category, intent, roleType, actionType)`, `OutputOptions(jsonSchema, engineMode)` → `UnifiedGeneratePromptCommand.fromNormalized(..., RequestMode.ADVANCED, ...)`.
  - **ExtractionGeneratePromptRequest** (`request_type=EXTRACTION`): no category/intent; `SemanticSelection=null`, `OutputOptions(jsonSchema, null)` → `UnifiedGeneratePromptCommand.fromNormalized(..., RequestMode.EXTRACTION, ...)`.
- **Command defaults (in record canonical constructor):** `engineMode` → AUTO if null; `tone` → NEUTRAL; `style` → NARRATIVE; `language` → KOREAN; `experience` → INTERMEDIATE; `tags` copy; `disableQualityPipeline=true` → throws `UnsupportedQualityPipelineOptionException`. Category and intent are **not** defaulted in the command; resolution layer handles them.

### 2.3 Use case / orchestrator invoked

- **Port:** `GenerateUnifiedPromptUseCase` (`application.port.in.GenerateUnifiedPromptUseCase`).
- **Implementation:** `UnifiedPromptGenerationOrchestrator` (`application.service.orchestration.UnifiedPromptGenerationOrchestrator`).
- **Flow inside orchestrator:**
  1. **SemanticResolutionService.resolve(command)** → `Result(success, axes, errors)`. On `!success` → throw `SemanticResolutionException(errors)`.
  2. **toV2Command(command, axes)** → build `GeneratePromptCommand` from command + `ConfirmedSemanticAxes` (title/description fallback from axes.intent if blank).
  3. **GeneratePromptUseCase.generate(v2Command, axes)** → `GeneratePromptResult` (see below).
  4. **SchemaContractEvaluator.evaluate(v2Result)** → schema contract failure flags.
  5. **Metrics** (PromptEngineMetrics): record success/failure, latency, repair count, finallyPassed, schemaContractFailed.
  6. Build and return **UnifiedGeneratePromptResult** (output, engine mode, resolved axes, badges, verify/repair/schema flags, profile IDs, warnings, recommendation hints, summary).

### 2.4 Where intent / category / role / action / tone / style / output are resolved or defaulted

- **SemanticResolutionService** (`application.service.semantic.SemanticResolutionService`):
  - **RequestMode.EXTRACTION:** intent=EXTRACT, category=ETC, taskDomain=ANALYTICAL, objective=EXTRACTION, outputNeeds=JSON_SCHEMA_REQUIRED; role/actionType=null. No profile lookup.
  - **SIMPLE/ADVANCED:**  
    - **Category:** required; null → `Result.fail("category is required for SIMPLE/ADVANCED")`.  
    - **Intent:** if null, taken from **CategorySemanticProfile.getFallbackIntent()** (profile from **CategorySemanticProfileRegistry**); if still null → fail.  
    - **Validation:** **SemanticValidationService.validate(command, profile, intent)** → invalid → fail; warnings collected.  
    - **Recommendation:** **SemanticRecommendationService.recommend(category, intent, profile, command.roleType(), command.actionType(), fallbackIntentUsed)** → recommended role/action and hints.  
    - **Objective / outputNeeds:** from **IntentDictionary.getResolutionDefaults(intent)** (defaultObjective, preferredOutputNeeds). If command has non-blank **jsonSchema** → objective=EXTRACTION, outputNeeds=JSON_SCHEMA_REQUIRED.  
    - **TaskDomain:** from profile.getBaseTaskDomain() or category.getDefaultDomain().  
    - **Tone / style / language / experience:** passed through from command (no resolution).  
  - **ConfirmedSemanticAxes** built with: category, taskDomain, intent, objective, outputNeeds, recommended role/action, tone, style, language, experienceLevel, appliedProfileIds, validationWarnings, recommendationHints.

- **Defaults / fallbacks:**
  - **Intent:** SIMPLE/ADVANCED only: profile fallback when intent is null (AUTO behavior for intent).
  - **Role / action:** Recommended from profile for (category, intent) when not provided; user overrides (ADVANCED) validated by SemanticValidationService.
  - **Tone / style:** Defaulted in **UnifiedGeneratePromptCommand** (NEUTRAL, NARRATIVE); not re-defaulted in resolution.
  - **Objective / outputNeeds:** From IntentDictionary; overridden to EXTRACTION + JSON_SCHEMA_REQUIRED when jsonSchema present.

### 2.5 Where PromptSpec or final prompt is created

- **GeneratePromptService** (`application.service.generate.GeneratePromptService`) implements **GeneratePromptUseCase**:
  - **PromptSpec creation:** `PromptSpecFactory.createFromConfirmedAxes(axes, command.input(), command.jsonSchema())` only. No other create path is supported in production (other overloads throw UnsupportedOperationException).
- **PromptSpecFactory** (`domain.service.spec.PromptSpecFactory`):
  - **createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema):**  
    - Gets **ObjectiveProfile** from **ObjectiveRegistry** by axes.objective(); builds **Constraints**, **OutputContract** from profile; builds **RuleContext** (taskDomain, objective name, actionType, supportsConstrainedDecoding, hasJsonSchema, rawInput).  
    - **buildSections:** (1) Role section if axes.role present (via **RoleDescriptorPort** for name/description by locale), (2) Checklist from **GuidelineBundleBuilder** (hard/soft rules), (3) Instruction from profile, (4) profile.extraSections().  
    - Builds **PromptSpec** with objective, priority, rubric, taskDomain, experienceLevel, sections, constraints, outputContract, contentSandbox, role, tone, style, strategyBundle (from **StrategyBundlePolicy**), locale, rawInput, actionType.
- **Final prompt string:** Built in **GeneratePromptService** by calling **LLMClientPort.solve(spec)**. The port’s adapter uses **PromptSpecRendererPort** (implemented by **PromptSpecRendererAdapter**) to turn **PromptSpec** into the meta-prompt string (ObjectiveSectionRenderer + StrategySectionRenderer + ConstraintsSectionRenderer + RoleContextRenderer + OutputContractRenderer + UserInputRenderer). So: **PromptSpec** is the “final spec”; the “final prompt” is the rendered string sent to the LLM.

### 2.6 Response DTO and what it exposes

- **UnifiedGeneratePromptResult** (port query model) is built in the orchestrator and mapped to **UnifiedGeneratePromptResponse** by **UnifiedPromptResponseMapper.toResponse(result)**.
- **UnifiedGeneratePromptResponse** (`adapter.in.web.dto.response.UnifiedGeneratePromptResponse`) exposes:
  - **output** — generated prompt text.
  - **requested_engine_mode**, **effective_engine_mode**, **engine_profile**.
  - **resolved_category**, **resolved_domain**, **objective**, **output_needs**, **resolved_intent**, **variant**, **resolved_role**, **resolved_action**.
  - **quality_badges** (list of BadgeDto: code, display_name).
  - **verify_passed**, **repair_count**, **finally_passed**.
  - **schema_contract_failed**, **schema_failure_reasons**.
  - **semantic_profiles_applied**, **validation_warnings**, **recommendation_hints**, **semantic_resolution_summary**.

So the response includes both the **final result** (output, badges, verify/repair/schema flags) and **recommendation/validation metadata** (profiles applied, warnings, recommendation hints, summary).

---

## 3. Routing, Resolution, Defaults, Finalizer, Planner — Summary

### 3.1 Active pipeline (semantic resolution)

- **SemanticResolutionService** — Central resolver. Dispatches by **RequestMode** (EXTRACTION vs SIMPLE/ADVANCED); for SIMPLE/ADVANCED: category required, intent from profile fallback if null, then **SemanticValidationService** and **SemanticRecommendationService**; builds **ConfirmedSemanticAxes**.
- **SemanticRecommendationService** — Recommends role/action from **CategorySemanticProfile** for (category, intent); builds recommendation hints (fallback intent used, fit level, role/action rationale).
- **SemanticValidationService** — Validates command against profile: intent allowed/fit (FORBIDDEN/DISCOURAGED), discouraged tone/style, role/action compatibility, forbidden combinations; returns **SemanticValidationResult** (valid / warning / invalid).
- **IntentDictionary** — Canonical intent definitions and **IntentResolutionDefaults** (defaultObjective, preferredOutputNeeds, defaultResponseShape) per **ActionIntent**. Used by SemanticResolutionService for objective/outputNeeds (and by legacy for defaults).
- **CategorySemanticProfileRegistry** / **CategorySemanticProfile** — Per-category allowed intents, fit levels, recommended roles/actions, discouraged tone/style, fallback intent. Drives validation and recommendation.

No “finalizer” or “planner” in the active path; resolution is profile → validate → recommend → build ConfirmedSemanticAxes.

### 3.2 Legacy (not used by active pipeline)

- **UnifiedRoutingFacade** — Orchestrates: **IntentDefaultsResolver** → **RoutingRuleEngine** → **DomainFinalizer** → **OutputContractPlanner**; produces **RoutingDecision** (objective, outputNeeds, finalDomain, roles, engine profile/mode, appliedRuleIds, reasons). **@Deprecated**; orchestrator uses only SemanticResolutionService.
- **IntentDefaultsResolver** — Resolves **IntentDefaults** (intent, objective, outputNeeds, responseShape, domainAffinity, recommendedEngineProfile) from command using **IntentDictionary.getResolutionDefaults(intent)**; intent null → GENERATE.
- **RoutingRuleEngine** — In-memory **RoutingRule** list; **apply(command, IntentDefaults)** returns **RoutingOverrides** (objective, outputNeeds, domain, engine profile, role overrides, appliedRuleIds). Priority + specificity + order.
- **DomainFinalizer** — **finalizeDomain(command, IntentDefaults, RoutingOverrides)**: base domain from category/defaults, then overrides; **DomainResolutionService.resolveForUnified(category, baseDomain)** → **FinalDomainDecision(finalDomain, reasons)**.
- **OutputContractPlanner** — **plan(command, currentObjective, currentOutputNeeds)**: if command has jsonSchema → objective=EXTRACTION, outputNeeds=JSON_SCHEMA_REQUIRED; returns **ContractDecision(objective, outputNeeds, schemaRequired, reasons)**.
- **UnifiedRoutingPolicy** — Thin wrapper delegating to UnifiedRoutingFacade; **@Deprecated**.

### 3.3 Factory / builder for PromptSpec

- **PromptSpecFactory** (`domain.service.spec.PromptSpecFactory`) is the only production path:
  - **createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema)** builds **PromptSpec** from axes + input + optional schema. All semantic meaning (category, intent, role, action, tone, style, objective, outputNeeds) comes from **ConfirmedSemanticAxes**; no inference from category alone in the factory.
- **PromptSpec** is built via **PromptSpec.builder()** inside the factory (sections, constraints, outputContract, strategyBundle, etc.).

### 3.4 AUTO / fallback behavior

- **Engine mode:** Command defaults **engineMode** to **EngineMode.AUTO**; orchestrator does not switch engine by AUTO (effective path is V2/quality pipeline); requested vs effective mode and profile are recorded in result/response.
- **Intent:** For SIMPLE/ADVANCED, when **intent is null**, **CategorySemanticProfile.getFallbackIntent()** is used; if null, resolution fails. So AUTO is “profile fallback intent” only when intent omitted.
- **Role / action:** When not provided, **SemanticRecommendationService** recommends from profile (first of recommended roles/compatible actions); no separate “AUTO” enum, just recommendation.
- **Tone / style:** Defaulted in command (NEUTRAL, NARRATIVE); no further AUTO logic in resolution.
- **Domain:** In legacy **DomainFinalizer**, base domain from category or intent affinity; in active path **TaskDomain** comes from profile.getBaseTaskDomain() or category.getDefaultDomain().

### 3.5 Response: recommendation metadata vs final result

- **UnifiedGeneratePromptResponse** includes both:
  - **Final result:** `output`, `quality_badges`, `verify_passed`, `repair_count`, `finally_passed`, `schema_contract_failed`, `schema_failure_reasons`, and resolved axes (`resolved_category`, `resolved_domain`, `objective`, `output_needs`, `resolved_intent`, `resolved_role`, `resolved_action`).
  - **Recommendation/audit metadata:** `semantic_profiles_applied`, `validation_warnings`, `recommendation_hints`, `semantic_resolution_summary`, plus engine mode/profile fields.

So the API exposes both the generated prompt and rich metadata for transparency and UX (e.g. “why this intent/role”, “consider a preferred intent”).

---

## 4. Overloaded Classes and Semantic Finalization Without Confirmation

### 4.1 Potentially overloaded or broad responsibilities

- **UnifiedPromptGenerationOrchestrator** — Coordinates resolution, V2 command mapping, use case call, schema evaluation, metrics, and result assembly. Single place for the unified flow; could be split into a “resolution step” and a “generation step” if needed for clarity or reuse.
- **SemanticResolutionService** — Handles EXTRACTION branch, SIMPLE/ADVANCED branch, validation, recommendation, IntentDictionary defaults, jsonSchema override, and ConfirmedSemanticAxes building. Resolution logic is all in one service; validation and recommendation are delegated but the “flow” is here.
- **PromptSpecFactory** — Builds sections (role, guideline checklist, instruction, extra), constraints, output contract, strategy bundle, and full spec. Large but single responsibility “build spec from axes”; section building is private and could be extracted to a SectionBuilder if desired.
- **UnifiedGeneratePromptCommand** — Record with many fields and multiple static factories (fromNormalized, of, forSimple, forExtraction). Reasonable for a command; normalization types (SemanticSelection, ExpressionOptions, OutputOptions) keep the constructor from being a flat bag.

### 4.2 Semantic decisions finalized without user confirmation

- **Intent:** If the user omits intent (SIMPLE/ADVANCED), **profile.getFallbackIntent()** is applied and execution continues. The response includes `recommendation_hints` and `semantic_resolution_summary` (and optionally “Fallback: …” in hints), but there is no separate “confirm intent” step; the resolved intent is final for that request.
- **Role / action:** When the user does not send role_type/action_type, **SemanticRecommendationService** recommends from profile and those values are set in **ConfirmedSemanticAxes** and used for spec generation. Again, response includes `recommendation_hints` and resolved_role/resolved_action, but no explicit confirmation; the recommended values are final.
- **Objective / outputNeeds:** From **IntentDictionary** (and jsonSchema override). No user confirmation step; they are fixed for the request once intent (and optional jsonSchema) is known.
- **Validation warnings:** If validation returns VALID_WITH_WARNING (e.g. discouraged intent/tone/style or role/action mismatch), resolution still succeeds and generation proceeds; warnings are only in `validation_warnings` and optionally in `recommendation_hints`. So “discouraged” choices are accepted without blocking or explicit confirm.

In short: AUTO/fallback and recommendations are applied and used for generation in one shot; the API returns rich metadata so clients can show “we used this intent/role/action” and warnings, but there is no two-phase “propose → user confirms → generate” in the current design.

---

## 5. File Path Reference (Key Classes)

| Class | Path |
|-------|------|
| UnifiedPromptEngineController | `adapter/in/web/controller/UnifiedPromptEngineController.java` |
| PromptController | `adapter/in/web/controller/PromptController.java` |
| UnifiedGeneratePromptRequest | `adapter/in/web/dto/request/UnifiedGeneratePromptRequest.java` |
| SimpleGeneratePromptRequest | `adapter/in/web/dto/request/SimpleGeneratePromptRequest.java` |
| AdvancedGeneratePromptRequest | `adapter/in/web/dto/request/AdvancedGeneratePromptRequest.java` |
| ExtractionGeneratePromptRequest | `adapter/in/web/dto/request/ExtractionGeneratePromptRequest.java` |
| UnifiedGeneratePromptResponse | `adapter/in/web/dto/response/UnifiedGeneratePromptResponse.java` |
| UnifiedPromptResponseMapper | `adapter/in/web/mapper/UnifiedPromptResponseMapper.java` |
| GenerateUnifiedPromptUseCase | `application/port/in/GenerateUnifiedPromptUseCase.java` |
| GeneratePromptUseCase | `application/port/in/GeneratePromptUseCase.java` |
| UnifiedGeneratePromptCommand | `application/port/in/command/UnifiedGeneratePromptCommand.java` |
| GeneratePromptCommand | `application/port/in/command/GeneratePromptCommand.java` |
| SemanticSelection, ExpressionOptions, OutputOptions | `application/port/in/command/normalization/*.java` |
| UnifiedGeneratePromptResult | `application/port/in/query/UnifiedGeneratePromptResult.java` |
| GeneratePromptResult | `application/port/in/query/GeneratePromptResult.java` |
| UnifiedPromptGenerationOrchestrator | `application/service/orchestration/UnifiedPromptGenerationOrchestrator.java` |
| SchemaContractEvaluator | `application/service/orchestration/SchemaContractEvaluator.java` |
| DomainResolutionService | `application/service/orchestration/DomainResolutionService.java` |
| SemanticResolutionService | `application/service/semantic/SemanticResolutionService.java` |
| SemanticRecommendationService | `application/service/semantic/SemanticRecommendationService.java` |
| SemanticValidationService | `application/service/semantic/SemanticValidationService.java` |
| GeneratePromptService | `application/service/generate/GeneratePromptService.java` |
| UnifiedRoutingFacade | `application/service/orchestration/legacy/UnifiedRoutingFacade.java` |
| IntentDefaultsResolver | `application/service/orchestration/legacy/IntentDefaultsResolver.java` |
| RoutingRuleEngine | `application/service/orchestration/legacy/RoutingRuleEngine.java` |
| DomainFinalizer | `application/service/orchestration/legacy/DomainFinalizer.java` |
| OutputContractPlanner | `application/service/orchestration/legacy/OutputContractPlanner.java` |
| IntentDefaults, FinalDomainDecision, ContractDecision | `application/service/orchestration/legacy/*.java` |
| PromptSpecFactory | `domain/service/spec/PromptSpecFactory.java` |
| PromptSpec | `domain/model/spec/PromptSpec.java` |
| ConfirmedSemanticAxes | `domain/semantic/ConfirmedSemanticAxes.java` |
| CategorySemanticProfile, CategorySemanticProfileRegistry | `domain/semantic/CategorySemanticProfile.java`, `.../impl/*.java` |
| IntentDictionary | `domain/semantic/IntentDictionary.java` |
| PromptSpecRendererAdapter | `adapter/out/render/PromptSpecRendererAdapter.java` |

All paths under: `src/main/java/org/example/sharedprompts/domain/prompt/`.
