## Prompt Strategy Layer – Risk & Evolution Plan

### 1. Current Strategy Layer Shape

Today the `PromptSpec` roughly looks like:

- **Objective**: what kind of task this prompt is for  
- **StrategyBundle**: `Set<PromptingStrategy>` (enum-based)  
- **Constraints**: safety / structure rules  
- **Role / Tone / Style**  
- **OutputContract**: format + sections + language  
- **UserInput**: raw + clarified input

Rendering uses the strategies directly:

- `[STRATEGIES]` section lists active strategies and their descriptions.
- Each strategy is a static enum value (e.g. `CLARIFY_FIRST`, `STEP_BY_STEP`, `CHECKLIST_VERIFY`, …).

This shape is clean for an initial version, but it has an inherent long‑term risk.

---

### 2. Long‑Term Risk: Strategy Explosion

As the prompt engine matures, we inevitably add more strategies:

- `CLARIFY_FIRST`
- `CHECKLIST_VERIFY`
- `STEP_BY_STEP`
- `DECOMPOSITION`
- `CITE_OR_UNCERTAIN`
- `REQUIRE_JUSTIFICATION`
- `FEW_SHOT_EXEMPLAR`
- `CHAIN_OF_VERIFICATION`
- `EDGE_CASE_SCAN`
- `SELF_CONSISTENCY`
- `TREE_OF_THOUGHTS`
- `FORMAT_LOCK`
- … (more over time)

If each of these remains a “flat enum switch” that can be arbitrarily combined, three problems show up in production:

1. **Strategy conflicts**
   - Example: `SELF_CONSISTENCY` and `TREE_OF_THOUGHTS` both push heavy multi‑path reasoning. Used together, they can easily overshoot cost and latency and confuse the model.

2. **Strategy overload**
   - A single prompt can end up with 8–12 active strategies.
   - The meta‑prompt becomes a long list of techniques; the model sees instruction overload instead of a clear, compact plan.

3. **Task–strategy mismatch**
   - Some strategies only make sense for certain task types:
     - `TREE_OF_THOUGHTS` is helpful for creative reasoning, but pointless or harmful for simple information extraction.
   - With only a flat enum set, nothing prevents us from activating the wrong strategy bundle for a given objective.

The net effect is that over time the engine tends toward:

- **Longer, denser meta‑prompts**
- **Lower effective instruction clarity**
- **More difficult debugging/maintenance**

This “strategy explosion” is one of the most common failure patterns in real‑world prompt engines.

---

### 3. Recommended Evolution: StrategyProfile Layer

Conceptually, we want to move from:

- **Current**:  
  `PromptSpec` → directly contains `Set<PromptingStrategy>`

to:

- **Recommended**:  
  `PromptSpec` → has an **Objective**  
  Objective (plus constraints, risk appetite, etc.) → resolves to a **StrategyProfile**  
  Profile → internally decides the concrete `Set<PromptingStrategy>`

#### 3.1 Example profiles

Potential profile types:

- `REASONING_HEAVY`
- `STRUCTURED_EXTRACTION`
- `CREATIVE_GENERATION`
- `SIMPLE_TASK`

Each profile owns:

- Which strategies may be used
- How many strategies are allowed (e.g. max 3–4)
- Which strategies are mutually exclusive
- How to adapt to constraints (e.g. no questions allowed, strict latency budget)

Additionally, a production‑grade `StrategyProfile` system should define:

- **Profile resolution failure handling**
  - When no profile matches the given objective/constraints, fall back to a safe default profile (e.g. `SIMPLE_TASK`) or a “no extra strategies” baseline, and surface metrics/alerts for missing coverage.

- **Profile versioning**
  - Profiles will evolve; introduce an explicit version (e.g. `REASONING_HEAVY_V1`, `REASONING_HEAVY_V2`) and keep old versions around long enough to support A/B testing and safe rollbacks.

