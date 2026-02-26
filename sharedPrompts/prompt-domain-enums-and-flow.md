## 프롬프트 도메인 Enum 정리 및 v2 흐름 개요

이 문서는 `domain/prompt/enums`에 정의된 enum 들과, 새로 도입된 도메인/전략 기반 프롬프트 엔진(v2)의 주요 흐름을 정리한다.

---

## 1. Enum 정리 (`domain.prompt.enums`)

### 1-1. `TaskDomain`

- **역할**: 작업의 상위 도메인(기술/창작/분석/실무/교육/일반)을 표현하고, 각 도메인에 맞는 가이드라인/톤/스타일을 제공한다.
- **값**
  - **TECHNICAL**: 기술형, `TechnicalGuidelines`
  - **CREATIVE**: 창의형, `CreativeGuidelines`
  - **ANALYTICAL**: 분석형, `AnalyticalGuidelines`
  - **PRACTICAL**: 실무형, `PracticalGuidelines`
  - **EDUCATIONAL**: 교육형, `EducationalGuidelines`
  - **GENERAL**: 일반형(보수적 안전 모드), `GeneralGuidelines`
- **핵심 메서드**
  - **`principles()/structuringRules()/qualityStandards()/outputConstraints()`**: 내부 `GuidelinePolicy`로 위임.
  - **`getRecommendedStyles()`**: 도메인별 추천 `StyleType` 목록 반환.
  - **`getRecommendedTones()`**: 도메인별 추천 `ToneType` 목록 반환.
  - **`getRulesByLevel(RuleLevel level)`**: 레벨 기준으로 모든 규칙 스트림 병합 후 필터링.

### 1-2. `PromptCategory`

- **역할**: 유저가 선택하는 상위 프롬프트 카테고리이며, 각 카테고리는 대표 `TaskDomain`(기본 도메인)을 가진다.
- **주요 필드**
  - **`displayName`**: UI 표시용 한글 이름
  - **`guidelineKo/En/Ja`**: 카테고리 설명 (3개 언어)
  - **`defaultDomain`**: 대표 `TaskDomain` (도메인 해석의 2순위 힌트)
- **값 (요약)**
  - **PRODUCTIVITY**: 생산성 → `PRACTICAL`
  - **DEVELOPMENT / CODING / PROGRAMMING**: 개발·코딩·프로그래밍 → `TECHNICAL`
  - **ANALYSIS / RESEARCH**: 분석/연구 → `ANALYTICAL`
  - **MARKETING / BUSINESS**: 마케팅/비즈니스 → `PRACTICAL`
  - **CONTENT / CREATIVE / WRITING / DESIGN**: 콘텐츠·창작·글쓰기·디자인 → `CREATIVE`
  - **STUDY / EDUCATION**: 학습/교육 → `EDUCATIONAL`
  - **ETC**: 기타 → `GENERAL`

### 1-3. `ToneType`

- **역할**: 응답의 어조(톤)를 정의하고, 언어별 톤 가이드라인과 추천 도메인을 제공한다.
- **주요 필드**
  - **`displayName`**: 예) "친근한", "공식적인"
  - **`guidelineKo/En/Ja`**: 톤 설명 (3개 언어)
- **핵심 메서드**
  - **`getGuidelineByLang(LanguageType lang)`**: 언어별 가이드라인 텍스트 반환.
  - **`getRecommendedDomains()`**: 톤에 추천되는 `TaskDomain` 목록.
- **값 (예시)**
  - **FRIENDLY / CASUAL / EMPATHETIC / POSITIVE / ENTHUSIASTIC** 등
  - **FORMAL / PROFESSIONAL / NEUTRAL** 등
  - **HUMOROUS / INSPIRATIONAL / SARCASTIC / NEGATIVE** 등

### 1-4. `StyleType`

- **역할**: 응답의 서술 스타일(서사형, 글머리형, 분석형 등)을 정의하고, 언어별 스타일 가이드라인 및 추천 도메인을 제공한다.
- **주요 필드**
  - **`displayName`**: 예) "서사형", "글머리형", "분석형"
  - **`guidelineKo/En/Ja`**: 스타일 설명 (3개 언어)
- **핵심 메서드**
  - **`getGuidelineByLang(LanguageType lang)`**: 언어별 스타일 가이드라인.
  - **`getRecommendedDomains()`**: 스타일에 추천되는 `TaskDomain` 목록.
