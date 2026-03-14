# Action Taxonomy Architecture

**Constitutional source of truth** for the Prompt Engine Action taxonomy. All taxonomy truth (ActionGroup, ActionType), registry, resolver, profile, validation, recommendation, and guardrail layers must align with this document.

---

## 1. Layering

| Layer | Purpose | Rule |
|-------|---------|------|
| **ActionGroup** | Capability family only | One level above ActionType; answers "What kind of capability is being performed?" |
| **ActionType** | Executable action only | Concrete leaf; stable key is external contract. |

**Semantic flow:** Category → Intent → ActionGroup → ActionType.

---

## 2. ActionGroup = Capability Family Only

- **Must** represent a stable semantic **capability**, reusable across categories/intents where that capability applies.
- **Must NOT** represent:
  - Output shape (e.g. "report" vs "presentation" as group name)
  - Format (e.g. "PDF", "email body")
  - Channel (e.g. "Instagram", "WhatsApp")
  - Domain bucket only (e.g. "marketing" as a group; domain comes from PromptCategory/TaskDomain)
  - Artifact type as primary (artifact is outcome of capability)
  - Outcome or metric (e.g. "conversion", "ROI")
- **Single-leaf groups** (exactly one ActionType in the group) are allowed when intentional; they must be documented and bounded in guardrail tests (e.g. TRANSLATION, DEBUGGING, INTERVIEW_PREPARATION).
- Vendor/product/framework-specific items are not represented as ActionGroup; they belong in metadata or compatibility mapping.

---

## 3. ActionType = Executable Action Only

- **Must** be a concrete, executable action a user can request.
- **Must NOT** be:
  - Too generic (e.g. "general analysis" with no capability boundary)
  - Domain-only (e.g. "marketing" without the action)
  - Pure outcome/metric (e.g. "increase conversion")
  - Format/channel/purpose variant as the primary identity (prefer one action + metadata; legacy channel variants may be deprecated and kept for stable-key compatibility)
- Duplicate or near-duplicate leaves across enums are discouraged; merge or deprecate with compatibility mapping.
- Misplaced group member: ActionType must belong to the correct capability group (e.g. market/customer analysis → DATA_ANALYSIS, not a "marketing-only" group).

---

## 4. Compatibility and Migration

- **Stable key** is the external contract. Never change stable keys for existing actions when renaming or merging semantics.
- Compatibility concerns (legacy names, merged semantics, deprecated concepts) live in **resolver/mapping layers**, not in taxonomy truth.
- Do not preserve bad taxonomy inside active semantic truth; move legacy semantics into explicit legacy mapping and keep deprecated concepts traceable in tests.

---

## 5. Profiles: Group-First

- **Compatible ActionGroups** are the primary semantic truth per (category, intent).
- **Compatible ActionTypes** are derived from group membership via registry/catalog (not maintained as exhaustive leaf lists in profiles).
- Validation: resolve ActionType → ActionGroup and validate primarily at group layer; fall back to leaf list only when group map is empty.
- Recommendation: category + intent + group driven; action candidates derived from groups when possible.

---

## 6. Validation and Recommendation at Scale

- Validation and recommendation must remain scalable for 600+ ActionTypes: group-first resolution and profile compatibility at group level, not per-leaf matrices.
- No giant per-ActionType compatibility matrices in taxonomy or profile data.

---

## 7. Governance Guardrails (Code and Tests)

Mandatory guardrails must ensure:

- Every ActionType has non-null ActionGroup and required metadata (key, display names, output behavior).
- Invalid generic leaves are visible (tests or reports).
- Duplicate / near-duplicate leaves are visible.
- Single-leaf groups are reported and bounded (e.g. allowlist in tests).
- Profile compatibility is group-first; profiles are not empty by accident.
- Deprecated/merged action mappings remain traceable.
- Invalid group semantics do not re-enter taxonomy (e.g. no output-shape or channel-only groups added as ActionGroup).

---

## 8. Documented Single-Leaf Groups

The following ActionGroups are intentionally single-leaf (one ActionType each):

- **TRANSLATION** — capability to translate; sole implementation: TRANSLATION action.
- **DEBUGGING** — capability to debug; sole implementation: DEBUGGING action.
- **INTERVIEW_PREPARATION** — capability to prepare for interviews; sole implementation: INTERVIEW_PREPARATION action.

Any new single-leaf group must be added to this list and to the guardrail test allowlist.
