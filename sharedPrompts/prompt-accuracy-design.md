# Prompt 정확도 설계 체크리스트

전체 설계·연구 근거·패키지 구조는 **`prompt-hexagonal-architecture.md`** 를 참고한다.
이 문서는 정확도 중심 작업만 빠르게 점검할 때 쓰는 체크리스트다.

---

## R. 설계 리스크 `NEW`

> 이 시스템은 연구형 플랫폼이 아니라 **UX-우선 프롬프트 생성 서비스**다.
> 다음 리스크를 설계 단계에서 명문화하고 가드레일로 통제한다.

| 리스크 | 왜 문제인가 | 통제 정책 | 가드레일 |
|--------|------------|-----------|---------|
| **전략 조합 폭증** | Objective × Strategy × Bundle 조합이 기하급수적으로 늘면 디버깅·테스트 비용 폭발 | Objective당 허용 StrategyBundle 최대 3개 하드캡 | 초과 시 Bundle 등록 거부 (빌드 타임 검증) |
| **Experimental 호출 증가** | SELF_CONSISTENCY·ToT는 +2~4 호출 → 비용·지연 즉시 증가 | Experimental 동시 활성화 1개 제한, 실험 플래그 없으면 기본 비활성화 | `experimentalStrategies` 플래그 미설정 시 Experimental 전략 자동 스킵 |
| **Verify-Repair 지연** | 2회 Repair × LLM 호출 시간 = 최악 5~6× 지연 | Objective별 Verify 강도 차등화 (§3 참조), Repair 2회 상한 고수 | 상한 초과 시 현재 결과 반환 + Repair 실패 로깅 |
| **Golden set 분포 불일치** | 수동 구성 샘플이 실제 트래픽과 다를 경우 회귀 지표 신뢰성 저하 | Static + Traffic-weighted Dynamic Set 이원화 (§6 참조) | Dynamic Set 분포 이탈 감지 시 경보 |
| **Constrained Decoding 벤더 의존** | 프레임워크별 compliance rate 최대 2× 편차, 벤더 업데이트 시 동작 변경 위험 | adapter 인터페이스 뒤에 격리 | 벤더별 compliance rate 지표 별도 수집·임계 알림 |
| **하드캡 vs 실사용 충돌** | 복잡한 FACTUAL 작업에서 상한이 실제 필요 호출 수보다 낮을 수 있음 | 상한 초과 시 완전 차단 아닌 async fallback — 동기 응답은 Core 전략으로 빠르게, 비동기로 Experimental 재검증 후 개선 버전 저장 | UX: 즉시 응답 → "개선 버전 사용 가능" 배지로 알림 |
| **Soft-verify 기준 모호성** | CREATIVE soft-verify가 구체적으로 무엇인지 불명확하면 운영 중 판단 불일치 발생 | 체크리스트 명문화 (§3 참조) | Soft-verify 항목 외 Repair 유도 금지 |
| **승격 가중치 단일값 고정** | 무료/유료 플랜별 비용 민감도가 다른데 동일 α·β 적용 시 플랜 전략과 충돌 | `StrategyPromotionPolicy`에 티어별 가중치 맵으로 분리 | 운영 중 티어별 가중치 독립 조정 가능 |

---

## 0. 목표 정의

### 0-1. UX-우선 원칙 `NEW`

> 사용자는 PromptObjective·Strategy·QualityPriority를 **직접 선택하지 않는다**.
> 내부 엔진이 사용자 입력으로부터 자동 매핑한다.
> 품질 지표(pass rate, repair rate 등)는 **내부 측정용**이며, UX에는 **"검증 완료"** 형태로만 노출한다.
> 정확도 엔진(도메인·애플리케이션 레이어)은 UI(어댑터 레이어)와 엄격히 분리된다.

### 0-2. 내부 도메인 모델

- [ ] **PromptObjective** enum 도입 (FACTUAL, ANALYTICAL, REASONING, EXTRACTION, PLANNING, CREATIVE_WITH_CONSTRAINTS)
- [ ] **QualityPriority** enum 도입 (ACCURACY_FIRST, STRUCTURE_FIRST, BREVITY_SECOND, CREATIVITY_SECOND)
- [ ] **QualityRubric** 도메인 규칙 정의 (필수 요구사항, 입력 유지, 숫자/엔티티 보존, 모순 없음, 근거/불확실성 명시)

