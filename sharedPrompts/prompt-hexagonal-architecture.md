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

### 3. 다양한 기법과 "길이 vs 품질"

- Zero-shot, Few-shot, CoT, 역할/페르소나, 단계별 지시 등 여러 기법이 존재하며, **목적/작업에 따라 조합**해야 한다 (*Systematic Survey*, arXiv:2507.18638 — 29개+ 기법 분류).
- **긴 프롬프트가 항상 좋은 것은 아니다.** 핵심 목적·제약·우선순위가 명확한 것이 더 중요하다.
- **Self-Consistency**: 동일 입력에 대해 N회 실행 → 다수결로 신뢰도 향상. 논리 추론(`REASONING`)·사실 검증(`FACTUAL`)에 유효.
- **Tree-of-Thoughts (ToT)**: 탐색적 추론 트리로 복수 경로 평가. 단계 일관성이 중요한 `PLANNING` objective에 적합.
- **Chain-of-Verification**: 초안 생성 → 자기 검증 질문 생성 → 재답변 → 수정. `Verify→Repair` 루프의 연구 근거.
- **Constrained Decoding** (arXiv:2501.10868): JSON Schema 기반 토큰 마스킹으로 무효 출력 원천 차단. 무제약 대비 **속도 ~50% 향상**, 품질 최대 4% 향상. 단, 프레임워크별 compliance rate 편차가 최대 2배이므로 adapter 선택이 중요.

**적용:** 길이가 아니라 **구조/우선순위**를 `PromptSpec`으로 관리하고, 인프라는 스펙을 실행만 한다. `PromptStrategyBundle`은 위 기법들을 `PromptObjective`에 따라 조합·고정한다.

### 4. 역할/페르소나와 품질

- 역할 부여는 스타일/톤/관점 조절에 유용하지만, **작업 유형과 설계 방식**에 따라 효과가 다르다.

**적용:** `RoleType`, `ToneType`, `StyleType`을 enum·도메인 값으로 정규화하고, 가이드라인을 도메인 레이어에 정의한다.

### 5. 정확도는 "느낌"이 아니다

- 정확도는 프롬프트를 길게 쓰거나 역할만 줘서 올라가지 **않는다**.
- **명확한 Objective**, **Solve → Verify → Repair** 루프, **전략 조합·실험**, **Golden set 회귀 테스트**가 있을 때 올라가고 유지된다.

**적용:** `PromptObjective`, `QualityRubric`, 4단계 유즈케이스, Validator 확장, Repair 루프, Golden set을 도메인·애플리케이션에 명시한다.

---

## Part Ia. 설계 리스크 `NEW`

> 이 시스템은 연구형 정확도 플랫폼이 아니라 **UX-우선 프롬프트 생성 서비스**다.
> 다음 리스크는 설계 단계에서 명문화하고 구조적 가드레일로 통제한다.

### R-1. 전략 조합 폭증

**왜 문제인가:** Objective(5) × StrategyBundle(N) × Experimental(M) 조합이 제한 없이 늘어나면 디버깅·테스트·운영 비용이 기하급수적으로 증가한다. 전략 수가 10개를 넘는 순간 조합 경우의 수는 1,000+를 초과한다.

**통제 정책:** Objective당 허용 StrategyBundle 최대 3개 하드캡.

**가드레일:** 초과 Bundle 등록 시 빌드 타임 또는 애플리케이션 시작 시점에 오류 발생. 런타임 조합은 허용하지 않는다.

### R-2. Experimental 전략의 호출 증가

**왜 문제인가:** SELF_CONSISTENCY(+2~4 호출)·Tree-of-Thoughts(+2~4 호출)를 동시에 활성화하면 단일 요청이 최대 8회 LLM 호출을 유발한다. 트래픽 증가 시 비용·지연이 선형이 아닌 복수 배로 폭증한다.

**통제 정책:** Experimental 전략 동시 활성화 1개 제한. 실험 플래그(`experimentalStrategies`) 미설정 시 전략 자동 스킵.

**가드레일:** 2개 이상 Experimental 요청 시 우선순위 낮은 전략 자동 비활성화 + 경고 로그 기록.

### R-3. Verify-Repair 루프의 지연 증가

