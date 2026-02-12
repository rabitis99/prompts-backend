feat(production): P0 렌더링 아키텍처 개선 - 타입 안전성 및 Presigned URL 지원

## 주요 변경사항

### 1. ArtifactDto 타입 안전성 개선
- `ArtifactDto`를 sealed interface로 변경하여 타입 안전성 확보
- `location` 필드 제거 및 타입별 전용 필드 도입
  - `TextArtifactDto`: `content` 필드
  - `FileArtifactDto`: `filePath`, `downloadUrl` 필드
  - `ImageArtifactDto`: `filePath`, `previewUrl` 필드
- 모든 DTO를 record로 변경하고 `@JsonProperty` 명시

### 2. Presigned URL 기본 구현
- `ArtifactAccessService` 인터페이스 및 `ArtifactAccessServiceImpl` 구현
- S3Presigner Bean 설정 (`S3Config`)
- Redis 캐싱 적용 (TTL: 5분, 기본값)
- `ImageArtifactDto` 생성 시 `previewUrl` 자동 포함
- `FileArtifactDto` 생성 시 `downloadUrl` 자동 포함

### 3. ProductionStatus enum 도입
- `success` 필드 중복 제거
- `ProductionStatus` enum 생성 (PROCESSING, SUCCESS, FAILED)
- `ProductionResponseDto`에서 `success` 제거, `status` 필드로 변경
- `errorMessage`는 FAILED일 때만, `artifact`는 SUCCESS일 때만 포함

### 4. Application Service 도입
- `ProductionResultApplicationService` 생성
- Controller에서 비즈니스 로직 제거
- `ProductionResponseDtoMapper`, `ArtifactDtoMapper` 생성 (비즈니스 로직 분리)

### 5. 예외 처리 개선
- `ModuleErrorCode`에 Production 관련 에러 코드 추가
  - `PRODUCTION_NOT_FOUND`
  - `PRODUCTION_FORBIDDEN`
  - `JOB_FORBIDDEN`
- `BaseException` 사용으로 일관된 예외 처리

## 변경된 파일

### 신규 생성
- `TextArtifactDto.java`
- `FileArtifactDto.java`
- `ImageArtifactDto.java`
- `ProductionStatus.java`
- `ArtifactAccessService.java`
- `ArtifactAccessServiceImpl.java`
- `S3Config.java`
- `ProductionResponseDtoMapper.java`
- `ArtifactDtoMapper.java`
- `ProductionResultApplicationService.java`

### 수정
- `ArtifactDto.java` (abstract class → sealed interface)
- `ProductionResponseDto.java` (record, status 필드)
- `ProductionResultController.java` (Application Service 사용)
- `ModuleErrorCode.java` (Production 관련 에러 코드 추가)

### 설정
- `application.yml` (artifact.url.default-ttl 추가)

## 효과
- 타입 안전성 확보로 클라이언트 런타임 에러 감소
- 이미지 미리보기 기능 즉시 사용 가능
- 명확한 API 계약 및 상태 관리
- Presigned URL 캐싱으로 생성 비용 절감