---

## 1. 아키텍처

- [ ] **PromptSpec**에 `objective`, `qualityPriority`, `rubric` 필드 추가
- [ ] **GeneratePromptUseCase** 4단계: Clarify → Solve → Verify → Repair
- [ ] **Repair** 최대 2회, 실패 루브릭 로깅

---

## 2. 전략

- [ ] **PromptStrategyBundle** 도입 (전략 조합)
- [ ] 전략을 3계층으로 분류해 기본 활성화 범위 제한

### 2-1. 전략 3계층 정의

| 계층 | 전략 목록 | LLM 추가 호출 |
|------|---------|-------------|
| **Core** (항상 적용) | CLARIFY_FIRST, STEP_BY_STEP, CHECKLIST_VERIFY | 0 |
| **Objective-specific** (조건부) | DECOMPOSITION, CITE_OR_UNCERTAIN, REQUIRE_JUSTIFICATION, FEW_SHOT_EXEMPLAR, CHAIN_OF_VERIFICATION, EDGE_CASE_SCAN | +1~2 |
| **Experimental** (A/B 실험만) | SELF_CONSISTENCY, TREE_OF_THOUGHTS | +2~4 |

- [ ] Experimental 전략은 기본 비활성화 — `experimentalStrategies` 플래그로만 활성화
- [ ] **`NEW`** Objective당 허용 StrategyBundle **최대 3개** (초과 시 빌드 타임 거부)
- [ ] **`NEW`** Experimental 전략 **동시 1개** 제한 (동시 2개 이상 요청 시 우선순위 낮은 것 자동 비활성화)
- [ ] A/B 결과(비용-정확도-지연 **3축 승격 점수**)로만 Core/Objective-specific 승격 결정 (§7 승격 수식 참조)
- [ ] TaskDomain / PromptCategory별 기본 전략 집합을 GuidelinePolicy와 연결

### 2-2. Objective별 기본 전략 매핑 및 호출 상한 `NEW`

> 호출 상한을 초과하는 전략 조합은 **자동 비활성화**된다.
> 초과 전략은 실험 로그에 기록되고, 해당 요청에서는 Core 전략만 적용된다.

| Objective | 기본 전략 | **최대 LLM 호출 상한** |
|-----------|----------|-----------------------|
| `FACTUAL` | CHAIN_OF_VERIFICATION + CITE_OR_UNCERTAIN | **≤ 4** |
| `ANALYTICAL` | CHAIN_OF_VERIFICATION + CITE_OR_UNCERTAIN | **≤ 4** |
| `REASONING` | STEP_BY_STEP + REQUIRE_JUSTIFICATION | **≤ 2** |
| `EXTRACTION` | OutputContract strict schema + Constrained Decoding adapter | **≤ 1** |
| `PLANNING` | DECOMPOSITION + EDGE_CASE_SCAN | **≤ 3** |
| `CREATIVE_WITH_CONSTRAINTS` | FEW_SHOT_EXEMPLAR + CHECKLIST_VERIFY | **≤ 2** |

---

## 3. Verify 선택적 실행 `NEW`

> **비용 기반 Verify 선택 정책:** 모든 Objective에 동일 강도의 Verify를 적용하지 않는다.

| Objective | Verify 모드 | 적용 방식 |
|-----------|-----------|---------|
| `FACTUAL` | **CoV 강화** | Chain-of-Verification 전체 실행, 사실·수치·인용 검증 우선 |
| `ANALYTICAL` | **표준** (또는 별도 정의) | 분석형 추론 일관성·근거 연결성 검증 |
| `REASONING` | **표준** | 루브릭 Coverage + 모순 탐지 |
| `EXTRACTION` | **Schema 우선** | JSON Schema 기반 검증 우선, 통과 시 LLM Verify 생략 가능 |
| `PLANNING` | **표준** | 단계 순서·선행조건 커버리지 |
| `CREATIVE_WITH_CONSTRAINTS` | **Soft-verify** | 형식·톤 준수 + 명백한 모순 여부만 체크 (의미적 정답 기준 없음) |

**Soft-verify 체크리스트 `NEW`** (CREATIVE_WITH_CONSTRAINTS 전용)