**왜 문제인가:** Repair 2회 × LLM 호출 시간을 포함하면 최악 케이스에서 최초 응답 대비 5~6× 지연이 발생한다. CREATIVE처럼 정답 기준이 모호한 Objective에서 Repair를 강하게 돌리면 지연만 늘고 품질 개선 효과가 작다.

**통제 정책:** Objective별 Verify 강도 차등화 (Part IV §8-2 참조). Repair 2회 상한 절대 고수.

**가드레일:** 상한 초과 시 현재 결과를 반환하고 Repair 실패를 로깅한다. 무한 루프 방지 로직을 GeneratePromptService에 명시적으로 구현한다.

**`NEW` 하드캡 async fallback:** LLM 호출 상한 초과 시 완전 차단이 아닌 2단계 응답을 사용한다.
1. 동기 응답: Core 전략만 적용한 결과를 즉시 반환 → UX에 "✅ 빠른 생성" 배지 표시
2. 비동기 재검증: 백그라운드에서 Experimental 전략 적용 → 개선 버전 저장 → "개선 버전 사용 가능" 알림

### R-4. Golden Set 분포와 실제 트래픽 불일치

**왜 문제인가:** 수동으로 구성한 50~140개 샘플이 특정 Objective나 도메인에 편향될 경우, Golden set pass rate가 높아도 실제 트래픽에서 오류율이 높은 사각지대가 발생한다.

**통제 정책:** Static Golden Set(회귀 방지) + Traffic-weighted Dynamic Set(최근 30일 실제 사용 기반) 이원화 (Part V §9 참조).

**가드레일:** Dynamic Set과 Static Set의 Objective 분포 편차가 임계값(예: ±20%p)을 초과하면 알림 발송.

### R-5. Constrained Decoding 벤더 의존성

**왜 문제인가:** 프레임워크별 compliance rate 편차가 최대 2배에 달한다. 벤더 업데이트나 API 정책 변경 시 EXTRACTION 정확도가 예고 없이 저하될 수 있다.

**통제 정책:** Constrained Decoding 구현을 adapter 인터페이스(`ConstrainedDecodingPort`) 뒤에 격리한다.

**가드레일:** 벤더별 compliance rate를 지표로 수집하고, 임계값(예: 90%) 이하로 떨어지면 대체 adapter로 전환하는 fallback 정책을 명시한다.

---

## Part II. 목표와 도메인 정의

### 0. 목표

- **헥사고날 아키텍처:** 도메인은 순수 자바, 애플리케이션은 유즈케이스·포트, 인프라는 어댑터로 분리.
- **정확도 정의:** 도메인에 `PromptObjective`, `QualityPriority`, `QualityRubric`을 두어 정확도를 enum·정책으로 고정.
- **품질 루프:** Clarify → Solve → Verify → Repair 4단계와 2회 제한 Repair로 품질을 제어·측정.

### 0-1. UX-우선 원칙 `NEW`

> 이 시스템은 사용자가 정확도 엔진을 직접 조작하는 연구 도구가 아니다.

| 원칙 | 내용 |
|------|------|
| **자동 매핑** | 사용자는 PromptObjective·Strategy·QualityPriority를 직접 선택하지 않는다. 내부 엔진이 사용자 입력(자연어·메타데이터)으로부터 자동 매핑한다. |
| **지표 은닉** | pass rate, repair rate 등 품질 지표는 내부 측정용이며, UX에는 "검증 완료" 형태로만 노출한다. |
| **레이어 분리** | 정확도 엔진(domain·application)은 UI(adapter)와 엄격히 분리된다. adapter는 결과만 받아 사용자에게 전달한다. |

### 0-2. 정확도 관련 도메인 모델

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
  - LLMClientPort, PromptSpecRendererPort, ConstrainedDecodingPort

domain.prompt.adapter
  - in: HTTP/메시징 → Command/Result  (UX: "검증 완료" 응답만 노출)
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
3. **Verify** — 루브릭·스키마 검증 (Objective별 강도 차등화, Part IV §8-2 참조)
4. **Repair** — 실패 항목만 수정 요청 (최대 2회, 실패 루브릭 로깅)

### 7-3. 전략 조합: PromptStrategyBundle `UPDATED`

**기존:** 단일 `PromptingStrategy` enum
**변경:** 조합 가능한 전략 집합 + 구조적 상한 제약

```java
class PromptStrategyBundle {
    Set<PromptingStrategy> strategies;
}
```

**전략 3계층 분류**

