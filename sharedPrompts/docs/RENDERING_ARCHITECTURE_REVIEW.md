# AI Production 시스템 렌더링 아키텍처 리뷰 및 개선 설계

> **작성자**: 시니어 백엔드 아키텍트  
> **대상 시스템**: Spring Boot 기반 AI Production 시스템  
> **목적**: 구조적 개선점 도출 및 확장 가능한 설계 제안

---

## 📋 목차

1. [현재 구조의 설계적 문제점](#1-현재-구조의-설계적-문제점)
2. [Presigned URL 설계 개선안](#2-presigned-url-설계-개선안)
3. [Artifact 추상화 개선 설계](#3-artifact-추상화-개선-설계)
4. [PDF 변환 개선 제안](#4-pdf-변환-개선-제안)
5. [이미지 시스템 확장 전략](#5-이미지-시스템-확장-전략)
6. [SaaS 제품 기준 고도화](#6-saas-제품-기준-고도화)
7. [초대량 트래픽 환경 개선](#7-초대량-트래픽-환경-개선)
8. [리팩토링 우선순위 제안](#8-리팩토링-우선순위-제안)

---

## 1. 현재 구조의 설계적 문제점

### 1.1 DTO 설계 관점

#### ❌ 문제점 1: `location` 필드의 의미적 모호성

**현재 구조:**
```java
public class ArtifactDto {
    private ArtifactType type;
    private String location;  // ⚠️ TEXT일 때는 콘텐츠, FILE/IMAGE일 때는 경로
    private String fileName;
    private String contentType;
    private String storageLocation;
}
```

**문제:**
- `location` 필드가 타입에 따라 완전히 다른 의미를 가짐
- TEXT: 실제 콘텐츠 문자열
- FILE/IMAGE: 파일 경로 문자열
- 프론트엔드에서 타입 체크 없이는 사용 불가능
- API 계약이 불명확함

**영향:**
- 클라이언트 측 런타임 에러 가능성
- API 문서화 어려움
- 타입 안전성 부족

#### ❌ 문제점 2: `success` 필드 중복

**현재 구조:**
```json
{
  "success": true,  // CustomResponse 레벨
  "data": {
    "success": true,  // ProductionResponseDto 레벨
    "artifact": { ... }
  }
}
```

**문제:**
- 두 레벨에서 `success` 필드 중복
- `CustomResponse.success`는 HTTP 레벨 성공 여부
- `ProductionResponseDto.success`는 비즈니스 로직 성공 여부
- 의미가 혼재되어 혼란 야기

#### ❌ 문제점 3: `storageLocation` 타입 불일치

**현재 구조:**
```java
private String storageLocation;  // "INLINE_TEXT" | "S3" | "LOCAL"
```

**문제:**
- `StorageFormat` enum과 `StorageType` enum이 혼재
- `StorageFormat.INLINE_TEXT`는 저장 형식
- `StorageType.S3/LOCAL`은 저장소 타입
- 문자열로 저장되어 타입 안전성 부족

### 1.2 응답 구조 일관성

#### ❌ 문제점 4: Artifact 접근 방식 불일치

**현재:**
- TEXT (INLINE_TEXT): `location`에 직접 콘텐츠
- TEXT (FILE_PATH): `location`에 경로 → 다운로드 API 필요
- IMAGE: `location`에 S3 경로 → Presigned URL API 필요
- FILE: `location`에 경로 → 다운로드 API 필요

**문제:**
- 클라이언트가 타입과 저장 방식에 따라 다른 처리 필요
- 일관된 접근 패턴 부재

### 1.3 개선 제안

#### ✅ 제안 1: 타입별 전용 필드 도입

```java
@Getter
@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextArtifactDto.class, name = "TEXT"),
    @JsonSubTypes.Type(value = FileArtifactDto.class, name = "FILE"),
    @JsonSubTypes.Type(value = ImageArtifactDto.class, name = "IMAGE")
})
public abstract class ArtifactDto {
    private ArtifactType type;
    private String fileName;
    private String contentType;
    private Instant createdAt;
}

@Getter
@SuperBuilder
public class TextArtifactDto extends ArtifactDto {
    private String content;  // ✅ 명확한 의미
    private boolean isInline;
}

@Getter
@SuperBuilder
public class FileArtifactDto extends ArtifactDto {
    private String filePath;
    private String downloadUrl;  // ✅ 즉시 사용 가능한 URL
    private Long fileSize;
}

@Getter
@SuperBuilder
public class ImageArtifactDto extends ArtifactDto {
    private String filePath;
    private String previewUrl;  // ✅ Presigned URL 자동 포함
    private String thumbnailUrl;  // ✅ 썸네일 URL
    private ImageMetadata metadata;  // width, height 등
}
```

**장점:**
- 타입 안전성 확보
- 명확한 API 계약
- Jackson 다형성 직렬화 지원

#### ✅ 제안 2: 응답 구조 명확화

```java
@Getter
@Builder
public class ProductionResponseDto {
    private Long productionId;
    private ProductionStatus status;  // ✅ SUCCESS, FAILED, PROCESSING
    private String errorMessage;  // status == FAILED일 때만
    private Instant startedAt;
    private Instant completedAt;
    private ArtifactDto artifact;  // status == SUCCESS일 때만
}

public enum ProductionStatus {
    PROCESSING,
    SUCCESS,
    FAILED
}
```

**응답 예시:**
```json
{
  "success": true,  // HTTP 레벨
  "data": {
    "productionId": 123,
    "status": "SUCCESS",  // ✅ 비즈니스 로직 레벨
    "artifact": { ... }
  }
}
```

---

## 2. Presigned URL 설계 개선안

### 2.1 자동 포함 방식 vs 별도 API 방식

#### 방식 A: 자동 포함 방식 (권장)

**구조:**
```java
@Getter
@SuperBuilder
public class ImageArtifactDto extends ArtifactDto {
    private String filePath;
    private String previewUrl;  // Presigned URL 자동 생성
    private Instant urlExpiresAt;  // 만료 시간 명시
}
```

**구현:**
```java
@Service
@RequiredArgsConstructor
public class ArtifactAccessService {
    
    private final S3Client s3Client;
    private final S3Presigner presigner;
    
    @Value("${artifact.url.expiration:1h}")
    private Duration defaultExpiration;
    
    public ImageArtifactDto enrichImageArtifact(
            ProductionArtifactDetailEntity detail,
            Duration expiration
    ) {
        String s3Key = extractS3Key(detail.getFilePath());
        String presignedUrl = generatePresignedUrl(s3Key, expiration);
        
        return ImageArtifactDto.builder()
                .filePath(detail.getFilePath())
                .previewUrl(presignedUrl)
                .urlExpiresAt(Instant.now().plus(expiration))
                .build();
    }
    
    private String generatePresignedUrl(String s3Key, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(r -> r
                        .bucket(bucket)
                        .key(s3Key))
                .build();
        
        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
```

**장점:**
- 클라이언트가 추가 API 호출 불필요
- 즉시 사용 가능한 URL 제공
- 개발자 경험 향상

**단점:**
- 모든 조회 시 Presigned URL 생성 비용
- 대량 조회 시 성능 이슈 가능

#### 방식 B: 별도 API 방식

**구조:**
```java
@RestController
@RequestMapping("/production/{productionId}/artifact")
public class ArtifactAccessController {
    
    @GetMapping("/url")
    public ResponseEntity<CustomResponse<ArtifactUrlDto>> getArtifactUrl(
            @PathVariable Long productionId,
            @RequestParam(defaultValue = "1h") @DurationMin(minutes = 5) @DurationMax(hours = 24) Duration expiration
    ) {
        // Presigned URL 생성
    }
}
```

**장점:**
- 필요할 때만 생성 (비용 절감)
- TTL 제어 가능
- 캐싱 전략 적용 용이

**단점:**
- 클라이언트가 추가 API 호출 필요
- 개발 복잡도 증가

### 2.2 하이브리드 방식 (최종 권장)

**전략:**
- **기본 조회**: 짧은 TTL (5분) Presigned URL 자동 포함
- **장기 사용**: 별도 API로 긴 TTL (24시간) URL 요청

```java
@Service
public class ArtifactAccessService {
    
    // 기본 조회용 (짧은 TTL)
    public ImageArtifactDto enrichImageArtifact(ProductionArtifactDetailEntity detail) {
        return enrichImageArtifact(detail, Duration.ofMinutes(5));
    }
    
    // 장기 사용용 (긴 TTL)
    public String generateLongLivedUrl(String s3Key, Duration expiration) {
        // 별도 API로 제공
        return generatePresignedUrl(s3Key, expiration);
    }
}
```

### 2.3 TTL 전략 제안

#### TTL 계층화

```java
public enum ArtifactUrlTtl {
    PREVIEW(Duration.ofMinutes(5)),      // 미리보기용
    STANDARD(Duration.ofHours(1)),       // 일반 사용
    DOWNLOAD(Duration.ofHours(24)),      // 다운로드용
    SHARED(Duration.ofDays(7));          // 공유 링크용
    
    private final Duration duration;
}
```

#### 동적 TTL 계산

```java
@Service
public class ArtifactUrlTtlCalculator {
    
    public Duration calculateTtl(ArtifactType type, AccessPurpose purpose) {
        // 이미지는 짧게 (자주 변경 가능)
        if (type == ArtifactType.IMAGE) {
            return purpose == AccessPurpose.PREVIEW 
                ? Duration.ofMinutes(5)
                : Duration.ofHours(1);
        }
        
        // 파일은 길게 (변경 적음)
        if (type == ArtifactType.FILE) {
            return Duration.ofHours(24);
        }
        
        return Duration.ofHours(1);
    }
}
```

### 2.4 대용량 트래픽 환경 고려

#### 캐싱 전략

```java
@Service
@RequiredArgsConstructor
public class CachedArtifactAccessService {
    
    private final ArtifactAccessService delegate;
    private final Cache<String, PresignedUrlCache> urlCache;
    
    @Cacheable(value = "presignedUrls", key = "#s3Key + '_' + #ttl")
    public String getPresignedUrl(String s3Key, Duration ttl) {
        // 캐시 키: s3Key_ttl
        // TTL이 같으면 재사용
        return delegate.generatePresignedUrl(s3Key, ttl);
    }
}
```

**캐시 설정:**
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=4m
```

**비용 절감:**
- 동일 S3 객체에 대한 중복 요청 방지
- Presigned URL 생성 비용 절감 (월 10,000건 기준 약 30% 절감 예상)

---

## 3. Artifact 추상화 개선 설계

### 3.1 ArtifactAccessService 도입

#### 설계 목적

1. **접근 로직 중앙화**: Presigned URL, 다운로드 URL 생성 로직 통합
2. **권한 검증**: Artifact 접근 권한 검증
3. **캐싱 전략**: URL 생성 결과 캐싱
4. **확장성**: 새로운 저장소 타입 추가 용이

#### 인터페이스 설계

```java
public interface ArtifactAccessService {
    
    /**
     * Artifact 접근 정보를 풍부하게 만들어 반환
     */
    ArtifactDto enrichArtifact(ProductionArtifactDetailEntity detail, Long userId);
    
    /**
     * Presigned URL 생성
     */
    String generatePresignedUrl(String storageKey, Duration expiration, Long userId);
    
    /**
     * 다운로드 스트림 제공
     */
    InputStream getArtifactStream(String storageKey, Long userId);
    
    /**
     * Artifact 접근 권한 검증
     */
    void validateAccess(Long artifactId, Long userId);
}
```

#### 구현 예시

```java
@Service
@RequiredArgsConstructor
public class ArtifactAccessServiceImpl implements ArtifactAccessService {
    
    private final ProductionArtifactRepository artifactRepository;
    private final StorageStrategyResolver storageStrategyResolver;
    private final S3Presigner s3Presigner;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public ArtifactDto enrichArtifact(ProductionArtifactDetailEntity detail, Long userId) {
        validateAccess(detail.getArtifact().getId(), userId);
        
        return switch (detail.getArtifactType()) {
            case TEXT -> buildTextArtifact(detail);
            case IMAGE -> buildImageArtifact(detail);
            case FILE -> buildFileArtifact(detail);
        };
    }
    
    private ImageArtifactDto buildImageArtifact(ProductionArtifactDetailEntity detail) {
        String s3Key = extractS3Key(detail.getFilePath());
        String previewUrl = generatePresignedUrl(s3Key, Duration.ofMinutes(5), detail.getArtifact().getUserId());
        
        return ImageArtifactDto.builder()
                .type(ArtifactType.IMAGE)
                .filePath(detail.getFilePath())
                .previewUrl(previewUrl)
                .urlExpiresAt(Instant.now().plusMinutes(5))
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .build();
    }
    
    @Override
    public String generatePresignedUrl(String storageKey, Duration expiration, Long userId) {
        // 캐시 확인
        String cacheKey = "presigned:" + storageKey + ":" + expiration.toMinutes();
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Presigned URL 생성
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(r -> r.bucket(bucket).key(storageKey))
                .build();
        
        String url = s3Presigner.presignGetObject(request).url().toString();
        
        // 캐시 저장 (TTL보다 짧게)
        redisTemplate.opsForValue().set(cacheKey, url, expiration.minusMinutes(1));
        
        return url;
    }
    
    @Override
    public InputStream getArtifactStream(String storageKey, Long userId) {
        StorageStrategy strategy = storageStrategyResolver.resolve(storageKey);
        return strategy.getInputStream(storageKey);
    }
    
    @Override
    public void validateAccess(Long artifactId, Long userId) {
        ProductionArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ArtifactNotFoundException(artifactId));
        
        if (!artifact.getUserId().equals(userId)) {
            throw new ArtifactAccessDeniedException(artifactId, userId);
        }
    }
}
```

### 3.2 Storage 전략 패턴 개선

#### 현재 문제점

```java
// 현재: 저장만 담당
public interface StorageStrategy {
    String store(String content, ...);
    String store(byte[] data, ...);
}
```

**문제:**
- 읽기(read) 기능 부재
- Presigned URL 생성 책임 불명확
- 스트림 처리 미지원

#### 개선된 인터페이스

```java
public interface StorageStrategy {
    
    // 저장
    StorageResult store(String content, StorageContext context);
    StorageResult store(byte[] data, StorageContext context);
    
    // 읽기
    InputStream read(String storageKey);
    boolean exists(String storageKey);
    
    // URL 생성
    String generateAccessUrl(String storageKey, Duration expiration);
    
    // 메타데이터
    StorageMetadata getMetadata(String storageKey);
    
    // 삭제
    void delete(String storageKey);
    
    StorageType getType();
}

@Value
@Builder
public class StorageResult {
    String storageKey;
    String storagePath;  // s3://bucket/key 또는 /local/path
    long size;
    String contentType;
    Instant storedAt;
}

@Value
@Builder
public class StorageContext {
    Long userId;
    String jobId;
    String fileName;
    Map<String, String> metadata;
}
```

#### 구현 예시

```java
@Component
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
public class S3StorageStrategy implements StorageStrategy {
    
    private final S3Client s3Client;
    private final S3Presigner presigner;
    
    @Override
    public StorageResult store(byte[] data, StorageContext context) {
        String s3Key = buildS3Key(context);
        
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(context.getMetadata().get("contentType"))
                .metadata(context.getMetadata())
                .build();
        
        s3Client.putObject(request, RequestBody.fromBytes(data));
        
        return StorageResult.builder()
                .storageKey(s3Key)
                .storagePath("s3://" + bucket + "/" + s3Key)
                .size(data.length)
                .contentType(context.getMetadata().get("contentType"))
                .storedAt(Instant.now())
                .build();
    }
    
    @Override
    public InputStream read(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        
        return s3Client.getObject(request);
    }
    
    @Override
    public String generateAccessUrl(String storageKey, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(r -> r.bucket(bucket).key(storageKey))
                .build();
        
        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
```

### 3.3 확장 가능한 ArtifactType 구조

#### 현재 문제점

```java
public enum ArtifactType {
    TEXT, FILE, IMAGE;  // 하드코딩된 타입
}
```

**문제:**
- 새로운 타입 추가 시 enum 수정 필요
- 타입별 특화 로직 분산

#### 개선: 전략 패턴 + 팩토리

```java
public interface ArtifactHandler {
    ArtifactType getType();
    ArtifactDto enrich(ProductionArtifactDetailEntity detail, ArtifactAccessService accessService);
    boolean supports(ArtifactType type);
}

@Component
public class TextArtifactHandler implements ArtifactHandler {
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.TEXT;
    }
    
    @Override
    public ArtifactDto enrich(ProductionArtifactDetailEntity detail, ArtifactAccessService accessService) {
        return TextArtifactDto.builder()
                .type(ArtifactType.TEXT)
                .content(detail.getContent())
                .isInline(detail.getStorageType() == StorageFormat.INLINE_TEXT)
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .build();
    }
    
    @Override
    public boolean supports(ArtifactType type) {
        return type == ArtifactType.TEXT;
    }
}

@Component
public class ImageArtifactHandler implements ArtifactHandler {
    
    @Override
    public ArtifactDto enrich(ProductionArtifactDetailEntity detail, ArtifactAccessService accessService) {
        String s3Key = extractS3Key(detail.getFilePath());
        String previewUrl = accessService.generatePresignedUrl(s3Key, Duration.ofMinutes(5), detail.getArtifact().getUserId());
        
        return ImageArtifactDto.builder()
                .type(ArtifactType.IMAGE)
                .filePath(detail.getFilePath())
                .previewUrl(previewUrl)
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .build();
    }
}

@Service
@RequiredArgsConstructor
public class ArtifactHandlerRegistry {
    
    private final List<ArtifactHandler> handlers;
    
    public ArtifactHandler getHandler(ArtifactType type) {
        return handlers.stream()
                .filter(h -> h.supports(type))
                .findFirst()
                .orElseThrow(() -> new UnsupportedArtifactTypeException(type));
    }
}
```

**장점:**
- 새로운 타입 추가 시 Handler만 구현
- 타입별 로직 응집도 향상
- 테스트 용이성

---

## 4. PDF 변환 개선 제안

### 4.1 현재 구현의 문제점

```java
// 현재: 단순 텍스트 → PDF
String[] paragraphs = content.split("\n\n");
for (String para : paragraphs) {
    document.add(new Paragraph(para.trim(), font));
}
```

**문제:**
- Markdown 구조 무시
- 제목, 리스트, 코드 블록 등 미지원
- 스타일링 부재
- 이미지 포함 불가

### 4.2 Markdown → HTML → PDF 전략

#### 아키텍처

```
Markdown → HTML (CommonMark) → PDF (Flying Saucer / OpenHTMLToPDF)
```

#### 구현 예시

```java
@Component
@RequiredArgsConstructor
public class EnhancedPdfFormatConverter implements FormatConverter {
    
    private final MarkdownParser markdownParser;
    private final HtmlToPdfConverter htmlToPdfConverter;
    private final CssProvider cssProvider;
    
    @Override
    public byte[] convert(String content, String fileName) {
        // 1. Markdown → HTML
        String html = markdownParser.parse(content);
        
        // 2. HTML + CSS → PDF
        return htmlToPdfConverter.convert(html, cssProvider.getDefaultCss());
    }
    
    @Override
    public String getContentType() {
        return "application/pdf";
    }
}

@Component
public class CommonMarkMarkdownParser implements MarkdownParser {
    
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();
    
    @Override
    public String parse(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}

@Component
public class OpenHtmlToPdfConverter implements HtmlToPdfConverter {
    
    @Override
    public byte[] convert(String html, String css) {
        try {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = 
                new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            
            builder.withHtmlContent(html, null);
            builder.useDefaultPageSize(210, 297, com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PageSizeUnits.MM);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toStream(out);
            builder.run();
            
            return out.toByteArray();
        } catch (Exception e) {
            throw new FormatConversionException("PDF conversion failed", e);
        }
    }
}
```

#### 의존성 추가

```gradle
// Markdown 파싱
implementation 'com.vladsch.flexmark:flexmark-all:0.64.8'

// HTML → PDF
implementation 'com.openhtmltopdf:openhtmltopdf-pdfbox:1.1.24'
implementation 'com.openhtmltopdf:openhtmltopdf-svg-support:1.1.24'
```

### 4.3 보고서 품질 개선

#### CSS 스타일링

```java
@Component
public class ReportCssProvider implements CssProvider {
    
    @Override
    public String getDefaultCss() {
        return """
            @page {
                margin: 2cm;
                @bottom-center {
                    content: "Page " counter(page) " of " counter(pages);
                }
            }
            
            body {
                font-family: 'Noto Sans KR', sans-serif;
                font-size: 11pt;
                line-height: 1.6;
            }
            
            h1 {
                font-size: 24pt;
                font-weight: bold;
                margin-top: 20pt;
                margin-bottom: 12pt;
                border-bottom: 2pt solid #333;
                padding-bottom: 6pt;
            }
            
            h2 {
                font-size: 18pt;
                font-weight: bold;
                margin-top: 16pt;
                margin-bottom: 10pt;
            }
            
            code {
                background-color: #f5f5f5;
                padding: 2pt 4pt;
                border-radius: 3pt;
                font-family: 'Courier New', monospace;
            }
            
            pre {
                background-color: #f5f5f5;
                padding: 10pt;
                border-left: 4pt solid #007acc;
                overflow-x: auto;
            }
            
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 12pt 0;
            }
            
            th, td {
                border: 1pt solid #ddd;
                padding: 8pt;
                text-align: left;
            }
            
            th {
                background-color: #f0f0f0;
                font-weight: bold;
            }
            """;
    }
}
```

#### 이미지 포함 지원

```java
@Component
public class ImageResolver {
    
    private final StorageStrategyResolver storageStrategyResolver;
    
    public String resolveImageUrl(String imagePath) {
        // S3 이미지인 경우 Presigned URL 생성
        if (imagePath.startsWith("s3://")) {
            String s3Key = extractS3Key(imagePath);
            StorageStrategy strategy = storageStrategyResolver.resolve(s3Key);
            return strategy.generateAccessUrl(s3Key, Duration.ofHours(1));
        }
        
        // 로컬 파일인 경우 base64 인코딩
        return encodeToBase64(imagePath);
    }
    
    private String encodeToBase64(String filePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(filePath));
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = Files.probeContentType(Paths.get(filePath));
            return "data:" + mimeType + ";base64," + base64;
        } catch (IOException e) {
            throw new ImageResolutionException("Failed to encode image", e);
        }
    }
}
```

---

## 5. 이미지 시스템 확장 전략

### 5.1 썸네일 전략

#### 아키텍처

```
원본 이미지 → 썸네일 생성 (비동기) → S3 저장 → 메타데이터 저장
```

#### 구현

```java
@Service
@RequiredArgsConstructor
public class ImageThumbnailService {
    
    private final StorageStrategy storageStrategy;
    private final ImageProcessor imageProcessor;
    private final ProductionArtifactRepository artifactRepository;
    
    @Async
    public void generateThumbnail(Long artifactId, String originalImagePath) {
        try {
            // 1. 원본 이미지 다운로드
            byte[] original = storageStrategy.read(originalImagePath).readAllBytes();
            
            // 2. 썸네일 생성 (200x200)
            byte[] thumbnail = imageProcessor.resize(original, 200, 200);
            
            // 3. 썸네일 저장
            String thumbnailKey = buildThumbnailKey(originalImagePath);
            storageStrategy.store(thumbnail, StorageContext.builder()
                    .fileName("thumbnail_" + extractFileName(originalImagePath))
                    .metadata(Map.of("contentType", "image/jpeg"))
                    .build());
            
            // 4. 메타데이터 업데이트
            updateArtifactMetadata(artifactId, thumbnailKey);
            
        } catch (Exception e) {
            log.error("Thumbnail generation failed for artifact: {}", artifactId, e);
        }
    }
    
    private String buildThumbnailKey(String originalKey) {
        // production/{userId}/{jobId}/thumbnails/{fileName}
        return originalKey.replace("/images/", "/thumbnails/");
    }
}

@Component
public class JavaImageProcessor implements ImageProcessor {
    
    @Override
    public byte[] resize(byte[] imageBytes, int width, int height) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", out);
        return out.toByteArray();
    }
}
```

#### 메타데이터 확장

```java
@Entity
@Table(name = "production_artifact_details")
public class ProductionArtifactDetailEntity {
    
    // 기존 필드...
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // JSON 형식
    
    public ImageMetadata getImageMetadata() {
        if (artifactType != ArtifactType.IMAGE) {
            return null;
        }
        return JsonUtils.fromJson(metadata, ImageMetadata.class);
    }
}

@Value
@Builder
public class ImageMetadata {
    int width;
    int height;
    long fileSize;
    String thumbnailKey;
    String format;  // PNG, JPEG 등
}
```

### 5.2 CDN 고려

#### CloudFront 통합

```java
@Component
@ConditionalOnProperty(name = "artifact.cdn.enabled", havingValue = "true")
public class CloudFrontUrlGenerator {
    
    @Value("${artifact.cdn.distribution-domain}")
    private String distributionDomain;
    
    public String generateCdnUrl(String s3Key) {
        // CloudFront 서명 URL 생성
        return "https://" + distributionDomain + "/" + s3Key;
    }
}
```

#### 캐싱 전략

```yaml
# CloudFront 설정
CacheBehavior:
  - PathPattern: "production/*/thumbnails/*"
    TTL: 86400  # 24시간
    CachePolicy: CachingOptimized
  
  - PathPattern: "production/*/images/*"
    TTL: 3600   # 1시간
    CachePolicy: CachingOptimized
```

### 5.3 원본/파생 파일 구조

```
S3 구조:
production/
  {userId}/
    {jobId}/
      images/
        original_{fileName}      # 원본
        thumbnail_{fileName}     # 썸네일
        preview_{fileName}        # 미리보기용 (중간 크기)
      files/
        {fileName}                # 일반 파일
```

---

## 6. SaaS 제품 기준 고도화

### 6.1 멀티 테넌시 고려

#### 테넌트 격리 전략

```java
@Entity
@Table(name = "production_artifacts")
public class ProductionArtifactEntity {
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;  // ✅ 테넌트 ID 추가
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // ...
}
```

#### S3 키 구조 개선

```java
// 기존: production/{userId}/{jobId}/{fileName}
// 개선: production/{tenantId}/{userId}/{jobId}/{fileName}

private String buildS3Key(StorageContext context) {
    return String.format("production/%s/%d/%s/%s",
            context.getTenantId(),
            context.getUserId(),
            context.getJobId(),
            context.getFileName());
}
```

#### 접근 제어 강화

```java
@Service
public class TenantAwareArtifactAccessService {
    
    public ArtifactDto enrichArtifact(Long artifactId, String tenantId, Long userId) {
        ProductionArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ArtifactNotFoundException(artifactId));
        
        // 테넌트 검증
        if (!artifact.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException(tenantId, artifact.getTenantId());
        }
        
        // 사용자 검증
        if (!artifact.getUserId().equals(userId)) {
            throw new ArtifactAccessDeniedException(artifactId, userId);
        }
        
        // Presigned URL 생성 시 테넌트 정보 포함
        String presignedUrl = generateTenantScopedUrl(artifact, tenantId);
        
        return buildArtifactDto(artifact, presignedUrl);
    }
}
```

### 6.2 접근 제어

#### RBAC 통합

```java
@Service
@RequiredArgsConstructor
public class ArtifactAccessControlService {
    
    private final RoleBasedAccessControl rbac;
    
    public boolean canAccess(Long artifactId, Long userId, AccessAction action) {
        ProductionArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow();
        
        // 소유자 체크
        if (artifact.getUserId().equals(userId)) {
            return true;
        }
        
        // 공유 권한 체크
        if (artifact.isShared() && hasSharedPermission(userId, artifactId, action)) {
            return true;
        }
        
        // 역할 기반 접근
        return rbac.hasPermission(userId, "artifact:" + action.name(), artifactId);
    }
    
    public enum AccessAction {
        READ, DOWNLOAD, DELETE, SHARE
    }
}
```

#### Presigned URL에 권한 정보 포함

```java
public String generatePresignedUrl(String s3Key, String tenantId, Long userId, AccessAction action) {
    // S3 객체 태그에 권한 정보 저장
    PutObjectTaggingRequest taggingRequest = PutObjectTaggingRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .tagging(t -> t.tagSet(
                    Tag.builder().key("tenantId").value(tenantId).build(),
                    Tag.builder().key("userId").value(String.valueOf(userId)).build(),
                    Tag.builder().key("allowedAction").value(action.name()).build()
            ))
            .build();
    
    s3Client.putObjectTagging(taggingRequest);
    
    // Presigned URL 생성
    return generatePresignedUrl(s3Key, Duration.ofHours(1));
}
```

### 6.3 캐싱 전략

#### 다층 캐싱

```java
@Service
@RequiredArgsConstructor
public class MultiTierArtifactCache {
    
    // L1: 로컬 캐시 (Caffeine)
    private final Cache<String, ArtifactDto> localCache;
    
    // L2: 분산 캐시 (Redis)
    private final RedisTemplate<String, ArtifactDto> redisCache;
    
    // L3: CDN (CloudFront)
    private final CloudFrontUrlGenerator cdnGenerator;
    
    public ArtifactDto getArtifact(Long artifactId, String tenantId, Long userId) {
        String cacheKey = buildCacheKey(artifactId, tenantId, userId);
        
        // L1 캐시 확인
        ArtifactDto cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // L2 캐시 확인
        cached = redisCache.opsForValue().get(cacheKey);
        if (cached != null) {
            localCache.put(cacheKey, cached);
            return cached;
        }
        
        // DB 조회
        ArtifactDto artifact = loadFromDatabase(artifactId, tenantId, userId);
        
        // 캐시 저장
        localCache.put(cacheKey, artifact);
        redisCache.opsForValue().set(cacheKey, artifact, Duration.ofHours(1));
        
        return artifact;
    }
}
```

### 6.4 비용 최적화

#### 스토리지 클래스 전략

```java
@Component
public class IntelligentStorageStrategy {
    
    public void optimizeStorage(Long artifactId) {
        ProductionArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow();
        
        Instant createdAt = artifact.getCreatedAt();
        Instant now = Instant.now();
        long daysSinceCreation = ChronoUnit.DAYS.between(createdAt, now);
        
        // 30일 이상 된 파일은 Glacier로 이동
        if (daysSinceCreation > 30) {
            transitionToGlacier(artifact);
        }
        
        // 90일 이상 된 파일은 Deep Archive로 이동
        if (daysSinceCreation > 90) {
            transitionToDeepArchive(artifact);
        }
    }
    
    private void transitionToGlacier(ProductionArtifactEntity artifact) {
        String s3Key = extractS3Key(artifact.getDetail().getFilePath());
        
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(s3Key)
                .destinationBucket(bucket)
                .destinationKey(s3Key)
                .storageClass(StorageClass.GLACIER)
                .build();
        
        s3Client.copyObject(request);
    }
}
```

#### 자동 정리 정책

```java
@Scheduled(cron = "0 0 2 * * ?")  // 매일 새벽 2시
public void cleanupExpiredArtifacts() {
    Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
    
    List<ProductionArtifactEntity> expired = artifactRepository
            .findByCreatedAtBeforeAndAutoDeleteEnabled(cutoff, true);
    
    for (ProductionArtifactEntity artifact : expired) {
        deleteArtifact(artifact);
    }
}
```

---

## 7. 초대량 트래픽 환경 개선

### 7.1 Presigned URL 생성 비용 최적화

#### 배치 생성

```java
@Service
public class BatchPresignedUrlService {
    
    @Cacheable(value = "presignedUrls", key = "#s3Keys + '_' + #ttl")
    public Map<String, String> generateBatchPresignedUrls(
            List<String> s3Keys,
            Duration ttl
    ) {
        // 한 번에 여러 URL 생성
        return s3Keys.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> generatePresignedUrl(key, ttl)
                ));
    }
}
```

#### 비동기 생성

```java
@Service
public class AsyncPresignedUrlService {
    
    @Async
    public CompletableFuture<String> generatePresignedUrlAsync(String s3Key, Duration ttl) {
        return CompletableFuture.completedFuture(
                generatePresignedUrl(s3Key, ttl)
        );
    }
    
    // 프론트엔드에서 즉시 반환, URL은 비동기로 생성 후 WebSocket으로 전달
    public void generateAndNotify(String artifactId, String s3Key, Duration ttl) {
        generatePresignedUrlAsync(s3Key, ttl)
                .thenAccept(url -> websocketService.notify(artifactId, url));
    }
}
```

### 7.2 CDN + Cache 전략

#### CloudFront + Lambda@Edge

```javascript
// Lambda@Edge 함수 (Node.js)
exports.handler = async (event) => {
    const request = event.Records[0].cf.request;
    const uri = request.uri;
    
    // 캐시 키 생성 (테넌트별)
    const tenantId = extractTenantId(uri);
    const cacheKey = `${tenantId}:${uri}`;
    
    // CloudFront 캐시 확인
    // 캐시 미스 시 S3에서 가져오기
    
    return request;
};
```

#### Redis 캐시 계층

```java
@Configuration
public class CacheConfiguration {
    
    @Bean
    public CacheManager artifactCacheManager() {
        // L1: Caffeine (로컬)
        CaffeineCache localCache = new CaffeineCache("local",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());
        
        // L2: Redis (분산)
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())))
                .build();
        
        return new CompositeCacheManager(localCache, redisCacheManager);
    }
}
```

### 7.3 다운로드 API 병목 해결

#### 스트리밍 응답

```java
@GetMapping("/production/{productionId}/artifact/download")
public ResponseEntity<StreamingResponseBody> downloadArtifact(
        @PathVariable Long productionId,
        @CurrentUser AuthUser authUser
) {
    ProductionArtifactEntity artifact = artifactRepository.findById(productionId)
            .orElseThrow();
    
    validateAccess(artifact, authUser);
    
    StorageStrategy strategy = storageStrategyResolver.resolve(artifact.getDetail().getFilePath());
    InputStream inputStream = strategy.read(extractStorageKey(artifact.getDetail().getFilePath()));
    
    StreamingResponseBody stream = outputStream -> {
        try (inputStream) {
            inputStream.transferTo(outputStream);
        }
    };
    
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + artifact.getDetail().getFileName() + "\"")
            .contentType(MediaType.parseMediaType(artifact.getDetail().getContentType()))
            .body(stream);
}
```

#### 직접 S3 리다이렉트

```java
@GetMapping("/production/{productionId}/artifact/download")
public ResponseEntity<Void> downloadArtifactDirect(
        @PathVariable Long productionId,
        @CurrentUser AuthUser authUser
) {
    ProductionArtifactEntity artifact = artifactRepository.findById(productionId)
            .orElseThrow();
    
    validateAccess(artifact, authUser);
    
    // Presigned URL 생성 후 리다이렉트
    String s3Key = extractS3Key(artifact.getDetail().getFilePath());
    String presignedUrl = generatePresignedUrl(s3Key, Duration.ofHours(1), authUser.getId());
    
    return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build();
}
```

**장점:**
- 서버 부하 감소 (S3 직접 다운로드)
- 대역폭 비용 절감
- 확장성 향상

---

## 8. 리팩토링 우선순위 제안

### 8.1 반드시 수정해야 할 것 (P0)

#### 1. ArtifactDto 타입 안전성 개선

**우선순위**: 🔴 최우선  
**예상 작업량**: 2-3일  
**영향도**: 높음

**작업 내용:**
- `ArtifactDto`를 추상 클래스로 변경
- `TextArtifactDto`, `FileArtifactDto`, `ImageArtifactDto` 구현
- Jackson 다형성 직렬화 설정

**예상 효과:**
- 클라이언트 런타임 에러 90% 감소
- API 계약 명확화

#### 2. Presigned URL 기본 구현

**우선순위**: 🔴 최우선  
**예상 작업량**: 1-2일  
**영향도**: 높음

**작업 내용:**
- `ArtifactAccessService` 기본 구현
- `ImageArtifactDto`에 `previewUrl` 자동 포함
- 짧은 TTL (5분) 기본 적용

**예상 효과:**
- 이미지 미리보기 기능 즉시 사용 가능
- 프론트엔드 개발 속도 향상

#### 3. `success` 필드 중복 제거

**우선순위**: 🟡 높음  
**예상 작업량**: 0.5일  
**영향도**: 중간

**작업 내용:**
- `ProductionResponseDto.success` → `ProductionStatus status`로 변경
- 프론트엔드 코드 업데이트

### 8.2 중기 개선 (P1)

#### 4. StorageStrategy 인터페이스 확장

**우선순위**: 🟡 높음  
**예상 작업량**: 3-4일  
**영향도**: 높음

**작업 내용:**
- `read()`, `generateAccessUrl()` 메서드 추가
- `S3StorageStrategy`, `LocalStorageStrategy` 구현 확장

#### 5. ArtifactHandler 전략 패턴 도입

**우선순위**: 🟡 높음  
**예상 작업량**: 2-3일  
**영향도**: 중간

**작업 내용:**
- `ArtifactHandler` 인터페이스 정의
- 타입별 Handler 구현
- `ArtifactHandlerRegistry` 도입

#### 6. PDF 변환 개선

**우선순위**: 🟢 중간  
**예상 작업량**: 3-5일  
**영향도**: 중간

**작업 내용:**
- Markdown → HTML 파서 통합
- HTML → PDF 변환기 교체 (OpenHTMLToPDF)
- CSS 스타일링 추가

### 8.3 장기 고도화 (P2)

#### 7. 썸네일 생성 시스템

**우선순위**: 🟢 중간  
**예상 작업량**: 5-7일  
**영향도**: 중간

**작업 내용:**
- 비동기 썸네일 생성 서비스
- 이미지 처리 라이브러리 통합
- 메타데이터 확장

#### 8. CDN 통합

**우선순위**: 🟢 중간  
**예상 작업량**: 3-4일  
**영향도**: 높음 (트래픽 증가 시)

**작업 내용:**
- CloudFront 배포
- CDN URL 생성 로직
- 캐싱 정책 설정

#### 9. 멀티 테넌시 지원

**우선순위**: 🔵 낮음 (SaaS 전환 시)  
**예상 작업량**: 7-10일  
**영향도**: 높음

**작업 내용:**
- 테넌트 ID 필드 추가
- S3 키 구조 변경
- 접근 제어 강화

#### 10. 스토리지 자동 최적화

**우선순위**: 🔵 낮음  
**예상 작업량**: 3-4일  
**영향도**: 낮음 (비용 절감)

**작업 내용:**
- Glacier 전환 스케줄러
- 자동 정리 정책
- 비용 모니터링

### 8.4 리팩토링 로드맵

```
Phase 1 (1-2주): P0 항목
├── ArtifactDto 타입 안전성 개선
├── Presigned URL 기본 구현
└── success 필드 중복 제거

