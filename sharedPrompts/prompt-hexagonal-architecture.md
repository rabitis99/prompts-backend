# Prompt 도메인 헥사고날 아키텍처 & 정확도 중심 설계

이 문서는 `prompt` 도메인에 **헥사고날/클린 아키텍처**를 도입하고, **정확도(accuracy)** 를 enum·정책·검증 루프로 정의하기 위한 설계·연구 근거·실행 요구사항을 하나로 정리한 것이다.

---

## Part I. 연구 근거와 설계 방향

### 1. 프롬프트 구조·명확성과 출력 품질

- 구조화되고 명확한 프롬프트는 LLM 출력의 품질·신뢰도·효율성을 향상시키는 것으로 연구에서 보고된다.
- 문맥과 지시가 명확할수록 사용자 만족도와 작업 효율이 올라간다는 설문 결과가 있다.

**적용:** 도메인/유즈케이스 레이어에서 프롬프트 구조를 결정하고, "구조와 명확성"을 코드로 강제·재사용한다.

### 2. 프롬프트 엔지니어링은 스킬/리터러시

- AI를 잘 쓰는 사람과 그렇지 않은 사람의 차이는 **어떤 정보를 포함하고, 어떤 순서·제약·우선순위로 지시하는지**에 달려 있다.
- 프롬프트 설계 능력이 높을수록 출력 품질이 통계적으로 좋아지는 경향이 있다.

**적용:** 이 "프롬프트 스킬"을 **도메인 규칙/정책/구조로 캡슐화**해 코드로 재사용한다.

### 3. 다양한 기법과 “길이 vs 품질”

- Zero-shot, Few-shot, CoT, 역할/페르소나, 단계별 지시 등 여러 기법이 존재하며, **목적/작업에 따라 조합**해야 한다.
- **긴 프롬프트가 항상 좋은 것은 아니다.** 핵심 목적·제약·우선순위가 명확한 것이 더 중요하다.

**적용:** 길이가 아니라 **구조/우선순위**를 `PromptSpec`으로 관리하고, 인프라는 스펙을 실행만 한다.

### 4. 역할/페르소나와 품질

- 역할 부여는 스타일/톤/관점 조절에 유용하지만, **작업 유형과 설계 방식**에 따라 효과가 다르다.

**적용:** `RoleType`, `ToneType`, `StyleType`을 enum·도메인 값으로 정규화하고, 가이드라인을 도메인 레이어에 정의한다.

### 5. 정확도는 “느낌”이 아니다

- 정확도는 프롬프트를 길게 쓰거나 역할만 줘서 올라가지 **않는다**.
- **명확한 Objective**, **Solve → Verify → Repair** 루프, **전략 조합·실험**, **Golden set 회귀 테스트**가 있을 때 올라가고 유지된다.

**적용:** `PromptObjective`, `QualityRubric`, 4단계 유즈케이스, Validator 확장, Repair 루프, Golden set을 도메인·애플리케이션에 명시한다.

---

## Part II. 목표와 도메인 정의

### 0. 목표

- **헥사고날 아키텍처:** 도메인은 순수 자바, 애플리케이션은 유즈케이스·포트, 인프라는 어댑터로 분리.
- **정확도 정의:** 도메인에 `PromptObjective`, `QualityPriority`, `QualityRubric`을 두어 정확도를 enum·정책으로 고정.
- **품질 루프:** Clarify → Solve → Verify → Repair 4단계와 2회 제한 Repair로 품질을 제어·측정.

### 0-1. 정확도 관련 도메인 모델

| 모델 | 용도 |
|------|------|
| **PromptObjective** | 이 프롬프트가 추구하는 정확도의 종류 |
| **QualityPriority** | 품질 우선순위 (정확도 vs 구조 vs 간결 vs 창의) |
| **QualityRubric** | 검증 시 사용할 체크리스트 (필수 요구사항, 입력 유지, 모순 없음 등) |

**PromptObjective (enum)**

- `FACTUAL` — 사실 정확성 (날짜, 수치, 인용)
- `REASONING` — 논리적 타당성 (전제→결론 일관)
- `EXTRACTION` — 정확한 필드 추출 (엔티티/키-값 보존)
- `PLANNING` — 단계 일관성 (순서·선행조건 유지)
- `CREATIVE_WITH_CONSTRAINTS` — 제약 내 창작 (형식/톤 준수)

**QualityPriority (enum)**

- `ACCURACY_FIRST`, `STRUCTURE_FIRST`, `BREVITY_SECOND`, `CREATIVITY_SECOND`

**QualityRubric (체크리스트)**

- 필수 요구사항 누락 없음
- 입력 조건 유지 (숫자/조건/엔티티 보존)
- 숫자/엔티티 보존
- 모순 없음 (A이다 + A가 아니다 패턴 금지)
- 근거 명시 또는 불확실성 명시

