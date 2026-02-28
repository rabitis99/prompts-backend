# Prompt Domain Refactoring Proposal — SOLID & Hexagonal

Production-grade AI prompt compiler: explicit mapping, stable engine, predictable domain, low runtime cost.

---

## 1. New Package Tree

```
domain/prompt/
├── domain/                                  # Pure domain — zero framework
│   ├── model/                               # PromptSpec, VerifyResult, OutputContract, ...
│   ├── value/                               # PromptObjective, PromptStrategyBundle, ...
│   ├── objective/                           # Objective profiles & registry
│   │   ├── ObjectiveProfile.java
│   │   ├── ObjectiveRegistry.java
│   │   ├── DefaultObjectiveRegistry.java
│   │   └── profiles/                        # One class per objective
│   ├── verification/                        # Verify step — strategy per objective
│   ├── policy/                              # StrategyBundlePolicy only
│   ├── resolution/                          # ★ TaskDomain / Objective 해석 전용
│   │   ├── DomainResolver.java              # ActionType + PromptCategory → TaskDomain
│   │   ├── ObjectiveResolverPort.java       # Port: (TaskDomain, ActionType) → PromptObjective
│   │   ├── ExplicitObjectiveMapping.java    # Explicit (action → objective) table
│   │   ├── ObjectiveMappingRegistry.java    # Heuristic fallback + domain default
│   │   └── ObjectiveResolver.java           # Implementation of ObjectiveResolverPort
│   └── service/                             # PromptSpecFactory, Validator, BadgeResolver, ...
│
├── application/                             # Use cases & ports
├── adapter/
├── enums/
└── infrastructure/
    └── config/
        ├── ResolutionConfig.java            # ★ 해석 빈만 (DomainResolver, ObjectiveResolver, ...)
        └── PromptDomainConfig.java          # 레지스트리·팩토리·Validator 등, @Import(ResolutionConfig)
```

**Rationale**

- **core**: No Spring/Jakarta; all behavior behind interfaces (ObjectiveRegistry, ObjectiveResolverPort). New objective = new profile + one registration line (OCP).
- **resolution**: Single place for TaskDomain and Objective resolution; no scattered logic in factory or services.
- **enums**: One package; document which enums are “resolution inputs” (TaskDomain, PromptCategory, ActionType) vs “spec-only” (Tone, Style, Language, Experience).

---

## 2. Objective Resolution Design (Explicit & Deterministic)

**Current issues**

- ActionType → Objective is split across: `ActionType.getDefaultObjective()`, `ObjectiveMappingRegistry` (keyword heuristic), `PromptSpecFactory.resolveObjective()`, and domain default.
- Name-based `String.contains(keyword)` is implicit and fragile.

**Target design**

- **Single port**: `ObjectiveResolverPort.resolve(TaskDomain taskDomain, ActionTypeInterface actionType) → PromptObjective`.
- **Deterministic chain** (one implementation):
  1. **Explicit override**: `actionType.getDefaultObjective()` if non-null.
  2. **Explicit mapping**: Lookup in `ExplicitObjectiveMapping`: a fixed map built at config time from (ActionType, PromptObjective) pairs — no string matching.
  3. **Domain default**: `TaskDomain → PromptObjective` (e.g. TECHNICAL→REASONING, CREATIVE→CREATIVE_WITH_CONSTRAINTS). No fallback to keyword.
- **Enum explosion**: Avoid by keeping many ActionType enums but **not** adding methods to all; resolution uses (1) only for enums that explicitly override, (2) explicit map for important actions, (3) domain default for the rest. New action types get an entry in the explicit map or a domain default; no new enum-to-enum coupling.

**Concrete types**

- `ObjectiveResolverPort`: interface in `core/resolution/`.
- `ExplicitObjectiveMapping`: holds `Map<ActionTypeInterface, PromptObjective>` (or equivalent key strategy); populated in `PromptDomainConfig` from a list of rules.
- `ObjectiveResolver`: implements port, depends only on `ExplicitObjectiveMapping` and domain-default function (e.g. `TaskDomain → PromptObjective`). No keyword logic in core; optional “legacy” keyword fallback can live in infrastructure if needed during migration.

**Result**

- One reason to change for “how is objective decided”: `ObjectiveResolver` (and its mapping data).
- New objective: add profile + register; new action type: add explicit mapping or domain default. No change to resolver algorithm (OCP).

---

## 3. Strategy Simplification Model

**Keep**

