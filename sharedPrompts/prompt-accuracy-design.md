# Prompt 정확도 설계 체크리스트

전체 설계·연구 근거·패키지 구조는 **`prompt-hexagonal-architecture.md`** 를 참고한다.  
이 문서는 정확도 중심 작업만 빠르게 점검할 때 쓰는 체크리스트다.

---

## 0. 목표 정의

- [ ] **PromptObjective** enum 도입 (FACTUAL, REASONING, EXTRACTION, PLANNING, CREATIVE_WITH_CONSTRAINTS)
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
- [ ] 정확도 전략 enum 추가: CLARIFY_FIRST, DECOMPOSITION, STEP_BY_STEP, CHECKLIST_VERIFY, EDGE_CASE_SCAN, CITE_OR_UNCERTAIN, REQUIRE_JUSTIFICATION
- [ ] TaskDomain / PromptCategory별 기본 전략 집합을 GuidelinePolicy와 연결

---

## 3. Validator 확장

- [ ] Coverage 검사 (요구사항·필수 섹션 누락)
- [ ] 입력 조건 유지 검사 (숫자/조건/엔티티 보존)
- [ ] 모순 탐지 (A이다 + A가 아니다, 확정 표현 남용)
- [ ] 불확실성 처리 (근거 없이 단정 → Repair 유도)

---

## 4. Repair

- [ ] 실패 항목만 수정 요청 (형식 유지, X만 보완)
- [ ] 2회 제한 + 실패 루브릭 타입 로깅

---

## 5. 테스트

- [ ] Golden set 최소 50개 (TaskDomain × ActionType, 기대 조건 체크리스트)
- [ ] 회귀 테스트 자동화 (정책/전략 변경 시 통과율 비교)
- [ ] 오류 유형 분류 (누락, 환각, 모순, 장황, 요구 불충족)

---

## 6. 운영 지표

- [ ] First-pass pass rate
- [ ] Repair rate / Repair success rate
- [ ] Golden set pass rate
- [ ] Error type distribution

---

## 7. 로드맵

| 단계 | 작업 |
|------|------|
| 1 (1~2주) | Objective·Rubric 추가, Verify 단계 추가 |
| 2 | StrategyBundle, CHECKLIST_VERIFY 적용 |
| 3 | Golden set 50개, 회귀 지표 연결 |
| 4 | Edge-case 전략, 전략별 정확도 실험 |

---

**핵심:** 정확도는 길이·역할이 아니라 **Objective + Verify–Repair 루프 + 전략 조합 + Golden set**으로 올라가고 유지된다.