| 항목 | 적용 | 비고 |
|------|------|------|
| 지정 형식 준수 | 필수 | 형식 미지정 시 생략 |
| 금지어·내용 정책 위반 | 필수 | ContentSandbox 기준 |
| 명백한 논리 모순 | 선택 | A이다 + A가 아니다 패턴만 |
| 의미적 창의성 판단 | **제외** | 정답 기준 없음 |
| 스타일·톤 평가 | **제외** | 주관적 판단 제외 |

> Soft-verify 항목 외 이유로 Repair를 유도하지 않는다.

---

## 4. Validator 확장

- [ ] Coverage 검사 (요구사항·필수 섹션 누락)
- [ ] 입력 조건 유지 검사 (숫자/조건/엔티티 보존)
- [ ] 모순 탐지 (A이다 + A가 아니다, 확정 표현 남용)
- [ ] 불확실성 처리 (근거 없이 단정 → Repair 유도)
- [ ] 환각 탐지 (검증 불가능한 수치·인용·고유명사 주장 감지 → Repair 유도)

---

## 5. Repair

- [ ] 실패 항목만 수정 요청 (형식 유지, X만 보완)
- [ ] 2회 제한 + 실패 루브릭 타입 로깅

---

## 6. 테스트

### 6-1. Golden Set 이원화 `NEW`

> 단일 Golden Set은 트래픽 분포 불일치 리스크를 야기한다. 두 종류로 분리한다.

| 종류 | 목적 | 갱신 주기 | 규모 |
|------|------|---------|------|
| **Static Golden Set** (회귀 방지용) | 정책·전략 변경 시 기준선 통과율 비교, 회귀 감지 | 수동 (PR 단위) | MVP 60개 → 장기 160개+ |
| **Traffic-weighted Dynamic Set** (분포 보정용) | 최근 30일 실제 트래픽 샘플 반영, Static Set의 분포 편향 교정 | 자동 (30일 롤링) | Objective별 최소 20개 유지 |

- [ ] Static MVP: Objective별 10개 × 6 = 60개 / 장기: FACTUAL 30 / ANALYTICAL 20 / REASONING 30 / EXTRACTION 30 / PLANNING 30 / CREATIVE 20 = 160개
- [ ] 회귀 테스트 자동화: Static Golden Set 통과율 기준선 비교
- [ ] 오류 유형 분류 (누락, 환각, 모순, 장황, 요구 불충족)

---

## 7. 운영 지표

- [ ] First-pass pass rate
- [ ] Repair rate / Repair success rate
- [ ] Golden set pass rate (**Static / Dynamic 분리 측정**)
- [ ] Error type distribution
- [ ] **평균 LLM 호출 횟수 / 요청** (Objective별 — 비용 기준선)
- [ ] **Experimental 전략 승격률** (3축 승격 점수 통과 후 승격된 전략 비율)
- [ ] **`NEW`** Constrained Decoding 벤더별 compliance rate
- [ ] **`NEW`** Verify 비용 대비 Repair 감소율 (Verify 강도 조정 근거)

### 7-1. Experimental 전략 승격 기준 (3축 승격 모델) `NEW`

> 정확도 상승만으로 승격하지 않는다. 비용·지연을 함께 고려한다.

```text
승격 점수 = 정확도 상승률 / max(호출 증가 가중치 + 지연 증가 가중치, ε)

  - 정확도 상승률  : A/B pass rate 차이 (%)
  - 호출 증가 가중치: (실험군 평균 호출 수 - 대조군 평균 호출 수) × α   [α = 0.4]
  - 지연 증가 가중치: (실험군 평균 응답시간 - 대조군 평균 응답시간)
                      / 기준 지연 × β                                  [β = 0.6]

  - ε: 아주 작은 양수 (예: 0.1). 분모가 0 이하인 경우 ε를 사용해 점수 폭주를 방지한다.

승격 조건: 승격 점수 > 임계값 (초기값 1.0, 운영 중 조정 가능)
```

**`NEW` 티어별 가중치 분리**

| 플랜 | α (호출 가중치) | β (지연 가중치) | 특성 |
|------|--------------|--------------|------|
| 무료 | 0.6 | 0.4 | 비용 우선 — 호출 증가에 더 민감 |
| 유료 | 0.3 | 0.3 | 정확도 우선 — 승격 기준 완화 |