- Three tiers: **CORE** (0 extra calls), **OBJECTIVE_SPECIFIC** (1–2 extra), **EXPERIMENTAL** (2–4 extra).
- One experimental strategy per bundle (enforced in `PromptStrategyBundle`).
- `StrategyBundlePolicy`: gets default bundle from `ObjectiveProfile.defaultBundle()`, strips experimental if disabled, downgrades to core-only if `1 + totalAdditionalLlmCalls() > profile.maxLlmCallCount()`.

**Avoid**

- Extra strategy tiers or runtime strategy expansion.
- `StrategyPromotionPolicy` (alpha/beta/tier) used in generation path — keep out of hot path or remove if unused.

**Simplification**

- Policy has a single responsibility: “resolve bundle for (objective, experimentalEnabled)” with the two guards above. No promotion logic in the main pipeline.
- Strategy set remains closed (enum); no dynamic registration.

---

## 4. Enum Reduction Strategy

| Enum / group           | Role                    | Reduction approach |
|------------------------|-------------------------|--------------------|
| **TaskDomain**         | Resolution + spec        | Keep; core domain concept. |
| **PromptCategory**     | Resolution only         | Keep; used only by DomainResolver. |
| **ActionTypeInterface** | Resolution + spec     | Keep interface; avoid adding more methods. Prefer explicit mapping over new enums. |
| **PromptObjective**    | Spec + profile key      | Keep; value object / identifier. |
| **PromptingStrategy**  | Bundle content          | Keep; closed set. |
| **ToneType, StyleType, LanguageType, ExperienceLevel** | Spec only | Keep in spec; do not use in resolution. Consider grouping in a “PresentationOptions” value object later to reduce PromptSpec constructor params. |
| **RoleTypeInterface**  | Spec (role section)     | Keep; display names by locale. |

**Rules**

- No enum-to-enum cross-coupling (e.g. no `ActionType → PromptCategory` or vice versa in domain logic beyond the defined resolution chain).
- No FQCN or reflection; no “resolve by enum name string”.
- New behavior = new profile or new mapping entry, not new enum branch in a switch.

---

## 5. Revised PromptSpec Structure

- **Keep** current `PromptSpec` as the single immutable DTO for the 4-step pipeline (Clarify → Solve → Verify → Repair). No structural change required for SOLID.
- **Optional later**: Extract “style” fields (tone, style, locale, experienceLevel) into a value object `PresentationOptions` to reduce parameter count and clarify “resolution vs presentation” in the factory.
- **Clarify**: PromptSpec is built **after** resolution; it receives already-resolved `PromptObjective`, `TaskDomain`, and `PromptStrategyBundle`. Factory’s only job is to build the spec from resolved values + profile (SRP).

---

## 6. Flow: Clarify → Solve → Verify → Repair

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  GeneratePromptService.generate(command)                                     │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. CLARIFY                                                                  │
│     • ValidateUserPort.validateUserExists(command.userId())                  │
│     • TaskDomain = DomainResolver.resolveDomain(actionType, promptCategory)  │
│     • PromptObjective = ObjectiveResolver.resolve(taskDomain, actionType)    │
│     • PromptSpec = PromptSpecFactory.create(rawInput, taskDomain, actionType,│
│                   role, tone, style, locale, experimentalEnabled, …)         │
│       → Factory uses ObjectiveResolver (not mapping registry directly)      │
│     • clarifiedInput = normalize(rawInput); spec = spec.withClarifiedInput() │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  2. SOLVE                                                                   │
│     • useConstrained = ObjectiveRegistry.get(spec.getObjective())            │
│                         .supportsConstrainedDecoding()                       │
│     • If useConstrained && spec has JSON schema → ConstrainedDecodingPort    │
│     • Else → LLMClientPort.solve(spec)                                       │
│     • draft = result                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  3. VERIFY                                                                   │
│     • VerifyResult = PromptSpecValidator.verify(draft, spec)                 │
│       → validator uses ObjectiveRegistry.get(spec.getObjective())            │
│         .verificationStrategy().verify(VerificationContext(spec, draft))    │
│     • No enum branching in validator; strategy is per objective (LSP).      │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  4. REPAIR (max 2 attempts if !verifyResult.isPassed())                      │
│     • draft = LLMClientPort.repair(draft, spec, failedItems, failureReasons) │
│     • Verify again; repeat until passed or max attempts                      │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  SavePromptVersionPort.save(command, spec, draft, repairCount, finallyPassed)│
│  BadgeResolver.resolve(...) → GeneratePromptResult                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