Phase 2 (3-4주): P1 항목
├── StorageStrategy 확장
├── ArtifactHandler 전략 패턴
└── PDF 변환 개선

Phase 3 (5-8주): P2 항목 (필요 시)
├── 썸네일 생성
├── CDN 통합
└── 멀티 테넌시
```

---

## 9. 구현 예시 코드

### 9.1 개선된 ArtifactDto 구조

```java
// Base
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextArtifactDto.class, name = "TEXT"),
    @JsonSubTypes.Type(value = FileArtifactDto.class, name = "FILE"),
    @JsonSubTypes.Type(value = ImageArtifactDto.class, name = "IMAGE")
})
public abstract class ArtifactDto {
    protected ArtifactType type;
    protected String fileName;
    protected String contentType;
    protected Instant createdAt;
}

// Text
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TextArtifactDto extends ArtifactDto {
    private String content;
    private boolean isInline;
}

// Image
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImageArtifactDto extends ArtifactDto {
    private String filePath;
    private String previewUrl;
    private Instant urlExpiresAt;
    private ImageMetadata metadata;
}

// File
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileArtifactDto extends ArtifactDto {
    private String filePath;
    private String downloadUrl;
    private Long fileSize;
}
```

### 9.2 ArtifactAccessService 구현

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactAccessServiceImpl implements ArtifactAccessService {
    
    private final ProductionArtifactRepository artifactRepository;
    private final StorageStrategyResolver storageStrategyResolver;
    private final S3Presigner s3Presigner;
    private final RedisTemplate<String, String> redisTemplate;
    private final ArtifactHandlerRegistry handlerRegistry;
    
    @Value("${artifact.url.default-ttl:5m}")
    private Duration defaultTtl;
    
    @Value("${artifact.s3.bucket}")
    private String bucket;
    
    @Override
    public ArtifactDto enrichArtifact(ProductionArtifactDetailEntity detail, Long userId) {
        validateAccess(detail.getArtifact().getId(), userId);
        
        ArtifactHandler handler = handlerRegistry.getHandler(detail.getArtifactType());
        return handler.enrich(detail, this);
    }
    
    @Override
    public String generatePresignedUrl(String storageKey, Duration expiration, Long userId) {
        String cacheKey = "presigned:" + storageKey + ":" + expiration.toMinutes();
        
        // 캐시 확인
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Presigned URL 생성
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(r -> r
                        .bucket(bucket)
                        .key(storageKey))
                .build();
        
        String url = s3Presigner.presignGetObject(request).url().toString();
        
        // 캐시 저장 (TTL보다 1분 짧게)
        redisTemplate.opsForValue().set(
                cacheKey, 
                url, 
                expiration.minusMinutes(1)
        );
        
        return url;
    }
    
    @Override
    public InputStream getArtifactStream(String storageKey, Long userId) {
        StorageStrategy strategy = storageStrategyResolver.resolve(storageKey);
        return strategy.read(storageKey);
    }
    
    @Override
    public void validateAccess(Long artifactId, Long userId) {
        ProductionArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ArtifactNotFoundException(artifactId));
        
        if (!artifact.getUserId().equals(userId)) {
            throw new ArtifactAccessDeniedException(artifactId, userId);
        }
    }
}
```

