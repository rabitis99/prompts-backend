# Module 패키지 파일 구조

```
module/
├── controller/
│   ├── delivery/
│   └── production/
│       ├── ProductionController.java
│       └── ProductionResultController.java
│
├── domain/
│   └── production/
│       ├── application/
│       │   ├── ArtifactApplicationService.java
│       │   ├── ProductionApplicationService.java
│       │   ├── ProductionResultApplicationService.java
│       │   ├── exception/
│       │   │   ├── ProductionApplicationException.java
│       │   │   └── ProductionCommandFactoryNotFoundException.java
│       │   ├── factory/
│       │   │   ├── ProductionCommandFactory.java
│       │   │   ├── ProductionCommandFactoryRegistry.java
│       │   │   └── impl/
│       │   │       ├── BlogCommandFactory.java
│       │   │       ├── DocumentCommandFactory.java
│       │   │       ├── EmailCommandFactory.java
│       │   │       ├── ImageCommandFactory.java
│       │   │       └── TextCommandFactory.java
│       │   └── storage/
│       │       ├── StorageCommandService.java
│       │       └── StorageFacade.java
│       │
│       ├── config/
│       │   ├── S3Config.java
│       │   └── condition/
│       │       └── ConditionalOnStorageType.java
│       │
│       ├── entity/
│       │   ├── job/
│       │   │   └── JobEntity.java
│       │   └── production/
│       │       ├── ProductionArtifactDetailEntity.java
│       │       ├── ProductionArtifactEntity.java
│       │       └── StorageFormat.java
│       │
│       ├── exception/
│       │   └── ParseException.java
│       │
│       ├── infra/
│       │   └── storage/
│       │       ├── S3DownloadService.java
│       │       ├── S3KeyGenerator.java
│       │       ├── S3PresignedUrlService.java
│       │       ├── S3UploadService.java
│       │       └── exception/
│       │           └── S3StorageException.java
│       │
│       ├── model/
│       │   ├── ai/
│       │   │   ├── AIContentRequest.java
│       │   │   └── AIContentResult.java
│       │   ├── contract/
│       │   │   ├── command/
│       │   │   │   ├── ProductionCommand.java
│       │   │   │   └── ProductionCommandType.java
│       │   │   └── result/
│       │   │       ├── ArtifactType.java
│       │   │       ├── FileArtifact.java
│       │   │       ├── ImageArtifact.java
│       │   │       ├── ProductionArtifact.java
│       │   │       └── TextArtifact.java
│       │   ├── executor/
│       │   │   ├── blog/
│       │   │   │   └── BlogCommand.java
│       │   │   ├── document/
│       │   │   │   └── DocumentCommand.java
│       │   │   ├── email/
│       │   │   │   └── EmailCommand.java
│       │   │   ├── image/
│       │   │   │   └── ImageCommand.java
│       │   │   └── text/
│       │   │       └── TextCommand.java
│       │   ├── job/
│       │   │   ├── Job.java
│       │   │   └── JobStatus.java
│       │   ├── storage/
│       │   │   └── StorageLifecyclePolicy.java
│       │   └── tenant/
│       │       └── TenantContext.java
│       │
│       ├── repository/
│       │   ├── job/
│       │   │   └── JobRepository.java
│       │   └── production/
│       │       ├── ProductionArtifactDetailRepository.java
│       │       └── ProductionArtifactRepository.java
│       │
│       ├── service/
│       │   ├── ai/
│       │   │   ├── AIService.java
│       │   │   ├── AIServiceRegistry.java
│       │   │   ├── ContentType.java
│       │   │   ├── config/
│       │   │   │   ├── GroqClientConfig.java
│       │   │   │   ├── LeonardoClientConfig.java
│       │   │   │   └── properties/
│       │   │   │       ├── GroqProperties.java
│       │   │   │       └── LeonardoProperties.java
│       │   │   ├── exception/
│       │   │   │   ├── AiClientException.java
│       │   │   │   └── UnsupportedContentTypeException.java
│       │   │   ├── image/
│       │   │   │   ├── ImageAIClient.java
│       │   │   │   ├── ImageAIService.java
│       │   │   │   ├── LeonardoImageAiClient.java
│       │   │   │   └── StubImageAIClient.java
│       │   │   ├── prompt/
│       │   │   │   ├── AbstractPromptBuilder.java
│       │   │   │   ├── ImagePromptBuilder.java
│       │   │   │   ├── PromptBuilder.java
│       │   │   │   └── TextPromptBuilder.java
│       │   │   ├── retry/
│       │   │   │   ├── ExponentialBackoffRetryPolicy.java
│       │   │   │   ├── RetryExecutor.java
│       │   │   │   └── RetryPolicy.java
│       │   │   ├── strategy/
│       │   │   │   ├── AIModelStrategy.java
│       │   │   │   ├── GroqModelStrategy.java
│       │   │   │   └── LeonardoModelStrategy.java
│       │   │   └── text/
│       │   │       ├── GroqResponseParser.java
│       │   │       ├── GroqTextAiClient.java
│       │   │       ├── StubTextAiClient.java
│       │   │       ├── TextAiClient.java
│       │   │       ├── TextAIService.java
│       │   │       └── dto/
│       │   │           ├── GroqChatRequest.java
│       │   │           ├── GroqChatResponse.java
│       │   │           ├── GroqChoice.java
│       │   │           ├── GroqMessage.java
│       │   │           └── GroqUsage.java
│       │   │
│       │   ├── artifact/
│       │   │   ├── ArtifactHandler.java
│       │   │   ├── ArtifactHandlerRegistry.java
│       │   │   ├── FileArtifactHandler.java
│       │   │   ├── ImageArtifactHandler.java
│       │   │   └── TextArtifactHandler.java
│       │   │
│       │   ├── cdn/
│       │   │   ├── CdnUrlProvider.java
│       │   │   ├── CloudFrontCdnUrlProvider.java
│       │   │   └── NoOpCdnUrlProvider.java
│       │   │
│       │   ├── format/
│       │   │   ├── CsvFormatConverter.java
│       │   │   ├── ExcelFormatConverter.java
│       │   │   ├── FormatConversionException.java
│       │   │   ├── FormatConverter.java
│       │   │   ├── FormatConverterRegistry.java
│       │   │   ├── HtmlFormatConverter.java
│       │   │   ├── JsonFormatConverter.java
│       │   │   ├── MarkdownFormatConverter.java
│       │   │   ├── PdfFormatConverter.java
│       │   │   ├── XmlFormatConverter.java
│       │   │   ├── markdown/
│       │   │   │   ├── FlexMarkMarkdownToHtmlConverter.java
│       │   │   │   └── MarkdownToHtmlConverter.java
│       │   │   └── pdf/
│       │   │       ├── DefaultPdfCssProvider.java
│       │   │       ├── HtmlToPdfConverter.java
│       │   │       ├── OpenHtmlToPdfConverterImpl.java
│       │   │       └── PdfCssProvider.java
│       │   │
│       │   ├── image/
│       │   │   ├── ImageMetadata.java
│       │   │   ├── ImageProcessor.java
│       │   │   ├── JavaImageProcessor.java
│       │   │   └── ThumbnailService.java
│       │   │
│       │   ├── job/
│       │   │   ├── JobEntityCreationService.java
│       │   │   ├── JobLockService.java
│       │   │   ├── JobStateService.java
│       │   │   ├── helper/
│       │   │   │   └── JobUpdateHelper.java
│       │   │   ├── idempotencykey/
│       │   │   │   ├── IdempotencyKeyGenerator.java
│       │   │   │   ├── exception/
│       │   │   │   │   └── IdempotencyKeyGenerationException.java
│       │   │   │   ├── hash/
│       │   │   │   │   ├── HashStrategy.java
│       │   │   │   │   └── Sha256HashStrategy.java
│       │   │   │   └── serializer/
│       │   │   │       └── ProductionCommandSerializer.java
│       │   │   ├── metrics/
│       │   │   │   └── JobMetrics.java
│       │   │   ├── process/
│       │   │   │   ├── JobProcessor.java
│       │   │   │   ├── exception/
│       │   │   │   │   ├── AIServiceException.java
│       │   │   │   │   ├── ContentRenderException.java
│       │   │   │   │   ├── JobExceptionHandler.java
│       │   │   │   │   ├── JobProcessingException.java
│       │   │   │   │   ├── RecoveryException.java
│       │   │   │   │   └── StorageException.java
│       │   │   │   ├── execution/
│       │   │   │   │   ├── ai/
│       │   │   │   │   │   ├── AIJobExecutor.java
│       │   │   │   │   │   ├── AIResponseHandler.java
│       │   │   │   │   │   ├── builder/
│       │   │   │   │   │   │   └── AIRequestBuilder.java
│       │   │   │   │   │   ├── circuitbreaker/
│       │   │   │   │   │   │   └── AICircuitBreakerWrapper.java
│       │   │   │   │   │   ├── retry/
│       │   │   │   │   │   │   ├── AIErrorClassifier.java
│       │   │   │   │   │   │   ├── AIRetryExecutor.java
│       │   │   │   │   │   │   └── AIRetryPolicy.java
│       │   │   │   │   │   └── token/
│       │   │   │   │   │       └── TokenExtractor.java
│       │   │   │   │   └── content/
│       │   │   │   │       ├── ContentFormatter.java
│       │   │   │   │       ├── ContentRenderer.java
│       │   │   │   │       └── ContentStorageService.java
│       │   │   │   ├── recovery/
│       │   │   │   │   ├── AiCalledRecoveryService.java
│       │   │   │   │   ├── ParsedRecoveryService.java
│       │   │   │   │   ├── ProcessJobRecoveryService.java
│       │   │   │   │   ├── RenderedRecoveryService.java
│       │   │   │   │   └── StoredRecoveryService.java
│       │   │   │   └── util/
│       │   │   │       ├── CommandDeserializer.java
│       │   │   │       ├── ContentTypeDeterminer.java
│       │   │   │       └── FileNameGenerator.java
│       │   │   ├── queue/
│       │   │   │   ├── JobAsyncScheduler.java
│       │   │   │   ├── JobCreationService.java
│       │   │   │   ├── JobMapper.java
│       │   │   │   └── JobQueueService.java
│       │   │   └── scheduler/
│       │   │       ├── JobRecoveryScheduler.java
│       │   │       ├── SchedulerJobRecoveryService.java
│       │   │       ├── handler/
│       │   │       │   ├── IntermediateJobRecoveryHandler.java
│       │   │       │   ├── JobRecoveryHandler.java
│       │   │       │   ├── JobRecoveryHandlerRegistry.java
│       │   │       │   ├── PendingJobRecoveryHandler.java
│       │   │       │   └── ProcessingJobRecoveryHandler.java
│       │   │       └── policy/
│       │   │           ├── JobRecoveryProperties.java
│       │   │           ├── RetryPolicy.java
│       │   │           └── StaleThresholdPolicy.java
│       │   │
│       │   ├── parser/
│       │   │   ├── AIResponseParser.java
│       │   │   ├── ExtractingJsonParser.java
│       │   │   ├── ParsedResponse.java
│       │   │   ├── Parser.java
│       │   │   ├── RelaxedJsonParser.java
│       │   │   └── StrictJsonParser.java
│       │   │
│       │   ├── production/
│       │   │   ├── ArtifactAccessService.java
│       │   │   ├── ArtifactAccessServiceImpl.java
│       │   │   ├── ProductionArtifactService.java
│       │   │   └── presign/
│       │   │       ├── DocumentPresignedStrategy.java
│       │   │       ├── ImagePresignedStrategy.java
│       │   │       ├── PresignedStrategy.java
│       │   │       ├── PresignedStrategyResolver.java
│       │   │       ├── PresignedUrlGenerator.java
│       │   │       └── S3PresignedUrlGenerator.java
│       │   │
│       │   ├── prompt/
│       │   │   ├── MergedPrompt.java
│       │   │   ├── PromptMerger.java
│       │   │   ├── PromptTemplateService.java
│       │   │   └── PromptValidator.java
│       │   │
│       │   ├── renderer/
│       │   │   ├── BlogRenderer.java
│       │   │   ├── DocumentRenderer.java
│       │   │   ├── EmailRenderer.java
│       │   │   ├── ImageRenderer.java
│       │   │   ├── ProductionRenderer.java
│       │   │   ├── RendererRegistry.java
│       │   │   └── TextRenderer.java
│       │   │
│       │   ├── storage/
│       │   │   ├── S3StorageStrategy.java
│       │   │   ├── StorageCleanupScheduler.java
│       │   │   ├── StorageLifecycleService.java
│       │   │   ├── StorageStrategy.java
│       │   │   ├── StorageStrategyFactory.java
│       │   │   ├── StorageType.java
│       │   │   └── exception/
│       │   │       └── AbstractStorageException.java
│       │   │
│       │   └── validator/
│       │       ├── BlogResponseValidator.java
│       │       ├── ContentResponseValidator.java
│       │       ├── DocumentResponseValidator.java
│       │       ├── EmailResponseValidator.java
│       │       ├── ResponseValidator.java
│       │       └── ValidatorFactory.java
│       │
│       ├── util/
│       │   └── ArtifactMetadataHelper.java
│       │
│       └── validation/
│           ├── BlogCommandValidator.java
│           ├── DocumentCommandValidator.java
│           ├── DocumentFormat.java
│           ├── EmailCommandValidator.java
│           ├── FormatValidator.java
│           ├── ImageCommandValidator.java
│           ├── ProductionValidator.java
│           ├── TextCommandValidator.java
│           ├── TextFormat.java
│           ├── ValidationException.java
│           └── ValidatorRegistry.java
│
├── dto/
│   ├── request/
│   │   └── production/
│   │       ├── BlogProductionRequestDto.java
│   │       ├── DocumentProductionRequestDto.java
│   │       ├── EmailProductionRequestDto.java
│   │       ├── ImageProductionRequestDto.java
│   │       ├── ProductionRequest.java
│   │       └── TextProductionRequestDto.java
│   │
│   └── response/
│       └── production/
│           ├── ArtifactDetailResponseDto.java
│           ├── ArtifactDto.java
│           ├── ArtifactDtoMapper.java
│           ├── ArtifactSummaryDto.java
│           ├── FileArtifactDto.java
│           ├── FileBasedArtifactDto.java
│           ├── ImageArtifactDto.java
│           ├── JobResponseDto.java
│           ├── ProductionResponseDto.java
│           ├── ProductionResponseDtoMapper.java
│           ├── ProductionStatus.java
│           └── TextArtifactDto.java
│
├── exception/
│   ├── BaseException.java
│   ├── ModuleErrorCode.java
│   ├── ModuleExceptionDto.java
│   ├── ModuleExceptionHandler.java
│   └── ModuleResponse.java
│
└── presentation/
    └── StorageController.java
```

