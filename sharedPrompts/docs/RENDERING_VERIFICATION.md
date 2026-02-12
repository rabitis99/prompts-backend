# AI 결과물 렌더링 방식 확인

## 목적
AI 결과물(텍스트, 이미지 등)의 렌더링 방식을 검증하고 확인합니다.

---

## 1. 텍스트 렌더링

### 1.1 렌더링 방식

텍스트 결과물은 두 가지 방식으로 저장 및 반환됩니다:

#### **INLINE_TEXT 방식**
- **용도**: 작은 텍스트 콘텐츠 (마크다운, HTML 등)
- **저장 위치**: 데이터베이스에 직접 저장
- **반환 형식**: JSON 응답의 `location` 필드에 실제 콘텐츠 문자열이 포함됨

```java
// ArtifactDto.java
String location = detail.getStorageType() == StorageFormat.INLINE_TEXT
        ? detail.getContent()  // 실제 콘텐츠 문자열
        : detail.getFilePath(); // 파일 경로
```

#### **FILE_PATH 방식**
- **용도**: 큰 텍스트 콘텐츠 또는 파일로 저장된 텍스트
- **저장 위치**: S3 또는 로컬 파일 시스템
- **반환 형식**: JSON 응답의 `location` 필드에 파일 경로가 포함됨

### 1.2 지원 형식

| 형식 | 라이브러리 | 구현 클래스 | 비고 |
|------|-----------|------------|------|
| **Markdown** | - | 인라인 텍스트로 저장 | 프론트엔드에서 마크다운 파서로 렌더링 |
| **HTML** | OWASP Java HTML Sanitizer | `HtmlSanitizer` | XSS 방지를 위한 HTML 정제 |
| **PDF** | iText (OpenPDF) | `PdfFormatConverter` | 텍스트를 PDF 바이너리로 변환 |

### 1.3 PDF 생성 구현

```java
// PdfFormatConverter.java
@Component
public class PdfFormatConverter implements FormatConverter {
    
    @Override
    public byte[] convert(String content, String fileName) {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        
        // 텍스트를 단락별로 분리하여 PDF에 추가
        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            document.add(new Paragraph(para.trim(), font));
        }
        
        return out.toByteArray();
    }
    
    @Override
    public String getContentType() {
        return "application/pdf";
    }
}
```

**특징:**
- A4 페이지 크기 사용
- 텍스트를 단락(`\n\n`) 기준으로 분리하여 렌더링
- Helvetica 폰트, 11pt 크기 사용

### 1.4 API 응답 구조

```json
{
  "success": true,
  "data": {
    "productionId": 123,
    "success": true,
    "artifact": {
      "type": "TEXT",
      "location": "실제 텍스트 콘텐츠 또는 파일 경로",
      "fileName": "output.txt",
      "contentType": "text/plain",
      "storageLocation": "S3 또는 LOCAL"
    }
  }
}
```

---

## 2. 이미지 렌더링

### 2.1 S3 연동

이미지는 **AWS S3**에 저장되며, `S3StorageStrategy`를 통해 관리됩니다.

#### **저장 프로세스**

```java
// S3StorageStrategy.java
@Override
public String store(byte[] data, String contentType, Long userId, String jobId, String fileName) {
    // S3 키 생성: production/{userId}/{jobId}/{fileName}
    String s3Key = buildS3Key(userId, jobId, fileName);
    
    // S3에 업로드
    PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .contentType(contentType)
            .contentLength((long) data.length)
            .build();
    
    s3Client.putObject(request, RequestBody.fromBytes(data));
    
    // S3 키 반환 (s3:// 형식)
    return s3Key;
}
```

#### **S3 키 구조**
```
production/{userId}/{jobId}/{fileName}
```

예시: `production/123/job-abc-123/image.png`

### 2.2 URL 반환 방식

#### **현재 구현**
- **반환 형식**: `s3://bucket-name/s3-key` 형식의 문자열
- **예시**: `s3://ai-generated-images/generated-images/uuid.png`