전략이 많아지면 비용·디버깅 난이도가 폭발한다. 계층을 분리해 기본 활성화 범위를 제한한다.

| 계층 | 전략 | LLM 추가 호출 | 활성화 조건 |
|------|------|-------------|------------|
| **Core** (항상 적용) | `CLARIFY_FIRST`, `STEP_BY_STEP`, `CHECKLIST_VERIFY` | 0 | 모든 Objective |
| **Objective-specific** (조건부) | `DECOMPOSITION`, `CITE_OR_UNCERTAIN`, `REQUIRE_JUSTIFICATION`, `FEW_SHOT_EXEMPLAR`, `CHAIN_OF_VERIFICATION`, `EDGE_CASE_SCAN` | +1~2 | Objective 매핑에 따라 고정 |
| **Experimental** (A/B 실험만) | `SELF_CONSISTENCY`, `TREE_OF_THOUGHTS` | +2~4 | 실험 플래그로만 활성화 |

**전략 구조 제약 `NEW`**

| 제약 | 내용 | 위반 시 처리 |
|------|------|------------|
| Bundle 수 상한 | Objective당 허용 StrategyBundle 최대 **3개** | 빌드 타임 오류 또는 애플리케이션 시작 시 예외 |
| Experimental 동시 제한 | Experimental 전략 동시 **1개** | 우선순위 낮은 전략 자동 비활성화 + 경고 로그 |
| LLM 호출 상한 | Objective별 최대 호출 수 (아래 표) | 상한 초과 전략 자동 비활성화, Core 전략만 적용 |

**PromptObjective ↔ 기본 전략 매핑 및 호출 상한 `UPDATED`**

| PromptObjective | Core | Objective-specific | **최대 LLM 호출** |
|-----------------|------|--------------------|-----------------|
| `FACTUAL` | CLARIFY_FIRST + STEP_BY_STEP | CHAIN_OF_VERIFICATION + CITE_OR_UNCERTAIN | **≤ 3** |
| `REASONING` | CLARIFY_FIRST + STEP_BY_STEP | REQUIRE_JUSTIFICATION | **≤ 2** |
| `EXTRACTION` | CLARIFY_FIRST | OutputContract strict schema (Constrained Decoding) | **≤ 1** |
| `PLANNING` | CLARIFY_FIRST + STEP_BY_STEP | DECOMPOSITION + EDGE_CASE_SCAN | **≤ 2** |
| `CREATIVE_WITH_CONSTRAINTS` | CLARIFY_FIRST + CHECKLIST_VERIFY | FEW_SHOT_EXEMPLAR | **≤ 2** |

**Experimental 전략 승격 기준 `UPDATED`**

> 정확도 상승만으로 승격하지 않는다. 비용·지연을 함께 측정한다.

```
승격 점수 = 정확도 상승률 / (호출 증가 가중치 + 지연 증가 가중치)

  - 정확도 상승률  : A/B pass rate 차이 (%)
  - 호출 증가 가중치: (실험군 평균 호출 수 - 대조군 평균 호출 수) × α   [α = 0.4]
  - 지연 증가 가중치: (실험군 평균 응답시간 - 대조군 평균 응답시간)
                      / 기준 지연 × β                                  [β = 0.6]

승격 조건: 승격 점수 > 임계값 (초기값 1.0, 운영 중 조정 가능)
```

**`NEW` 티어별 가중치 분리**

| 플랜 | α (호출 가중치) | β (지연 가중치) | 특성 |
|------|--------------|--------------|------|
| 무료 | 0.6 | 0.4 | 비용 우선 — 호출 증가에 더 민감 |
| 유료 | 0.3 | 0.3 | 정확도 우선 — 승격 기준 완화 |

`StrategyPromotionPolicy`에서 티어별 가중치를 주입받아 수식에 적용한다.

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
| 환각 탐지 | 검증 불가능한 수치·인용·고유명사 주장 감지 → Repair 유도 |

### 8-2. Verify 선택적 실행 정책 `NEW`

> **비용 기반 Verify 선택 정책:** 모든 Objective에 동일 강도의 Verify를 적용하지 않는다.
> Verify 강도가 높을수록 지연이 증가하므로, 정확도 효과가 낮은 Objective에는 경량 검증을 적용한다.