## 디렉토리 구조 설명

### controller/
- HTTP 요청을 처리하는 컨트롤러 클래스들
- `production/`: 프로덕션 관련 컨트롤러
- `delivery/`: 배송 관련 컨트롤러 (비어있음)

### domain/production/
도메인 로직을 담당하는 핵심 패키지

#### application/
- 애플리케이션 서비스 레이어
- `factory/`: Command Factory 패턴 구현
- `storage/`: 스토리지 관련 서비스

#### config/
- 설정 관련 클래스
- S3 설정 및 조건부 빈 설정

#### entity/
- JPA 엔티티 클래스
- `job/`: 작업 엔티티
- `production/`: 프로덕션 아티팩트 엔티티

#### infra/
- 인프라스트럭처 레이어
- `storage/`: S3 관련 서비스 구현

#### model/
- 도메인 모델 클래스
- `ai/`: AI 관련 모델
- `contract/`: 계약 인터페이스 (command, result)
- `executor/`: 실행자 모델
- `job/`: 작업 모델
- `storage/`: 스토리지 모델
- `tenant/`: 테넌트 컨텍스트

#### repository/
- 데이터 접근 레이어 (JPA Repository)

#### service/
비즈니스 로직을 담당하는 서비스 레이어
- `ai/`: AI 서비스 (텍스트, 이미지)
- `artifact/`: 아티팩트 핸들러
- `cdn/`: CDN URL 제공자
- `format/`: 포맷 변환기
- `image/`: 이미지 처리
- `job/`: 작업 처리 (큐, 스케줄러, 프로세스)
- `parser/`: 파서
- `production/`: 프로덕션 서비스
- `prompt/`: 프롬프트 처리
- `renderer/`: 렌더러
- `storage/`: 스토리지 전략
- `validator/`: 검증기

