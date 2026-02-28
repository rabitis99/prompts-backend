# Prompt Domain 검토: 헥사고날 & SOLID

`domain/prompt/domain` 패키지에 대한 정통 헥사고날 아키텍처 및 자바 5대 원칙(SOLID) 준수 여부 검토 결과.

---

## 1. 헥사고날 아키텍처 정합성

### 1.1 도메인 코어의 외부 의존

| 항목 | 상태 | 비고 |
|------|------|------|
| Spring / Jakarta | ✅ 없음 | domain 패키지에 `org.springframework`, `jakarta`, `javax` import 없음 |
| SLF4J / 로깅 | ✅ 제거됨 | `StrategyBundlePolicy`에 있던 `org.slf4j` 의존 제거 — 도메인은 이제 순수 POJO |
| Repository / Adapter | ✅ 없음 | domain이 application.port.out, adapter, repository를 참조하지 않음 |

**정통 헥사고날 원칙**: "도메인 코어는 프레임워크·인프라에 의존하지 않는다."  
→ **현재 준수**: domain은 `domain.*`, `enums`, `guideline`만 참조하며, 모두 같은 바운디드 컨텍스트(프롬프트) 내부다.

### 1.2 의존성 방향

- **domain → enums**: 도메인 개념(TaskDomain, PromptObjective 등) 사용. ✅
- **domain → guideline**: 가이드라인 규칙(GuidelineRule)은 프롬프트 규칙의 일부로 동일 컨텍스트. ✅
- **application → domain**: 유즈케이스가 도메인 서비스·포트 사용. ✅
- **adapter → application.port.out / domain**: 포트 구현·도메인 모델 사용. ✅

의존성은 "바깥 → 안(도메인)"으로만 흐르며, 도메인이 application/adapter를 알지 않는다.

### 1.3 포트 위치

- **ObjectiveResolverPort**가 `domain.resolutions`에 정의되어 있음.
- 정통 헥사고날에서는 포트를 "애플리케이션(유즈케이스) 계층"에 두는 경우가 많지만, "도메인이 해석 계약(포트)을 소유"하는 방식도 흔히 쓴다.
- 현재는 "해석 규칙은 도메인 소유, 구현은 인프라(ResolutionConfig)에서 주입"으로 일관되게 적용됨. ✅

### 1.4 요약 (헥사고날)

- 도메인은 **프레임워크·인프라 무의존**하며, **외부 계층을 알지 못함**.
- **의존성 방향**과 **포트 소유** 방식이 프로젝트 의도와 맞게 유지됨.
- **수정 사항**: `StrategyBundlePolicy`에서 SLF4J 제거로 "순수 도메인" 원칙을 만족하도록 정리함. (다운그레이드 시 로깅이 필요하면 application/인프라에서 처리 권장.)

---

## 2. SOLID(자바 5대 원칙) 준수

### 2.1 S — Single Responsibility (단일 책임)

| 클래스/인터페이스 | 책임 | 판단 |
|-------------------|------|------|
| **DomainResolver** | ActionType + PromptCategory → TaskDomain 해석만 | ✅ |
| **ObjectiveResolverPort** / **ObjectiveResolver** | (TaskDomain, ActionType) → PromptObjective 해석만 | ✅ |
| **PromptSpecFactory** | 사용자 입력 + 해석 결과 → PromptSpec 조립만 | ✅ |
| **PromptSpecValidator** | draft + spec → 검증 전략 실행만 | ✅ |
| **BadgeResolver** | VerifyResult + 파이프라인 지표 → QualityBadge 목록만 | ✅ |
| **StrategyBundlePolicy** | objective + experimental 플래그 → 번들 결정 및 상한 가드 | ✅ |
| **ObjectiveProfile** | 한 목표(Objective)에 대한 제약·루브릭·검증·전략 번들 제공 | ✅ |
| **VerificationStrategy** | VerificationContext → VerifyResult | ✅ |

각 타입이 "한 가지 이유로만 변경"되는 구조로 잘 나뉘어 있음.