`StrategyPromotionPolicy`에서 티어별 가중치를 주입받아 수식에 적용한다.

---

## 8. 로드맵

| 단계 | 작업 |
|------|------|
| 1 (1~2주) | Objective·Rubric 추가, Verify 단계 추가, 전략 3계층 정의, Objective별 매핑 확정, **UX-우선 원칙·계층 분리 적용** |
| 2 | StrategyBundle 도입; Core + Objective-specific 적용; **Objective별 호출 상한·Bundle 3개 가드레일 구현**; 평균 LLM 호출 지표 수집 시작 |
| 3 | **Static Golden Set** MVP 50개 구축; 환각 탐지 Validator 연결; **Verify 선택적 실행 정책·Soft-verify 기준 적용**; **하드캡 async fallback 구현**; 회귀 지표 연동 |
| 4 | SELF_CONSISTENCY·TREE_OF_THOUGHTS A/B 실험; **3축 승격 점수 + 티어별 가중치 측정**; **Traffic-weighted Dynamic Set 구축**; Golden set 160개 확장 |
| **5 (NEW)** | **Result Badge UX 구현** (adapter 레이어); **PromptLifecycle 도메인 구축** (버전 저장·이력 조회); 성공률 기반 추천 서비스 |
| **6 (NEW)** | 버전 diff UI; **즐겨찾기 → Dynamic Golden Set 연동**; 재실행 이력 기반 개인화 |

---

 ## 9. Result Badge UX `NEW`

> 품질 지표는 내부에서 측정하고, 사용자에게는 배지 형태로만 전달한다.
> 배지 변환 로직은 adapter/in/web 전용이다. 도메인·애플리케이션 레이어를 침범하지 않는다.

| 배지 | 트리거 조건 | 표시 |
|------|------------|------|
| ✅ 조건 반영됨 | Coverage 검사 통과 | 항상 |
| ✅ 형식 검증 완료 | Schema/OutputContract 통과 | 항상 |
| ✅ 금지어 없음 | ContentSandbox 통과 | 항상 |
| ⚡ 빠른 생성 | Repair 0회, First-pass 통과 | 선택적 |
| 🔄 재검증 완료 | Repair 1~2회 후 통과 | 선택적 (횟수는 숨김) |

- [ ] `GeneratePromptResult` DTO에 `List<QualityBadge>` 필드 추가
- [ ] `BadgeResponseAssembler` — Result → BadgeResponseDto 변환 (adapter 전용)
- [ ] pass rate, repair rate 등 수치는 UX 응답에 포함하지 않는다

---

## 10. 프롬프트 유지·재사용 체계 `NEW`

> 사용자가 계속 쓰는 플랫폼을 만들기 위한 라이프사이클 설계다.

### 10-1. 버전 관리

- [ ] 프롬프트 생성 시 `PromptVersion` 자동 저장 (버전 번호, 생성 시각, Objective, strategyBundle snapshot, pass rate)
- [ ] 이전 버전과 현재 버전 diff 지원 (섹션 단위)

### 10-2. 성공률 기반 추천

- [ ] 동일 TaskDomain + PromptCategory 조합에서 pass rate 상위 프롬프트 추천
- [ ] 추천 기준: Static Golden Set pass rate ≥ 85% + 최근 30일 사용 빈도 가중

### 10-3. 즐겨찾기

- [ ] 사용자가 프롬프트 북마크 가능
- [ ] 북마크된 프롬프트는 Traffic-weighted Dynamic Set 후보로 우선 샘플링

### 10-4. 재실행 이력

- [ ] 최근 N개 프롬프트 이력 저장
- [ ] 동일 입력으로 재실행 시 이전 결과와 diff 제공
- [ ] 이력 기반 "개선 버전 사용 가능" 알림 (async fallback 결과 연동)

---

**핵심:** 정확도는 길이·역할이 아니라 **Objective + Verify–Repair 루프 + 전략 조합 + Golden set**으로 올라가고 유지된다.
**비용 원칙:** 전략은 3계층·Objective당 최대 3 Bundle로 제한하고, Experimental은 **3축 승격 점수** 검증 후에만 승격한다. 정확도·비용·지연은 동시에 측정해야 한다.
**UX 원칙:** 사용자는 내부 엔진을 보지 않는다. 품질 지표는 "검증 완료"로만 노출한다.