#### util/
- 유틸리티 클래스

#### validation/
- 검증 관련 클래스

### dto/
- 데이터 전송 객체
- `request/`: 요청 DTO
- `response/`: 응답 DTO

### exception/
- 예외 처리 관련 클래스
- 전역 예외 핸들러 포함

### presentation/
- 프레젠테이션 레이어
- 스토리지 컨트롤러

잘된 점
1. 아키텍처
   계층 분리: Controller → Application → Domain → Infra
   DDD 적용: Entity, Repository, Service, Application Service 구분
   패키지 구조: 기능별 모듈화
2. 디자인 패턴
   전략 패턴: StorageStrategy, PresignedStrategy, ArtifactHandler
   팩토리 패턴: ProductionCommandFactory + Registry
   퍼사드 패턴: StorageFacade
   레지스트리 패턴: 여러 Registry 클래스
3. 보안
   소유권 검증: resolveS3KeyWithOwnerCheck
   파일명 정리: sanitizeFileName로 경로 탐색 공격 방지
   Presigned URL 캐싱: Redis로 성능 최적화
4. 성능
   Range 다운로드: 이미지 포맷 감지 최적화
   Redis 캐싱: Presigned URL 캐싱
   레지스트리 O(1) 조회
5. 예외 처리
   BaseException + ModuleErrorCode로 일관성
   전역 예외 핸들러
   적절한 HTTP 상태 코드 매핑
   아쉬운 점 및 개선 제안