```java
// ImageAIService.java
String imagePath = imageAIClient.generateImage(...);
// imagePath = "s3://ai-generated-images/generated-images/uuid.png"

return AIContentResult.success(
    ContentType.IMAGE,
    imagePath, // S3 경로 반환
    modelName
);
```

#### **API 응답 구조**

```json
{
  "success": true,
  "data": {
    "productionId": 456,
    "success": true,
    "artifact": {
      "type": "IMAGE",
      "location": "s3://bucket-name/path/to/image.png",
      "fileName": "image.png",
      "contentType": "image/png",
      "storageLocation": "S3"
    }
  }
}
```

### 2.3 프론트엔드 연계

#### **필요한 작업**
1. **Presigned URL 생성**: S3 객체에 대한 임시 접근 URL 생성 필요
   - 현재는 `s3://` 형식만 반환하므로, 프론트엔드에서 직접 접근 불가
   - 백엔드에서 Presigned URL을 생성하여 반환하거나
   - 별도 API 엔드포인트 제공 필요

2. **이미지 다운로드 API**: 
   ```
   GET /api/production/{productionId}/artifact/download
   ```
   - S3에서 이미지를 읽어서 스트림으로 반환
   - 또는 Presigned URL을 생성하여 리다이렉트

#### **권장 개선사항**

```java
// ProductionResultController에 추가
@GetMapping("/production/{productionId}/artifact/url")
public ResponseEntity<CustomResponse<String>> getArtifactUrl(
        @PathVariable Long productionId,
        @RequestParam(defaultValue = "1h") Duration expiration
) {
    // S3 Presigned URL 생성
    String presignedUrl = s3Service.generatePresignedUrl(artifact.getLocation(), expiration);
    return CustomResponseHelper.ok(presignedUrl);
}
```

---

## 3. 프론트엔드 Preview 방식

### 3.1 API 엔드포인트

#### **Production 결과 조회**
```
GET /api/production/{productionId}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "productionId": 123,
    "success": true,
    "errorMessage": null,
    "startedAt": "2024-01-01T10:00:00Z",
    "completedAt": "2024-01-01T10:05:00Z",
    "artifact": {
      "type": "TEXT",
      "location": "# 제목\n\n내용...",
      "fileName": "output.md",
      "contentType": "text/markdown",
      "storageLocation": "INLINE_TEXT"
    }
  }
}
```

#### **Job 상태 조회**
```
GET /api/jobs/{jobId}
```

비동기 작업의 진행 상태를 확인할 수 있습니다.

### 3.2 프론트엔드 렌더링 전략

#### **텍스트 (TEXT 타입)**

```typescript
// 프론트엔드 예시
interface ArtifactDto {
  type: 'TEXT' | 'FILE' | 'IMAGE';
  location: string;  // 콘텐츠 또는 파일 경로
  fileName: string;
  contentType: string;
  storageLocation: string;
}

function renderArtifact(artifact: ArtifactDto) {
  if (artifact.type === 'TEXT') {
    if (artifact.storageLocation === 'INLINE_TEXT') {
      // 인라인 텍스트 직접 렌더링
      if (artifact.contentType === 'text/markdown') {
        return <MarkdownRenderer content={artifact.location} />;
      } else if (artifact.contentType === 'text/html') {
        return <div dangerouslySetInnerHTML={{ __html: artifact.location }} />;
      } else {
        return <pre>{artifact.location}</pre>;
      }
    } else {
      // 파일 경로인 경우 다운로드 API 호출 필요
      return <FileDownloader path={artifact.location} />;
    }
  }
}
```

#### **이미지 (IMAGE 타입)**

```typescript
function renderImage(artifact: ArtifactDto) {
  if (artifact.type === 'IMAGE') {
    // S3 경로인 경우 Presigned URL 생성 API 호출
    const imageUrl = await fetchPresignedUrl(artifact.location);
    return <img src={imageUrl} alt={artifact.fileName} />;
  }
}
```