- **Profile configuration management**
  - Decide where profiles live (code, config files, or database) and how they are deployed.
  - Early stage: code‑based profiles are fine; later, consider configuration‑driven profiles for faster iteration without redeploy.

- **Edge cases & overlap handling**
  - When multiple profiles could apply, define a deterministic tie‑breaker (priority, explicit overrides, or composition rules).
  - When no profile clearly fits, define a conservative fallback path rather than guessing, and record the case for future profile design.

Example:

- `REASONING_HEAVY`
  - `STEP_BY_STEP`
  - `CHECKLIST_VERIFY`
  - `CHAIN_OF_VERIFICATION`
  - (optional) `EDGE_CASE_SCAN`
- `STRUCTURED_EXTRACTION`
  - `CLARIFY_FIRST`
  - `CHECKLIST_VERIFY`
  - `FORMAT_LOCK`

The key point: **profiles keep the number of active strategies small and task‑appropriate.**

---

### 4. How It Fits the Existing Architecture

We do **not** change the renderer or the meta‑prompt section structure. Instead, we introduce a resolver in front of rendering:

1. **User input** → initial `PromptSpec` (objective, raw constraints, etc.)
2. **Objective Resolver**: normalizes/infers objective if needed
3. **StrategyProfileResolver**: decides the profile based on:
   - `Objective`
   - `Constraints` (e.g. no questions, strict length)
   - (Optionally) environment flags (risk level, latency budget)
4. **StrategyProfile → StrategyBundle**: profile materializes a bounded `Set<PromptingStrategy>`
5. **PromptSpecRendererAdapter** runs as today:
   - `[OBJECTIVE]`
   - `[STRATEGIES]`
   - `[CONSTRAINTS]`
   - `[ROLE CONTEXT]`
   - `[OUTPUT FORMAT]`
   - `[USER INPUT]`

In other words:

- **Strategy selection logic** lives in domain/application (`StrategyProfileResolver`).
- **Strategy presentation** remains in adapter/render layer (`StrategySectionRenderer`).

The existing types (`PromptSpec`, `StrategyBundle`, `Objective`) are already good anchors for this evolution.

From a production perspective, we also need a clear **migration and compatibility plan**:

- **Backward compatibility**
  - Keep accepting the existing “flat” `Set<PromptingStrategy>` for a transition period.
  - When both a legacy bundle and a profile are present, define which one wins (e.g. profile first, legacy only as fallback).

- **Migration strategy**
  - Start by mapping the most common legacy patterns to named profiles.
  - Gradually replace hard‑coded strategy bundles in application code with profile lookups, guided by logs/metrics.

- **Gradual rollout**
  - Enable the profile resolver behind a feature flag or environment toggle.
  - Roll out per endpoint/tenant or per objective type, and compare quality/cost metrics before and after enabling profiles.

---

### 5. Benefits of the Profile Approach

1. **Bounded complexity per prompt**
   - Even if we introduce many new strategies over time, each profile can enforce:
     - “At most 3–4 strategies per prompt.”
   - This keeps prompts short, focused, and more robust across models.

2. **Task‑aware strategy selection**
   - Profiles can be tuned per objective:
     - Creative / open‑ended tasks → reasoning‑heavy or creative profiles.
     - Extraction / classification → structured, low‑reasoning profiles.

3. **Conflict management in one place**
   - Profiles are the single point where mutual exclusions live:
     - e.g. never enable both `SELF_CONSISTENCY` and `TREE_OF_THOUGHTS` together.

4. **Clear separation of concerns**
   - Renderer focuses on: “Given a spec (already decided), render the best meta‑prompt.”
   - Profile resolver focuses on: “Given objective + constraints, what is the right strategy bundle?”

5. **Easier experimentation**
   - We can A/B test profiles (e.g. `REASONING_HEAVY_V2`) without touching renderers.
   - Each profile can evolve its internal mix of strategies over time.