| Objective | Verify 모드 | 구체적 적용 |
|-----------|-----------|-----------|
| `FACTUAL` | **CoV 강화** | Chain-of-Verification 전체 실행, 사실·수치·인용 검증 우선. Repair 유도 기준 가장 엄격. |
| `REASONING` | **표준** | 루브릭 Coverage + 모순 탐지. 표준 Verify 루프 적용. |
| `EXTRACTION` | **Schema 우선** | JSON Schema 기반 검증 우선. Schema 통과 시 LLM Verify 생략 가능. 비용 최소화. |
| `PLANNING` | **표준** | 단계 순서·선행조건 커버리지 확인. |
| `CREATIVE_WITH_CONSTRAINTS` | **Soft-verify** | 형식·톤 준수 + 명백한 모순 여부만 체크. 의미적 정답 기준 없음. Repair 유도 기준 가장 관대. |

**Soft-verify 체크리스트 `NEW`** (CREATIVE_WITH_CONSTRAINTS 전용)

| 항목 | 적용 | 비고 |
|------|------|------|
| 지정 형식 준수 | 필수 | 형식 미지정 시 생략 |
| 금지어·내용 정책 위반 | 필수 | ContentSandbox 기준 |
| 명백한 논리 모순 | 선택 | A이다 + A가 아니다 패턴만 |
| 의미적 창의성 판단 | **제외** | 정답 기준 없음 |
| 스타일·톤 평가 | **제외** | 주관적 판단 제외 |

> Soft-verify 항목 외 이유로 Repair를 유도하지 않는다.

### 8-3. Repair 루프

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

#### 9-1. Golden Set 이원화 `NEW`

> 단일 Golden Set은 트래픽 분포 불일치 리스크(R-4)를 야기한다. 두 종류로 분리한다.

| 종류 | 목적 | 갱신 방식 | 규모 |
|------|------|---------|------|
| **Static Golden Set** (회귀 방지용) | 정책·전략 변경 시 기준선 pass rate 비교, 회귀 감지 | 수동 (PR 단위) | MVP 50개 → 장기 140개 |
| **Traffic-weighted Dynamic Set** (분포 보정용) | 최근 30일 실제 트래픽 샘플 반영, Static Set의 분포 편향 교정 | 자동 (30일 롤링 샘플링) | Objective별 최소 20개 유지 |

- Static MVP: Objective별 10개 × 5 = 50개 / 장기: FACTUAL 30 / REASONING 30 / EXTRACTION 30 / PLANNING 30 / CREATIVE 20 = 140개
- 회귀 테스트 자동화: Static Golden Set 통과율 기준선 비교
- Dynamic Set과 Static Set 분포 편차 임계값(±20%p) 초과 시 알림
- **오류 유형 분류** — 누락, 환각, 모순, 장황, 요구 불충족 → 전략 최적화에 활용

### 10. 운영 지표 (정확도 포함)