---

## Part III. 아키텍처와 책임

### 7. Before → After 책임 재배치

```text
Before
------
Controller/Service: 요청 파싱 → 문자열 프롬프트 조립 → LLM 호출 → 응답 파싱

After
-----
domain.prompt.domain
  - TaskDomain, PromptCategory, ActionType, ToneType, StyleType, RoleType
  - PromptObjective, QualityPriority, QualityRubric
  - PromptSpec, PromptSection, Constraints, OutputContract, ContentSandbox
  - GuidelinePolicy, GuidelineRule, *Guidelines, DomainResolver
  - PromptSpecFactory, PromptSpecValidator (도메인 규칙)

domain.prompt.application
  - GeneratePromptUseCase: Clarify → Solve → Verify → Repair (4단계)
  - LLMClientPort, PromptSpecRendererPort

domain.prompt.adapter
  - in: HTTP/메시징 → Command/Result
  - out: LLMClientAdapter (OpenAI Structured Outputs 등), JsonSchemaValidatorAdapter
```

### 7-1. PromptSpec (통합 정의)

```java
class PromptSpec {
    // 정확도·품질
    PromptObjective objective;
    QualityPriority qualityPriority;
    QualityRubric rubric;
    // 구조
    List<PromptSection> sections;
    Constraints constraints;
    OutputContract outputContract;
    ContentSandbox contentSandbox;
    // 역할·톤·전략
    RoleType roleType;
    ToneType toneType;
    StyleType styleType;
    PromptStrategyBundle strategyBundle;
    // 기타
    Locale locale;
}
```

### 7-2. GeneratePromptUseCase 4단계

1. **Clarify** — 모호성 제거 (입력 정규화, 필수 조건 명시)
2. **Solve** — 초안 생성 (PromptSpec 기반 LLM 호출)
3. **Verify** — 루브릭·스키마 검증 (Coverage, 입력 유지, 모순, 불확실성)
4. **Repair** — 실패 항목만 수정 요청 (최대 2회, 실패 루브릭 로깅)

### 7-3. 전략 조합: PromptStrategyBundle

**기존:** 단일 `PromptingStrategy` enum  
**변경:** 조합 가능한 전략 집합

```java
class PromptStrategyBundle {
    Set<PromptingStrategy> strategies;
}
```

**정확도 상승 전략 (enum 추가)**

- `CLARIFY_FIRST`, `DECOMPOSITION`, `STEP_BY_STEP`, `CHECKLIST_VERIFY`
- `EDGE_CASE_SCAN`, `CITE_OR_UNCERTAIN`, `REQUIRE_JUSTIFICATION`

**도메인 연결:** TaskDomain / PromptCategory에 따라 기본 전략 집합 고정.  
예: `TaskDomain = ANALYSIS` → `DECOMPOSITION` + `CHECKLIST_VERIFY`.

---

## Part IV. 제약·검증·Repair

### 8. 제약 침범 방지와 검증 루프

- **PromptSpec = Constraints + OutputContract + ContentSandbox**
- **Validator:** 스키마/형식 + **정확도 루브릭** (Coverage, 입력 유지, 모순, 불확실성)
- **Repair:** 실패 항목만 수정 요청, **2회 제한**, 실패 루브릭 로깅

### 8-1. Validator 확장 (정확도 중심)

| 검사 | 내용 |
|------|------|
| Coverage | 사용자 요구사항 항목·필수 섹션 누락 여부 |
| 입력 조건 유지 | 숫자/조건/엔티티가 답변에서 사라지거나 변경되지 않았는지 |
| 모순 탐지 | "A이다" + "A가 아니다" 패턴, 확정 표현 남용 시 근거 부재 |
| 불확실성 처리 | 근거 없이 단정 → Repair 유도 |

### 8-2. Repair 루프

- 실패한 루브릭/스키마 항목만 지목해 수정 요청.  
  예: *"이전 응답에서 X 조건이 누락되었다. 동일 형식을 유지하며 X만 보완하라."*
- Repair 최대 2회, 무한 루프 방지.
- 실패한 루브릭 타입 로깅 → 전략/정책 개선에 활용.

---

## Part V. 테스트와 운영 지표

### 9. 테스트 전략

1. **Rule/메타데이터 테스트** — ActionType↔ActionCategory, TaskDomain×PromptCategory 허용 조합, GuidelineRule 존재 여부
2. **PromptSpec 구조 테스트** — 필수 섹션, 순서, 역할/톤/스타일 반영
3. **스냅샷/회귀 테스트** — PromptSpecRenderer 결과 문자열
4. **Golden Set (최소 50개)** — TaskDomain×ActionType 대표 샘플 + 기대 조건 체크리스트, 정책/전략 변경 시 통과율 비교
5. **오류 유형 분류** — 누락, 환각, 모순, 장황, 요구 불충족 → 전략 최적화에 활용

