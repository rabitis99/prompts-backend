# 유지보수성 개선 방안 (3/5 → 5/5)

## 현재 상태

- 구조는 좋으나 테스트 부족

## 5점 달성 방안

### 1. 테스트 코드 작성 🟠 **가장 중요**

**단위 테스트** (Service 계층):
- 테스트 커버리지 80% 이상 목표
- Mockito 활용한 의존성 모킹
- Given-When-Then 패턴 적용

**통합 테스트** (Repository, Controller):
- `@DataJpaTest`, `@WebMvcTest` 활용
- TestContainers 또는 H2 인메모리 DB 사용

**테스트 코드 구조**:
- 테스트 클래스명: `{ClassName}Test`
- 테스트 메서드명: `메서드명_조건_예상결과` 패턴

### 2. 테스트 커버리지 측정 🟡

- JaCoCo 도구 도입
- CI/CD 파이프라인에서 커버리지 측정 및 리포트 생성
- 커버리지 임계값 설정 (예: 80% 미만 시 빌드 실패)

### 3. 테스트 데이터 관리 🟡

- 테스트 픽스처 생성 유틸리티 (Builder 패턴)
- 테스트 데이터베이스 초기화 전략 (Flyway/Liquibase)

### 4. 문서화 강화 🟢

- README.md에 프로젝트 구조, 실행 방법, 테스트 방법 설명
- 주요 설계 결정 사항 문서화 (ADR: Architecture Decision Records)
- API 문서화 (Spring REST Docs 또는 Swagger)

### 5. 로깅 전략 수립 🟡

- 로깅 레벨 가이드라인 (ERROR, WARN, INFO, DEBUG)
- 구조화된 로깅 (JSON 형식)
- 주요 비즈니스 로직에 로깅 추가

**우선순위**: 테스트 코드 작성이 가장 중요 (유지보수성의 핵심)

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)