1. 코드 중복
   문제 1: extractS3Key 메서드가 4곳에 중복
   // ArtifactApplicationService.java (132-144)// StorageCommandService.java (114-126)// ArtifactAccessServiceImpl.java (120-139)// StorageFacade.java에는 없지만 extractFileName만 있음
   일관성 부족: 일부는 null 반환, 일부는 예외 발생
   유지보수 어려움: 로직 변경 시 여러 곳 수정 필요
   문제 2: extractFileName 메서드가 5곳에 중복
   // ArtifactMetadataHelper.java (13-19) - 유틸리티로 존재// StorageFacade.java (133-141)// DocumentPresignedStrategy.java (40-49)// ThumbnailService.java (100-104)// ImageArtifactHandler.java는 ArtifactMetadataHelper 사용
   ArtifactMetadataHelper가 있음에도 중복 구현 존재
   문제 3: buildContentDisposition 메서드가 2곳에 중복
   // StorageFacade.java (150-165)// DocumentPresignedStrategy.java (58-73)
   동일 로직이 두 곳에 존재
   개선 제안:
   // 공통 유틸리티 클래스 생성public final class S3PathUtils {    public static String extractS3Key(String filePath) { ... }    public static String extractFileName(String filePath) { ... }}public final class ContentDispositionBuilder {    public static String buildAttachment(String fileName) { ... }}