| 지표 | 의미 |
|------|------|
| First-pass pass rate | Verify 1회 만에 통과한 비율 |
| Repair rate | Repair가 한 번이라도 발생한 비율 |
| Repair success rate | Repair 후 최종 통과 비율 |
| Golden set pass rate (Static) | Static Golden Set 회귀 통과율 |
| Golden set pass rate (Dynamic) | Traffic-weighted Dynamic Set 통과율 |
| Error type distribution | 누락/환각/모순/장황/요구불충족 비율 |
| 스키마/형식 위반률 | Validator 스키마 실패 비율 |
| **평균 LLM 호출 횟수 / 요청** | Objective별 실제 호출 수 추적 (비용 기준선) |
| **`NEW` Constrained Decoding compliance rate** | 벤더별 Schema 준수율 (R-5 가드레일 근거) |
| **`NEW` Verify 비용 대비 Repair 감소율** | Verify 강도 조정 효과 측정 |
| **`UPDATED` Experimental 전략 승격률** | 3축 승격 점수 통과 후 Core/Objective-specific으로 승격된 전략 비율 |

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
│   │   ├── QualityRubric.java
│   │   ├── PromptVersion.java           ← NEW: 버전 저장 모델
│   │   ├── PromptUsageHistory.java      ← NEW: 사용 이력
│   │   └── PromptBookmark.java          ← NEW: 즐겨찾기
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
│   │   ├── StrategyPromotionPolicy.java  ← NEW: 티어별 가중치 관리
│   │   └── *Guidelines.java
│   └── service/
│       ├── DomainResolver.java
│       ├── PromptSpecFactory.java
│       └── PromptSpecValidator.java
├── application/
│   ├── port/in/
│   │   ├── GeneratePromptCommand.java
│   │   ├── GeneratePromptUseCase.java
│   │   ├── GetPromptHistoryUseCase.java  ← NEW
│   │   ├── RecommendPromptUseCase.java   ← NEW
│   │   └── BookmarkPromptUseCase.java    ← NEW
│   ├── port/out/
│   │   ├── LLMClientPort.java
│   │   ├── PromptSpecRendererPort.java
│   │   ├── ConstrainedDecodingPort.java  ← 벤더 의존성 격리
│   │   └── SavePromptVersionPort.java    ← NEW
│   ├── service/
│   │   ├── GeneratePromptService.java    // Clarify→Solve→Verify(선택적)→Repair + async fallback
│   │   └── PromptLifecycleService.java   ← NEW: 버전·이력·추천
│   └── dto/
├── adapter/
│   ├── in/web/
│   │   └── BadgeResponseAssembler.java   ← NEW: Result → Badge DTO 변환
│   └── out/
│       ├── llm/ (OpenAIClientAdapter 등)
│       └── validation/ (JsonSchemaValidatorAdapter)
```

---

## Part VII. 실행 로드맵

| 단계 | 기간 | 작업 |
|------|------|------|
| 1단계 | 1~2주 | PromptObjective, QualityPriority, QualityRubric 추가; Verify 단계 추가; 전략 3계층 정의; **UX-우선 원칙·레이어 분리 적용** |
| 2단계 | — | PromptStrategyBundle 도입; Core + Objective-specific 전략 적용; **Objective별 호출 상한·Bundle 3개 가드레일 구현**; 평균 LLM 호출 지표 수집 시작 |
| 3단계 | — | **Static Golden Set** MVP 50개 구축; 환각 탐지 Validator 연결; **Verify 선택적 실행 정책·Soft-verify 기준 적용**; **하드캡 async fallback 구현**; `ConstrainedDecodingPort` 격리; 회귀 지표 연동 |
| 4단계 | — | SELF_CONSISTENCY·TREE_OF_THOUGHTS A/B 실험; **3축 승격 점수 + 티어별 가중치 측정**; **Traffic-weighted Dynamic Set 구축**; Golden set 140개로 확장 |
| **5단계** | — | **Result Badge UX 구현** (`BadgeResponseAssembler`); **PromptLifecycle 도메인 구축** (`PromptVersion`, `PromptUsageHistory`); 성공률 기반 추천 서비스 |
| **6단계** | — | 버전 diff API; **즐겨찾기 → Dynamic Golden Set 연동**; 재실행 이력 기반 "개선 버전 사용 가능" 알림 |

---

## Part VIII. 핵심 원칙 요약

- **구조·명확성**이 출력 품질을 향상시킨다 → 도메인에서 스펙을 결정한다.
- **정확도**는 길이·역할이 아니라 **Objective + Verify–Repair 루프 + 전략 조합 + Golden set**으로 올라가고 유지된다.
- **prompt 도메인**은 IO보다 정책/규칙/조합 로직이 복잡하므로, 도메인을 순수화할수록 테스트·변경 비용 대비 효과(ROI)가 커진다.
- **`NEW` UX-우선:** 사용자는 정확도 엔진을 직접 조작하지 않는다. 엔진은 자동 매핑하고, 품질 지표는 내부 측정용이며, UX에는 배지("✅ 조건 반영됨", "✅ 형식 검증 완료" 등)로만 노출한다.
- **`NEW` 비용·지연·정확도 3축 균형:** 전략 승격은 정확도 단독이 아니라 3축 승격 점수로 결정한다. Experimental 전략은 동시 1개, Objective당 Bundle 최대 3개, 호출 상한을 구조적으로 강제한다.
- **`NEW` 플랫폼 지속성:** 정확도 엔진(A)만으로는 사용자가 계속 쓰는 제품이 되지 않는다. PromptLifecycle(버전·이력·추천·즐겨찾기)이 사용자를 서비스에 머물게 하는 B 레이어다. A와 B는 헥사고날 구조 안에서 분리된 책임을 갖는다.

---

## Part IX. UX Output Layer — Result Badge `NEW`

> 품질 지표는 내부에서 측정하고, 사용자에게는 배지로만 전달한다.
> 배지 변환 책임은 adapter/in/web 전용이며, 도메인·애플리케이션 레이어를 침범하지 않는다.

### 9-1. 배지 타입 정의

```java
// adapter/in/web 전용 — 도메인 레이어에 두지 않는다
record QualityBadge(BadgeType type, boolean passed) {}