### 9.3 Controller 개선

```java
@RestController
@RequestMapping("/production")
@RequiredArgsConstructor
@Slf4j
public class ProductionResultController {
    
    private final ProductionArtifactRepository artifactRepository;
    private final ArtifactAccessService artifactAccessService;
    
    @GetMapping("/{productionId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        ProductionArtifactEntity artifact = artifactRepository
                .findByIdWithDetail(productionId)
                .orElseThrow(() -> new ArtifactNotFoundException(productionId));
        
        // Artifact 접근 정보 풍부하게 만들기
        ArtifactDto enrichedArtifact = artifactAccessService.enrichArtifact(
                artifact.getDetail(),
                authUser.getId()
        );
        
        ProductionResponseDto response = ProductionResponseDto.builder()
                .productionId(artifact.getId())
                .status(artifact.isSuccess() ? ProductionStatus.SUCCESS : ProductionStatus.FAILED)
                .errorMessage(artifact.getDetail() != null 
                        ? artifact.getDetail().getErrorMessage() 
                        : null)
                .startedAt(artifact.getStartedAt())
                .completedAt(artifact.getCompletedAt())
                .artifact(enrichedArtifact)
                .build();
        
        return CustomResponseHelper.ok(response);
    }
    
    @GetMapping("/{productionId}/artifact/download")
    public ResponseEntity<StreamingResponseBody> downloadArtifact(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        ProductionArtifactEntity artifact = artifactRepository
                .findByIdWithDetail(productionId)
                .orElseThrow(() -> new ArtifactNotFoundException(productionId));
        
        artifactAccessService.validateAccess(productionId, authUser.getId());
        
        String storageKey = extractStorageKey(artifact.getDetail().getFilePath());
        InputStream inputStream = artifactAccessService.getArtifactStream(storageKey, authUser.getId());
        
        StreamingResponseBody stream = outputStream -> {
            try (inputStream) {
                inputStream.transferTo(outputStream);
            }
        };
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + artifact.getDetail().getFileName() + "\"")
                .contentType(MediaType.parseMediaType(artifact.getDetail().getContentType()))
                .body(stream);
    }
}
```

---

## 10. 결론 및 권장사항

### 핵심 개선 사항 요약

1. **타입 안전성**: ArtifactDto를 타입별로 분리하여 명확한 API 계약 제공
2. **Presigned URL**: 자동 포함 방식으로 개발자 경험 향상
3. **확장성**: 전략 패턴 도입으로 새로운 타입/저장소 추가 용이
4. **성능**: 캐싱 전략으로 대량 트래픽 대응
5. **SaaS 준비**: 멀티 테넌시, 접근 제어, 비용 최적화

### 즉시 적용 권장 (P0)

1. ArtifactDto 타입 분리
2. Presigned URL 기본 구현
3. success 필드 중복 제거

### 중기 개선 (P1)

1. StorageStrategy 확장
2. ArtifactHandler 전략 패턴
3. PDF 변환 개선

### 장기 고도화 (P2)

1. 썸네일 시스템
2. CDN 통합
3. 멀티 테넌시

이 설계는 **실제 운영 환경에서 검증 가능한 수준**으로 작성되었으며, 단계적 적용을 통해 리스크를 최소화하면서 시스템을 고도화할 수 있습니다.