2. 설정 값 중복 및 일관성
   문제: 같은 설정이 여러 곳에서 주입됨
   // S3UploadService, S3DownloadService, S3PresignedUrlService 모두@Value("${production.storage.s3.bucket}")private String bucket;// ArtifactApplicationService, ArtifactAccessServiceImpl@Value("${artifact.url.default-ttl:300}")private int presignedUrlTtlSeconds;
   개선 제안:
   @ConfigurationProperties(prefix = "production.storage.s3")public class S3StorageProperties {    private String bucket;    private String prefix = "production";    private PresignedUrl presignedUrl = new PresignedUrl();        @Data    public static class PresignedUrl {        private int ttlSeconds = 300;    }}
3. TenantContext null 처리 일관성 부족
   문제: null 처리 방식이 일관되지 않음
   // StorageFacade.java - 경고만 하고 계속 진행if (tenantId == null || tenantId.isBlank()) {    log.warn("...");}return uploadService.upload(content, tenantId, userId, jobId, fileName);// StorageCommandService.java - 경고만 하고 계속 진행// ProductionArtifactService.java - null 체크 없음
   개선 제안:
   정책 명확화: null 허용 여부와 기본값/실패 처리 결정
   헬퍼 메서드 제공:
   public static String requireTenantId() {    String tenantId = getCurrentTenantId();    if (tenantId == null || tenantId.isBlank()) {        throw new IllegalStateException("TenantContext is required");    }    return tenantId;}
4. 예외 처리 일관성
   문제 1: extractS3Key의 예외 처리 불일치
   // ArtifactApplicationService - null 반환private String extractS3Key(String filePath) {    if (filePath == null || filePath.isBlank()) {        return null;  // null 반환    }    ...}// ArtifactAccessServiceImpl - 예외 발생private String extractS3Key(String filePath) {    if (filePath == null || filePath.isBlank()) {        throw new BaseException(...)
   문제 2: PresignedStrategyResolver의 예외 처리
   // PresignedStrategyResolver.java (26-38)// IllegalArgumentException을 던지지만, ArtifactApplicationService에서는 null 체크만 함PresignedStrategy strategy = presignedStrategyResolver.resolve(contentType);if (strategy == null) {  // resolve()는 예외를 던지므로 이 체크는 불필요    return null;}