### 10. 운영 지표 (정확도 포함)

| 지표 | 의미 |
|------|------|
| First-pass pass rate | Verify 1회 만에 통과한 비율 |
| Repair rate | Repair가 한 번이라도 발생한 비율 |
| Repair success rate | Repair 후 최종 통과 비율 |
| Golden set pass rate | Golden set 회귀 통과율 |
| Error type distribution | 누락/환각/모순/장황/요구불충족 비율 |
| 스키마/형식 위반률 | Validator 스키마 실패 비율 |

---

## Part VI. 타겟 패키지 구조 (실행용)

```text
domain/prompt/
├── domain/
│   ├── model/
│   │   ├── PromptSpec.java
│   │   ├── PromptSection.java
│   │   ├── OutputContract.java
│   │   ├── Constraints.java
│   │   ├── ContentSandbox.java
│   │   └── QualityRubric.java
│   ├── value/
│   │   ├── TaskDomain.java
│   │   ├── PromptCategory.java
│   │   ├── ActionType.java / ActionCategory.java
│   │   ├── ToneType.java, StyleType.java, RoleType.java
│   │   ├── PromptObjective.java
│   │   ├── QualityPriority.java
│   │   ├── PromptingStrategy.java
│   │   └── PromptStrategyBundle.java
│   ├── policy/
│   │   ├── GuidelinePolicy.java
│   │   ├── GuidelineRule.java
│   │   └── *Guidelines.java
│   └── service/
│       ├── DomainResolver.java
│       ├── PromptSpecFactory.java
│       └── PromptSpecValidator.java
├── application/
│   ├── port/in/
│   │   ├── GeneratePromptCommand.java
│   │   └── GeneratePromptUseCase.java
│   ├── port/out/
│   │   ├── LLMClientPort.java
│   │   └── PromptSpecRendererPort.java
│   ├── service/
│   │   └── GeneratePromptService.java  // Clarify→Solve→Verify→Repair
│   └── dto/
├── adapter/
│   ├── in/web/
│   └── out/
│       ├── llm/ (OpenAIClientAdapter 등)
│       └── validation/ (JsonSchemaValidatorAdapter)
```

---

## Part VII. 실행 로드맵

| 단계 | 기간 | 작업 |
|------|------|------|
| 1단계 | 1~2주 | PromptObjective, QualityPriority, QualityRubric 추가; Verify 단계 추가 |
| 2단계 | — | PromptStrategyBundle 도입; CHECKLIST_VERIFY 등 기본 전략 적용 |
| 3단계 | — | Golden set 50개 구축; 회귀 지표 연결 |
| 4단계 | — | Edge-case/Counterexample 전략; 전략별 정확도 비교 실험 |

---

## Part VIII. 핵심 원칙 요약

- **구조·명확성**이 출력 품질을 향상시킨다 → 도메인에서 스펙을 결정한다.
- **정확도**는 길이·역할이 아니라 **Objective + Verify–Repair 루프 + 전략 조합 + Golden set**으로 올라가고 유지된다.
- **prompt 도메인**은 IO보다 정책/규칙/조합 로직이 복잡하므로, 도메인을 순수화할수록 테스트·변경 비용 대비 효과(ROI)가 커진다.

---

## 참고 문헌 및 추가 자원

| 번호 | 주제 | 논문/자료 | 링크 |
|------|------|-----------|------|
| 1 | Prompt 구조·생산성 | *Prompt Engineering and the Effectiveness of Large Language Models...* | [ResearchGate](https://www.researchgate.net/publication/391690485_Prompt_Engineering_and_the_Effectiveness_of_Large_Language_Models_in_Enhancing_Human_Productivity) |
| 2 | 프롬프트 기법 서베이 | *Prompt Engineering Practices for Large Language Models: A Systematic Survey* | [arXiv:2507.18638](https://arxiv.org/abs/2507.18638) |
| 3 | Constrained Decoding | *Generating Structured Outputs from Language Models* | [arXiv HTML](https://arxiv.org/html/2501.10868v1) |
| 4 | 기법·평가 한계 | *A Systematic Survey of Prompt Engineering in LLMs* | [서베이 요약(한글)](https://www.themoonlight.io/ko/review/a-systematic-survey-of-prompt-engineering-in-large-language-models-techniques-and-applications) |

**추가 자원:** OpenAI Structured Outputs 가이드, GitHub `awesome-prompt-engineering`, Scholar: `prompt engineering systematic literature review`