6. **Known trade‑offs**
   - Additional abstraction layer introduces some **initial complexity** in the codebase.
   - There is a **learning curve** for the new `StrategyProfile` concept (especially for new contributors).
   - Profiles require **ongoing maintenance** as objectives, constraints, and product requirements evolve.
   - However, these trade‑offs are typically **small and front‑loaded** compared to the long‑term benefits in safety, clarity, and evolvability of the prompt engine.

---

### 6. Suggested Next Steps

1. **Introduce a StrategyProfile abstraction in the domain layer**
   - Simple enum or interface to start:
     - `StrategyProfile { Set<PromptingStrategy> toStrategies(PromptSpec spec); }`

2. **Add a StrategyProfileResolver**
   - Pure function/service:
     - Input: `PromptSpec` (especially objective, constraints, system flags)
     - Output: `StrategyProfile`

3. **Hook the resolver into PromptSpec construction**
   - At the point where `PromptSpec` is finalized for rendering:
     - Resolve profile
     - Derive `StrategyBundle`
     - Keep the renderer completely unaware of *how* it was chosen.

4. **Define a small initial set of profiles**
   - Start with 3–4 profiles that reflect current real use‑cases.
   - Gradually move existing strategy selection rules into these profiles.

With this evolution, the current meta‑prompt and renderer design remains intact, while the system becomes much more resilient to long‑term growth in strategies and complexity.

5. **Add testing and operations safeguards**
   - **테스트 전략**
     - Profile resolver에 대해: 다양한 `Objective`/`Constraints` 조합에 대한 단위·통합 테스트를 작성하고, 각 조합이 예상 프로필로 매핑되는지 검증합니다.
     - 프로필 간 A/B 테스트: 동일한 입력 세트에 대해 서로 다른 프로필 버전을 돌려 품질/비용/레이턴시를 비교합니다.
     - 회귀 테스트: 기존 flat 전략 번들과 새 프로필 기반 결과를 비교하는 회귀 스위트(or 골든 세트)를 유지합니다.
   - **관찰 가능성**
     - 어떤 프로필이 왜 선택되었는지(입력 objective/constraints, 결정 경로)를 구조화 로그/트레이스로 남깁니다.
     - 프로필별 성공률, 비용, 레이턴시 메트릭을 수집해 대시보드로 모니터링합니다.
     - 예상치 못한 프로필 선택 패턴이나 오류에 대한 알림 규칙을 정의합니다.
   - **성능 및 롤백**
     - Profile resolution 오버헤드를 측정하고, 레이턴시 예산 내에서 동작하는지 주기적으로 검증합니다.
     - 문제가 발생할 경우를 대비해, 기능 플래그 하나로 기존 flat 전략 경로로 완전히 되돌릴 수 있는 롤백 플랜을 마련합니다.

---

### 7. Topics for Future Iterations

- **멀티 테넌시 (Multi‑Tenancy)**  
  - 고객/테넌트별로 서로 다른 `StrategyProfile` 구성이 필요한지, 그리고 이를 어떻게 격리/구성할지.

- **비용 분석 (Cost & Latency)**  
  - 다양한 프로필 조합이 LLM 호출 횟수, 토큰 사용량, 지연 시간에 미치는 영향을 어떻게 모니터링하고 제한할지.

- **보안 & 안전성 (Security & Safety)**  
  - 악의적인 입력이나 비정상적인 컨텍스트가 잘못된 프로필 선택을 유도하지 않도록 어떤 방어 로직을 둘지.

- **국제화 (Internationalization)**  
  - 언어/지역별로 다른 전략 프로필이 필요한지, 예를 들어 특정 언어에서 더 잘 동작하는 전략이나 포맷이 있는지.

이 문서는 핵심 아키텍처 진화에 집중하고 있으며, 위의 주제들은 시스템이 성숙해짐에 따라 별도 문서나 후속 설계 논의에서 다루는 것을 권장합니다.