enum BadgeType {
    CONDITIONS_MET,        // Coverage 검사 통과 → "✅ 조건 반영됨"
    FORMAT_VERIFIED,       // Schema/OutputContract 통과 → "✅ 형식 검증 완료"
    NO_PROHIBITED_CONTENT, // ContentSandbox 통과 → "✅ 금지어 없음"
    FAST_GENERATION,       // Repair 0회 → "⚡ 빠른 생성"
    REVERIFIED             // Repair 1~2회 통과 → "🔄 재검증 완료" (횟수 숨김)
}
```

### 9-2. 책임 분리

| 레이어 | 책임 |
|--------|------|
| domain/application | `GeneratePromptResult` 생성 (pass rate, repairCount, rubric 결과 포함) |
| adapter/in/web | `BadgeResponseAssembler`가 Result → `List<QualityBadge>` 변환 후 응답 DTO 조립 |
| UX | 배지 렌더링. 수치(pass rate, repair count)는 절대 노출하지 않는다. |

---

## Part X. 프롬프트 라이프사이클 도메인 `NEW`

> 사용자가 계속 쓰는 플랫폼(B)을 위한 유지·재사용 설계다.

### 10-1. 도메인 모델

```java
class PromptVersion {
    VersionId id;
    PromptObjective objective;
    PromptStrategyBundle strategyBundleSnapshot; // 생성 당시 스냅샷
    double passRate;                             // 내부 측정값 (UX 비노출)
    Instant createdAt;
}

class PromptUsageHistory {
    UserId userId;
    VersionId promptVersionId;
    List<BadgeType> badgesGranted;
    Instant usedAt;
}

class PromptBookmark {
    UserId userId;
    VersionId promptVersionId;
    Instant bookmarkedAt;
}
```

### 10-2. 유즈케이스

| 유즈케이스 | 설명 |
|-----------|------|
| `SavePromptVersionPort` (out) | 생성 직후 버전 저장 |
| `GetPromptHistoryUseCase` (in) | 사용자 이력 조회 (최근 N개) |
| `RecommendPromptUseCase` (in) | 동일 TaskDomain + PromptCategory에서 pass rate 상위 추천 |
| `BookmarkPromptUseCase` (in) | 즐겨찾기 등록·해제 |

### 10-3. 추천 기준

```
추천 점수 = Static Golden Set pass rate (가중 0.6)
           + 최근 30일 사용 빈도 정규화 (가중 0.4)

추천 조건: pass rate ≥ 85% + 추천 점수 상위 5개
```

### 10-4. 즐겨찾기 → Dynamic Golden Set 연동

북마크된 프롬프트는 Traffic-weighted Dynamic Set의 후보로 우선 샘플링된다.
이를 통해 사용자 피드백이 품질 엔진 개선으로 순환된다.

---

## 참고 문헌 및 추가 자원

| 번호 | 주제 | 논문/자료 | 링크 |
|------|------|-----------|------|
| 1 | Prompt 구조·생산성 | *Prompt Engineering and the Effectiveness of Large Language Models...* | [ResearchGate](https://www.researchgate.net/publication/391690485_Prompt_Engineering_and_the_Effectiveness_of_Large_Language_Models_in_Enhancing_Human_Productivity) |
| 2 | 프롬프트 기법 서베이 | *Prompt Engineering Practices for Large Language Models: A Systematic Survey* | [arXiv:2507.18638](https://arxiv.org/abs/2507.18638) |
| 3 | Constrained Decoding | *Generating Structured Outputs from Language Models* | [arXiv HTML](https://arxiv.org/html/2501.10868v1) |
| 4 | 기법·평가 한계 | *A Systematic Survey of Prompt Engineering in LLMs* | [서베이 요약(한글)](https://www.themoonlight.io/ko/review/a-systematic-survey-of-prompt-engineering-in-large-language-models-techniques-and-applications) |

**추가 자원:** OpenAI Structured Outputs 가이드, GitHub `awesome-prompt-engineering`, Scholar: `prompt engineering systematic literature review`
