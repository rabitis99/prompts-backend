refactor(production): 코드 품질 개선 및 아키텍처 정리

## 주요 변경사항

### 1. YAML 설정 구조 개선
- `application.yml`: artifact 키의 YAML 계층 구조 명확화
  - 주석 들여쓰기 수정 및 섹션 헤더 추가
  - `artifact:`를 루트 레벨로 명확히 표시

### 2. 예외 처리 일관성 개선
- `ArtifactAccessServiceImpl`: RuntimeException → BaseException 변경
  - `ModuleErrorCode.STORAGE_ERROR` 사용으로 일관된 예외 처리
  - 원인 예외(cause) 포함하여 예외 체인 유지

### 3. 리소스 관리 개선
- `S3Config`: S3Presigner에 `@Bean(destroyMethod = "close")` 명시
  - 명시적 리소스 정리로 메모리 누수 방지

### 4. 코드 리팩토링
- `ArtifactDtoMapper`:
  - private 생성자 추가 (유틸리티 클래스 인스턴스화 방지)
  - if-else 체인을 switch 표현식으로 변경
  - enum 값 추가 시 컴파일 타임 누락 감지 가능

- `ProductionResultApplicationService`:
  - 중복된 Job 조회 및 소유권 검증 로직을 `getJobOrThrow()` 헬퍼 메서드로 추출
  - `retryJob()`에 Job 상태 검증 추가 (FAILED/PARSE_FAILED만 retry 가능)

### 5. 인터페이스 분리 원칙 적용
- `ArtifactDto` 구조 개선:
  - `FileBasedArtifactDto` sealed interface 추가
  - `TextArtifactDto`에서 불필요한 파일 메타데이터 필드 제거
  - `ImageArtifactDto`, `FileArtifactDto`는 `FileBasedArtifactDto` 구현

### 6. 환경별 구현체 분리
- `LocalArtifactAccessServiceImpl` 추가:
  - LOCAL 환경용 fallback 구현체
  - `@ConditionalOnProperty(matchIfMissing = true)`로 기본값 지원
  - 파일 경로를 그대로 반환 (Presigned URL 불필요)

### 7. TTL 일관성 개선
- `ArtifactAccessServiceImpl`:
  - 단일 TTL 설정값에서 Presigned URL TTL과 캐시 TTL 도출
  - 캐시 TTL을 Presigned URL TTL보다 짧게 설정하여 만료된 URL 반환 방지
  - `Math.max(ttlSeconds - 30, ttlSeconds / 2)` 공식 적용

### 8. 테스트 코드 수정
- `ProductionResponseDtoTest`:
  - `ProductionResponseDto.from()` → `ProductionResponseDtoMapper.toDto()` 변경
  - record 접근자 메서드 수정 (getStatus() → status())
  - `testProcessingStatus`: completedAt을 null로 변경하여 비즈니스 로직 일관성 개선

## 변경된 파일

### 신규 생성
- `LocalArtifactAccessServiceImpl.java`
- `FileBasedArtifactDto.java`

### 수정
- `application.yml` (artifact 섹션 구조 개선)
- `ArtifactAccessServiceImpl.java` (예외 처리, TTL 일관성)
- `S3Config.java` (destroyMethod 명시)
- `ArtifactDtoMapper.java` (private 생성자, switch 표현식)
- `ProductionResultApplicationService.java` (중복 제거, 상태 검증)
- `ArtifactDto.java` (인터페이스 분리)
- `TextArtifactDto.java` (불필요한 필드 제거)
- `ImageArtifactDto.java` (FileBasedArtifactDto 구현)
- `FileArtifactDto.java` (FileBasedArtifactDto 구현)
- `ProductionResponseDtoTest.java` (테스트 코드 수정)

## 효과
- 코드 품질 향상: 중복 제거, 일관성 개선
- 타입 안전성 강화: sealed interface 활용
- 환경별 지원: LOCAL/S3 환경 모두 정상 동작
- 리소스 관리 개선: 명시적 정리로 메모리 누수 방지
- 테스트 신뢰성 향상: 비즈니스 로직 반영
