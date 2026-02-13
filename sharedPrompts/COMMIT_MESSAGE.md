fix(production): 코드 리뷰 피드백 반영 - 예외 처리 및 테넌트 격리 개선

## 주요 변경사항

### 1. Spring DI 문제 해결

#### LocalStorageStrategy
- @ConditionalOnProperty 추가: production.storage.type=LOCAL 또는 미설정 시 활성화
- S3StorageStrategy와의 빈 충돌 해결 (서로 배타적 조건으로 설정)

### 2. 테넌트 격리 강화

#### StorageStrategy
- tenant-aware store 메서드의 fallback을 WARN 로깅 → BaseException으로 변경
- 멀티테넌시 환경에서 데이터 격리 위반 방지를 위한 fail-fast 전략 적용
- LoggerFactory import 제거 (사용하지 않음)

#### ProductionArtifactService
- TenantContext.getCurrentTenantId() null 체크 추가
- tenantId를 명시적으로 캡처하여 비동기 스레드에 전달
- ThreadLocal 기반 TenantContext가 비동기 스레드에 전파되지 않는 문제 해결

#### ThumbnailService
- generateThumbnailsAsync()에 tenantId 파라미터 추가
- tenant-aware store 메서드 사용으로 테넌트별 경로 생성 보장

### 3. 입력 검증 추가

#### JavaImageProcessor
- resize() 메서드에 width/height 양수 검증 추가
- generateThumbnail() 메서드에 size 양수 검증 추가
- extractMetadata()에서 format/mimeType 불일치 수정 (detectedFormat 분리)

### 4. 로깅 개선

#### StorageLifecycleService
- "completed" 로그를 try 블록 내부로 이동
- 예외 발생 시 "failed" 로그만 출력되도록 수정 (모니터링 정확도 향상)

### 5. 예외 처리 통일

#### 모든 파일
- IllegalArgumentException → BaseException (ModuleErrorCode.VALIDATION_ERROR)
- IllegalStateException → BaseException (ModuleErrorCode.VALIDATION_ERROR)
- UnsupportedOperationException → BaseException (ModuleErrorCode.STORAGE_ERROR)
- IOException은 인터페이스 시그니처 유지 (checked exception)

## 변경된 파일

- `LocalStorageStrategy.java` - @ConditionalOnProperty 추가
- `StorageStrategy.java` - BaseException 사용, tenant isolation fail-fast
- `JavaImageProcessor.java` - 입력 검증 추가, format/mimeType 수정, BaseException 사용
- `ProductionArtifactService.java` - tenantId null 체크 및 명시적 전달, BaseException 사용
- `ThumbnailService.java` - tenantId 파라미터 추가
- `StorageLifecycleService.java` - 로그 위치 수정, BaseException 사용