### 2.2 O — Open/Closed (개방-폐쇄)

- **새 Objective 추가**: `ObjectiveProfile` 구현체 추가 + `DefaultObjectiveRegistry` 등록.  
  `PromptSpecFactory`, `PromptSpecValidator`, `StrategyBundlePolicy`는 수정 불필요. ✅
- **새 액션 타입 / 해석 규칙**: `ObjectiveMappingRegistry`(명시 매핑/키워드 규칙) 또는 도메인 기본값에 항목 추가.  
  `ObjectiveResolver` 알고리즘은 그대로 둠. ✅
- **새 검증 방식**: `VerificationStrategy` 구현체 추가.  
  `PromptSpecValidator`는 수정 불필요. ✅

확장은 "추가"로 하고, 기존 도메인 클래스는 닫혀 있음.

### 2.3 L — Liskov Substitution (리스코프 치환)

- **ObjectiveProfile** 구현체들: 동일 인터페이스·계약 준수, 서로 치환 가능. ✅
- **VerificationStrategy** 구현체들: `verify(VerificationContext) → VerifyResult` 계약 동일. ✅
- **ObjectiveResolver** (ObjectiveResolverPort 구현): `resolve(...)` 계약 준수. ✅

하위 타입이 상위 타입의 사용처를 깨지 않음.

### 2.4 I — Interface Segregation (인터페이스 분리)

- **ObjectiveResolverPort**: `resolve` 하나만 노출. ✅
- **VerificationStrategy**: `verify` 하나 (함수형 인터페이스). ✅
- **ObjectiveProfile**: 한 목표에 필요한 메서드만 포함.  
  여러 메서드가 있지만, 하나의 "목표 프로파일" 개념에 묶여 있어 과도하게 쪼개지 않은 수준. ✅

클라이언트가 불필요한 메서드에 의존하지 않도록 잘 나뉘어 있음.

### 2.5 D — Dependency Inversion (의존성 역전)

- **PromptSpecFactory**: `ObjectiveRegistry`, `ObjectiveResolverPort`, `StrategyBundlePolicy` 등 **추상(인터페이스/정책)**에 의존. ✅
- **PromptSpecValidator**: `ObjectiveRegistry`(인터페이스)에 의존. ✅
- **StrategyBundlePolicy**: `ObjectiveRegistry`(인터페이스)에 의존. ✅
- **ObjectiveResolver**: 해석 규칙 관련 포트(추상)에 의존하도록 정리됨.  
  → 구현체 교체 시 상위 정책/서비스 수정 없이 확장 가능.

고수준(팩토리·검증·정책)이 저수준 구현이 아니라 추상에 의존하는 구조가 유지됨.

---

## 3. 개선 제안 요약

| 항목 | 제안 | 우선순위 |
|------|------|----------|
| 도메인 로깅 | `StrategyBundlePolicy`에서 SLF4J 제거 완료. 다운그레이드 로깅이 필요하면 application/인프라에서 처리 | ✅ 반영됨 |
| ObjectiveResolver 의존 | ExplicitObjectiveMappingPort, ObjectiveMappingRegistryPort 추출·주입으로 DIP 강화. 포트 추출 반영됨 | ✅ 반영됨 |
| DomainResolver | DomainResolverPort 추출. 구현체는 DomainResolver. 포트 추출 반영됨 | ✅ 반영됨 |

---

## 4. 결론

- **헥사고날**: 도메인 코어가 프레임워크·인프라에 의존하지 않으며, 의존성 방향과 포트 사용 방식이 정통 헥사고날 사고와 맞음.  
  SLF4J 제거로 "순수 도메인" 원칙을 만족하도록 수정함.
- **SOLID**: 단일 책임, 개방-폐쇄, 리스코프 치환, 인터페이스 분리, 의존성 역전이 domain 패키지에서 전반적으로 준수됨.  
  DIP는 ObjectiveResolver·DomainResolver가 포트(추상)에만 의존하도록 반영 완료됨.