- **값 (예시)**
  - **NARRATIVE / STORYTELLING / DESCRIPTIVE / CREATIVE / DIALOGUE** → 주로 `CREATIVE`
  - **TECHNICAL** → `TECHNICAL`
  - **ANALYTICAL / COMPARATIVE** → `ANALYTICAL`
  - **BULLET** → `PRACTICAL`
  - **DETAILED** → `EDUCATIONAL`
  - **INSTRUCTIVE / QUESTION_ANSWER / FORMATTED / CONCISE** → 여러 도메인 범용

### 1-5. `LanguageType`

- **역할**: 응답/가이드라인 언어를 표현한다.
- **값**
  - **KOREAN**: "한국어"
  - **ENGLISH**: "영어"
  - **JAPANESE**: "일본어"
- **필드**
  - **`description`**: 한글 설명
  - **`promptToken`**: 프롬프트 안에서 사용할 언어 토큰 문자열

### 1-6. `ExperienceLevel`

- **역할**: 타깃 사용자의 경험 수준(초급~전문가)에 따른 설명 깊이 가이드라인.
- **값**
  - **BEGINNER**: 기초부터 단계별, 예시 충분
  - **INTERMEDIATE**: 기본 개념 가정, 실용 팁 중심
  - **ADVANCED**: 심화 내용, 최적화 기법, 고급 패턴
  - **EXPERT**: 최신 연구, 엣지 케이스, 트레이드오프, 전문 용어 자유 사용

### 1-7. `SortType`

- **역할**: 프롬프트 목록/피드 정렬 기준.
- **값**
  - **LATEST**: 최신순 (`createdAt.desc()`)
  - **POPULAR**: 인기순 (`viewCount.desc(), createdAt.desc()`)
- **핵심 메서드**
  - **`toOrderSpecifiers(QPrompt prompt)`**: Querydsl용 정렬 스펙 반환.

### 1-8. `I18nUtils`

- **역할**: `LanguageType` 기반 다국어 문자열 선택 유틸리티.
- **핵심 메서드**
  - **`getByLang(LanguageType lang, Function<LanguageType, String> provider)`**
  - **`getByLang(LanguageType lang, String ko, String en, String ja)`**

---

## 2. 변경된 도메인/프롬프트 흐름 (v2)

### 2-1. 전체 파이프라인 개요

- **입력**: 클라이언트가 `POST /api/v2/prompts/generate` (`PromptEngineController`) 호출
  - `GeneratePromptRequest` 안에는 `PromptCategory`, `ToneType`, `StyleType`, `LanguageType`, `ActionType`(인터페이스), `RoleType` 등이 포함된다.
- **어댑터 → 애플리케이션**
  - `PromptEngineController.generate(...)`
    - `GeneratePromptCommand command = request.toCommand(authUser.getId());`
    - `GeneratePromptResult result = generatePromptUseCase.generate(command);`
  - 이때 어댑터 레이어는 **도메인 객체를 직접 조합하지 않고**, 유스케이스 인터페이스만 호출한다.
- **애플리케이션 → 도메인**
  - `GeneratePromptUseCase` 구현체 내부에서
    - `DomainResolver`를 사용해 `TaskDomain` 결정
    - `PromptSpecFactory`를 사용해 `PromptSpec` 생성
    - 이후 Clarify → Solve → Verify → Repair 4단계 파이프라인 실행 및 결과/배지 생성

### 2-2. 도메인 결정 규칙 (`DomainResolver`)

- **클래스**: `DomainResolver`
- **입력**
  - `ActionTypeInterface` (액션 타입)
  - `PromptCategory`
  - 또는 `InputRequestDto` (내부적으로 위 값 포함)
- **우선순위 규칙 (`resolveDomain(InputRequestDto)` 기준)**
  - **1순위**: `ActionType.getTaskDomain()` 이 `GENERAL`이 아닌 경우 → 해당 도메인 사용
  - **2순위**: `PromptCategory.getDefaultDomain()` 이 `GENERAL`이 아닌 경우 → 해당 도메인 사용
  - **3순위**: `ActionType`이 명시적으로 `GENERAL`을 반환하는 경우 → `GENERAL` 사용(폴백 아님)
  - **폴백**: 위 조건 어디에도 해당하지 않거나, `ActionType`/`PromptCategory`가 `null`인 경우
    - `TaskDomain.GENERAL` + `isFallback = true`
    - 로그로 미매핑/이상 상황 경고
- **오버로드**
  - **`resolveDomainSimple(InputRequestDto)`**: 도메인만 필요할 때 사용.
  - **`resolveDomain(ActionTypeInterface, PromptCategory)`**: 헥사고날 어댑터에서 DTO 없이 직접 사용 가능.