#### **파일 (FILE 타입)**

```typescript
function renderFile(artifact: ArtifactDto) {
  if (artifact.type === 'FILE') {
    // PDF, Excel 등은 다운로드 링크 제공
    const downloadUrl = `/api/production/${productionId}/artifact/download`;
    return (
      <a href={downloadUrl} download={artifact.fileName}>
        {artifact.fileName} 다운로드
      </a>
    );
  }
}
```

### 3.3 Spring MVC 설정

#### **CORS 설정**
프론트엔드와의 연동을 위해 CORS가 설정되어 있습니다:

```yaml
# application.yml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

#### **응답 래핑**
모든 API 응답은 `CustomResponse`로 래핑됩니다:

```java
// CustomResponseHelper.java
public static <T> ResponseEntity<CustomResponse<T>> ok(T data) {
    return ResponseEntity.ok(CustomResponse.success(data));
}
```

---

## 4. 사용된 도구 및 라이브러리

### 4.1 Spring MVC
- **용도**: REST API 엔드포인트 제공
- **구현**: `@RestController`, `ResponseEntity`
- **파일**: `ProductionResultController.java`

### 4.2 PDF 라이브러리
- **라이브러리**: iText (OpenPDF)
- **용도**: 텍스트를 PDF 바이너리로 변환
- **구현**: `PdfFormatConverter.java`
- **의존성**: `com.lowagie.text.*`

### 4.3 S3 클라이언트
- **라이브러리**: AWS SDK for Java v2 (`software.amazon.awssdk.services.s3`)
- **용도**: 이미지 및 파일 저장
- **구현**: `S3StorageStrategy.java`
- **설정**: `production.storage.type=S3`, `production.storage.s3.bucket`

### 4.4 HTML Sanitizer
- **라이브러리**: OWASP Java HTML Sanitizer
- **용도**: XSS 방지를 위한 HTML 정제
- **구현**: `HtmlSanitizer.java`

---

## 5. 확인 사항 체크리스트

### ✅ 텍스트 렌더링
- [x] Markdown 형식 지원 (인라인 텍스트)
- [x] HTML 형식 지원 (Sanitizer 적용)
- [x] PDF 변환 기능 (iText 사용)
- [x] INLINE_TEXT vs FILE_PATH 구분

### ✅ 이미지 렌더링
- [x] S3 연동 구현
- [x] S3 키 구조 확인 (`production/{userId}/{jobId}/{fileName}`)
- [ ] Presigned URL 생성 기능 (개선 필요)
- [ ] 이미지 다운로드 API (개선 필요)

### ✅ 프론트엔드 연계
- [x] REST API 엔드포인트 제공
- [x] JSON 응답 구조 정의
- [x] CORS 설정
- [x] Artifact 타입별 렌더링 전략 문서화

### ⚠️ 개선 필요 사항
1. **Presigned URL 생성**: S3 이미지 접근을 위한 임시 URL 생성 기능 추가
2. **파일 다운로드 API**: FILE 타입 아티팩트 다운로드 엔드포인트 추가
3. **이미지 미리보기**: 썸네일 생성 및 반환 기능 고려

---

## 6. 참고 파일

### 핵심 구현 파일
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ArtifactDto.java`
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ProductionResponseDto.java`
- `src/main/java/org/example/sharedprompts/module/controller/production/ProductionResultController.java`
- `src/main/java/org/example/sharedprompts/module/domain/production/service/storage/S3StorageStrategy.java`
- `src/main/java/org/example/sharedprompts/module/domain/production/service/format/PdfFormatConverter.java`
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/image/ImageAIService.java`

### 설정 파일
- `src/main/resources/application.yml`
- `build.gradle` (의존성 확인)

### 아키텍처 문서
- `docs/EXECUTION_MODULE_ARCHITECTURE.md`
- `docs/AI_INFRASTRUCTURE.md`
- `docs/PRODUCTION_ARCHITECTURE.md`

