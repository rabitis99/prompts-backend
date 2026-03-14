# Action Taxonomy Refactor — Implementation Report

**Date:** 2025-03-13  
**Scope:** Semantic architecture refactor across taxonomy, registry, resolver, profile, validation, recommendation, and guardrail layers per the Action Taxonomy Architecture document.

---

## 1. Modified Files

| File | Change |
|------|--------|
| `docs/action-taxonomy-architecture.md` | **Created.** Constitutional source of truth for ActionGroup (capability-family-only), ActionType (executable-action-only), group-first profiles, compatibility/migration rules, and governance guardrails. |
| `docs/action-taxonomy-refactor-report.md` | **Created.** This implementation report. |
| `src/main/.../action/canonical/ActionGroup.java` | Doc reference: `docs/action-taxonomy-architecture.md` clarified in javadoc. |
| `src/main/.../domain/semantic/impl/DefaultCategorySemanticProfileRegistry.java` | Removed `@Component`. Removed 1-arg constructor. Registry now **requires** both `CanonicalActionRegistry` and `ActionTypeRegistry` so that compatible actions are always **derived from groups** (group-first). |
| `src/main/.../infrastructure/config/ResolutionConfig.java` | Added `@Bean` `CategorySemanticProfileRegistry categorySemanticProfileRegistry(CanonicalActionRegistry, ActionTypeRegistry)` so the profile registry is built with both registries and used for group-first derivation. |
| `src/main/.../action/category/development/DevOpsActionType.java` | Removed duplicate `@Override` on `getActionGroup()`. |
| `src/test/.../canonical/ActionTaxonomyGuardrailTest.java` | Added: `noDuplicateStableKeys()`, `profileCompatibilityIsGroupFirst()`, `deprecatedActionsRemainTraceable()`. Updated doc reference. |
| `src/test/.../semantic/ProfileCanonicalFirstTest.java` | Updated to use 2-arg constructor `DefaultCategorySemanticProfileRegistry(registry, actionTypeRegistry)`. |

---

## 2. ActionGroup Changes

- **No enum value renames or removals.** Current `ActionGroup` values were audited and treated as capability-family-only; they already align with the architecture (no output-shape, channel, or domain-only groups were renamed in this pass).
- **Documentation:** Single-leaf groups (TRANSLATION, DEBUGGING, INTERVIEW_PREPARATION) are explicitly documented in `docs/action-taxonomy-architecture.md` and enforced in guardrail tests.

---

## 3. ActionType Merges / Deprecations / Moves

- **No new merges or moves in this refactor.** Existing deprecated leaves remain as-is with stable keys:
  - `CREATIVE_WRITING_GEN` (Writing) — deprecated duplicate of CreativeActionType.CREATIVE_WRITING; kept for key compatibility.
  - `TEXT_MESSAGE`, `WHATSAPP_MESSAGE` (Writing) — deprecated channel variants; use MESSAGE_WRITING + metadata.
- **Market/customer analysis:** Already map to `DATA_ANALYSIS` (MarketingActionType.MARKET_RESEARCH, CUSTOMER_ANALYSIS); guardrail test confirms traceability.

---

## 4. Profile Model Changes

- **Group-first enforced in production:** `DefaultCategorySemanticProfileRegistry` is no longer component-scanned with a 1-arg constructor. It is built via `ResolutionConfig` with both `CanonicalActionRegistry` and `ActionTypeRegistry`. Compatible actions are **derived from compatible ActionGroups** via `deriveActionsFromGroups()`; profile data no longer relies on exhaustive leaf lists for scalability.
- **Validation / recommendation:** Unchanged in contract. They already prefer `getCompatibleActionGroupsForIntent()` and fall back to `getCompatibleActionsForIntent()` when group map is empty; with group-first derivation, the group map is always populated when a profile has actions for an intent.

---

## 5. Validation / Recommendation Changes

- **SemanticValidationService:** No code change. Continues to validate action compatibility at group level first (`getCompatibleActionGroupsForIntent`), then falls back to leaf list.
- **SemanticRecommendationService:** No code change. Uses same group-first logic for hints and matching.
- **Scalability:** Profiles now derive action lists from groups, so adding new ActionTypes under existing ActionGroups does not require profile edits.

---

## 6. Compatibility Mapping Changes

- **Stable-key contract:** Preserved. All existing action keys remain valid; deprecated entries remain resolvable and map to expected ActionGroups (guardrail test `deprecatedActionsRemainTraceable`).
- **Resolver:** `DefaultActionTypeResolver` and `ActionTypeCompatibilityResolver` unchanged; compatibility remains in resolver/mapping layers, not in taxonomy truth.
- **Legacy mapping:** No new legacy map added; existing deprecated actions are traceable via registry + canonical resolution, as asserted by tests.

---

## 7. Guardrails Added / Updated

| Guardrail | Location | Purpose |
|-----------|----------|---------|
| Every ActionType has non-null ActionGroup and required metadata | `ActionTaxonomyGuardrailTest#everyActionHasRequiredMetadata` | Existing; unchanged. |
| Single-leaf groups documented and bounded | `ActionTaxonomyGuardrailTest#singleLeafGroupsAreDocumented` | Existing; unchanged. |
| No duplicate stable keys | `ActionTaxonomyGuardrailTest#noDuplicateStableKeys` | **New.** Fails if any two actions share a key. |
| Profile compatibility group-first | `ActionTaxonomyGuardrailTest#profileCompatibilityIsGroupFirst` | **New.** For each category with actions per intent, asserts non-empty `getCompatibleActionGroupsForIntent`. |
| Deprecated actions traceable | `ActionTaxonomyGuardrailTest#deprecatedActionsRemainTraceable` | **New.** Asserts TEXT_MESSAGE, WHATSAPP_MESSAGE, CREATIVE_WRITING_GEN resolve and map to expected ActionGroups. |
| Market/customer analysis → DATA_ANALYSIS | `ActionTaxonomyGuardrailTest#marketCustomerAnalysisMapsToDataAnalysis` | Existing; unchanged. |

---

## 8. Remaining Tradeoffs

- **Profile registry construction:** The profile registry **must** receive `ActionTypeRegistry` at construction time. All wiring is through `ResolutionConfig`; there is no fallback when the registry is missing. This is intentional so that group-first derivation is always used in production.
- **Single-leaf cap:** The test allows at most 5 single-leaf groups; any new intentional single-leaf group must be added to the allowlist in the test and to `docs/action-taxonomy-architecture.md`.
- **No automated “invalid group semantics” check:** The architecture doc forbids output-shape, channel, or domain-only groups. Enforcement is via code review and the documented rules; no automated test blocks specific group names from being added.
- **Linter:** Existing deprecation warnings (e.g. use of `CREATIVE_WRITING_GEN`, `EtcActionType.EXPLANATION`) remain; they are intentional for backward compatibility and are not addressed in this refactor.

---

## 9. Summary

- **Taxonomy truth:** ActionGroup remains capability-family-only; ActionType remains executable-action-only; no taxonomy renames or merges in this pass.
- **Profiles:** Group-first derivation is mandatory in production via explicit bean wiring and removal of the 1-arg profile registry constructor.
- **Validation / recommendation:** Unchanged in behavior; they remain group-first and scalable.
- **Stable keys and deprecated mappings:** Preserved and covered by new guardrail tests.
- **Governance:** Architecture doc and guardrail tests (duplicate keys, group-first profiles, deprecated traceability) added or extended as specified.
