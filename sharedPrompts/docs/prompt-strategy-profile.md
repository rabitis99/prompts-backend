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