### 2-3. `PromptSpec` 생성 흐름 (`PromptSpecFactory`)

- **클래스**: `PromptSpecFactory`
- **입력**
  - `rawInput`: 사용자 원문 입력
  - `taskDomain`: `DomainResolver`에서 결정된 `TaskDomain`
  - `actionType`: `ActionTypeInterface`
  - `role`: `RoleTypeInterface`
  - `tone`: `ToneType`
  - `style`: `StyleType`
  - `locale`: `LanguageType`
  - `experimentalEnabled`: 실험 플래그
- **주요 단계 (`create(...)`)**
  - **`PromptObjective objective = resolveObjective(taskDomain, actionType);`**
    - 현재 구현에서는 `TaskDomain` 기반으로 Objective 결정
      - TECHNICAL → REASONING
      - ANALYTICAL → FACTUAL
      - CREATIVE → CREATIVE_WITH_CONSTRAINTS
      - PRACTICAL → PLANNING
      - EDUCATIONAL → REASONING
      - 기본값 → REASONING
  - **품질 전략 결정**
    - `QualityPriority priority = resolvePriority(objective);`
    - `QualityRubric rubric = buildRubric(objective);`
      - FACTUAL, REASONING, EXTRACTION, PLANNING, CREATIVE 각각에 맞는 평가 항목 세팅
  - **구조/가이드라인 섹션 구성**
    - `buildSections(objective, role, taskDomain, locale)`
      - `role` 이 있으면 ROLE 섹션 추가 (영문 role 설명)
      - `taskDomain.principles()/structuringRules()/outputConstraints()` 를 모아
        - `appendGuidelineRules(...)` 로 언어(`LanguageType`)에 맞는 설명 텍스트 생성
      - VERIFICATION_CHECKLIST 섹션에 도메인별 체크리스트를 누적
      - Objective 별로 INSTRUCTION / OUTPUT_FORMAT / CONSTRAINTS 섹션 구조만 정의
  - **제약 및 출력 계약**
    - `Constraints constraints = buildConstraints(objective);`
    - `OutputContract outputContract = buildOutputContract(objective);`
      - EXTRACTION일 경우 JSON Schema 기반 구조화 출력, 그 외는 free text
  - **전략 번들 및 기타 필드**
    - `PromptStrategyBundle bundle = strategyBundlePolicy.resolveBundle(objective, experimentalEnabled);`
    - `tone`, `style`, `locale`이 `null`일 경우 기본값 적용
      - tone: `ToneType.NEUTRAL`
      - style: `StyleType.NARRATIVE`
      - locale: `LanguageType.KOREAN`
  - **최종 결과**
    - 위 요소들을 모두 포함한 `PromptSpec` 빌드 후 반환

### 2-4. v2 컨트롤러 레이어 (`PromptEngineController`)

- **엔드포인트**
  - `POST /api/v2/prompts/generate`
  - 비동기 처리 (`WebAsyncTask`) + 60초 타임아웃
- **역할**
  - **도메인/애플리케이션 계층에 직접 관여하지 않고**, `GeneratePromptUseCase` 하나만 호출.
  - 결과(`GeneratePromptResult`)를 `BadgeResponseAssembler`로 변환하여
    - 품질 배지 정보만 포함된 `GeneratePromptResponse` 생성
  - 실패/타임아웃 시, 공통 예외(`ApiException`)와 `CustomResponse.fail(...)`로 응답 포맷 통일.

---

## 3. 요약

- **Enum 계층 (`TaskDomain`, `PromptCategory`, `ToneType`, `StyleType`, `LanguageType`, `ExperienceLevel`, `SortType`)** 은
  - 프롬프트의 **도메인/어조/스타일/언어/경험 수준/정렬 기준**을 정형화하고
  - Guideline 및 추천 조합(톤·스타일 ↔ 도메인)을 제공한다.
- **`DomainResolver`** 가 ActionType/Category 기반으로 **TaskDomain을 일관되게 결정**하고,
- **`PromptSpecFactory`** 가 TaskDomain/Objective를 중심으로
  - 구조(섹션), 품질 기준, 제약, 출력 계약, 전략 번들을 한 번에 조합해 **표준화된 PromptSpec**을 만든다.
- 이 흐름을 통해 v2 엔진은 **카테고리/도메인/목적에 따라 일관된 품질과 구조를 가진 프롬프트**를 생성할 수 있다.