5. 보안 이슈
   문제 1: sanitizeFileName의 취약점
   // S3KeyGenerator.java (56)sanitized = sanitized.replace("..", "");  // "...." -> "" (빈 문자열)
   "...." → ""로 변환되어 기본값 "output" 사용
   더 엄격한 검증 필요
   개선 제안:
   if (sanitized.contains("..")) {    throw new IllegalArgumentException("Invalid file name: " + fileName);}
   문제 2: 파일 크기 제한이 다운로드에만 적용
   // S3DownloadService.java에는 maxDownloadSizeBytes 체크가 있지만// S3UploadService.java에는 업로드 크기 제한이 없음
6. 트랜잭션 경계
   문제: @Async와 @Transactional 조합
   // ThumbnailService.java (33-35)@Async("thumbnailTaskExecutor")@Transactionalpublic void generateThumbnailsAsync(...) {
   비동기 메서드에서 트랜잭션 전파가 예상과 다를 수 있음
   @Async는 프록시를 통해 동작하므로 트랜잭션 경계 확인 필요
7. 의존성 방향
   문제: Domain Service가 Application Service에 의존
   // ArtifactAccessServiceImpl (Domain Service)가// StorageFacade (Application Service)에 의존private final StorageFacade storageFacade;
   일반적으로 Domain → Application 의존은 피해야 함
   StorageFacade를 인터페이스로 분리하거나 의존성 역전 필요
8. 매직 넘버/문자열
   문제: 하드코딩된 값들
   // ImageArtifactHandler.javabyte[] fileHeader = storageFacade.downloadRange(filePath, 0, 11);  // 11은 왜?// ArtifactAccessServiceImpl.javathis.cacheTtl = Duration.ofSeconds(Math.max(ttlSeconds - 30, ttlSeconds / 2));  // 30은 왜?// S3DownloadService.java@Value("${production.storage.s3.max-download-size-bytes:104857600}")  // 100MB
   개선 제안:
   private static final int MAX_IMAGE_HEADER_BYTES = 12;  // WebP 감지에 필요private static final int CACHE_TTL_BUFFER_SECONDS = 30;
9. null 안전성
   문제: null 체크 일관성 부족
   // ProductionArtifactDetailEntity.java (33-35)public Boolean getIsPrimary() {    return isPrimary != null ? isPrimary : false;  // null 체크 있음}// 하지만 다른 곳에서는 null 체크 없이 사용detail.getIsPrimary()  // null일 수 있음
10. Deprecated 메서드
    문제: Deprecated 메서드가 여전히 사용 가능
    // ProductionArtifactEntity.java@Deprecatedpublic ProductionArtifactDetailEntity getDetail() { ... }// StorageCommandService.java@Deprecatedpublic String generateDownloadPresignedUrl(String s3Key) { ... }
    사용처 확인 후 제거 계획 필요
11. 로깅 일관성
    문제: 로그 레벨과 메시지 형식 불일치
    // 일부는 debug, 일부는 infolog.debug("Generating presigned URL...");log.info("Presigned URL generated...");
12. 테스트 가능성
    문제: 테스트 코드 부재
    복잡한 로직(이미지 포맷 감지, Content-Disposition 생성 등)에 대한 테스트 필요
    Mock 객체 주입이 어려운 구조 (final 필드, 생성자 주입)
    우선순위별 개선 제안
    높은 우선순위
    코드 중복 제거: extractS3Key, extractFileName, buildContentDisposition 통합
    보안 강화: sanitizeFileName 개선, 업로드 크기 제한 추가
    예외 처리 일관성: null 처리 정책 통일
    중간 우선순위
    설정 값 통합: @ConfigurationProperties 사용
    TenantContext 정책 명확화
    의존성 방향 개선: Domain → Application 의존 제거
    낮은 우선순위
    매직 넘버 상수화
    Deprecated 메서드 제거
    테스트 코드 작성
    로깅 일관성 개선
    전반적으로 구조와 패턴 사용은 적절합니다. 중복 제거와 일관성 개선으로 유지보수성을 높일 수 있습니다.