- **Clarify**: DomainResolver + ObjectiveResolver + PromptSpecFactory only. No application service doing resolution logic.
- **Solve**: Ports only; constrained decoding decided by profile.
- **Verify**: Validator + ObjectiveRegistry + VerificationStrategy; no verify-mode enum.
- **Repair**: Fixed max attempts; no strategy expansion.

---

## 7. How SOLID Is Satisfied

| Principle | Application |
|-----------|-------------|
| **S — Single Responsibility** | DomainResolver: only “resolve TaskDomain”. ObjectiveResolver: only “resolve PromptObjective”. PromptSpecFactory: only “build PromptSpec from resolved inputs + profile”. StrategyBundlePolicy: only “resolve bundle with guards”. PromptSpecValidator: only “run verification strategy for spec”. |
| **O — Open/Closed** | New objective: add ObjectiveProfile implementation + register in DefaultObjectiveRegistry (+ optional explicit mapping entry). New action type: add explicit mapping or rely on domain default. No change to resolver logic, factory, or validator. |
| **L — Liskov** | All ObjectiveProfile implementations are substitutable (same interface). All VerificationStrategy implementations accept VerificationContext and return VerifyResult. |
| **I — Interface Segregation** | ObjectiveProfile is one interface for “behavior of an objective”; no heavy optional methods. Ports (ObjectiveResolverPort, LLMClientPort, etc.) are narrow. ActionTypeInterface keeps display + optional getDefaultObjective/getTaskDomain; no forced heavy interface for all enums. |
| **D — Dependency Inversion** | Application (GeneratePromptService, PromptSpecFactory) depends on ObjectiveResolverPort, DomainResolver, ObjectiveRegistry, StrategyBundlePolicy, ports — all abstractions. Implementations live in core or infrastructure. |

---

## 8. Risk Analysis

| Risk | Mitigation |
|------|-------------|
| **Migrating from keyword to explicit map** | Introduce ObjectiveResolver with explicit mapping; keep keyword fallback behind a flag or in a separate “legacy” component until all important action types are in the map. Then remove keyword path. |
| **Breaking callers of PromptSpecFactory** | Factory signature unchanged; only internal dependency changes (ObjectiveMappingRegistry → ObjectiveResolverPort). Config wires new bean. |
| **StrategyPromotionPolicy unused** | Remove from generation path or delete; document that strategy set is closed. |
| **Package move (core/resolution)** | Do incrementally: add new packages/classes first, then move and update imports; run tests after each move. |
| **Performance** | Resolution is a few map lookups and one optional chain; no reflection, no scanning. Low token cost preserved. |

---

## 9. Implementation Checklist (Incremental)

- [ ] Introduce `ObjectiveResolverPort` and `ObjectiveResolver` in domain (resolution package or current service package).
- [ ] Introduce `ExplicitObjectiveMapping` (or equivalent) and populate from config for high-value action types; keep domain-default fallback.
- [ ] Change `PromptSpecFactory` to depend on `ObjectiveResolverPort` instead of `ObjectiveMappingRegistry`; remove `resolveObjective` from factory (call resolver in application or in factory with resolver injected).
- [ ] Wire `ObjectiveResolver` in `PromptDomainConfig`; optionally keep `ObjectiveMappingRegistry` as internal to `ObjectiveResolver` for backward compatibility (keyword fallback) until explicit map is complete.
- [ ] (Optional) Move DomainResolver / ObjectiveResolver into `core/resolution/` and update imports.
- [ ] (Optional) Group Tone/Style/Language/ExperienceLevel into `PresentationOptions` in PromptSpec.
- [ ] Remove or bypass StrategyPromotionPolicy from pipeline if unused.
- [ ] Add/update unit tests for ObjectiveResolver chain and StrategyBundlePolicy.

---

## 10. Philosophy Summary

- **UX**: Simple — one flow, clear outcomes.
- **Engine**: Stable — Clarify → Solve → Verify → Repair; no runtime strategy mutation.
- **Domain**: Predictable — explicit resolution chain, no implicit name-based mapping.
- **Mapping**: Explicit — config-driven (action → objective) and domain default.
- **Runtime**: Cheap — no reflection, no scanning, minimal branching.

This refactoring keeps hexagonal boundaries, preserves low token cost, hides engine complexity from UX, minimizes enum coupling, and aligns objective/strategy flow with the actual generation pipeline while enforcing SOLID.
