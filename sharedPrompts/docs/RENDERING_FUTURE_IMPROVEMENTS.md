# 렌더링 시스템 중기 개선 가이드 (P1-P2)

> **우선순위**: 🟢 중간  
> **예상 작업량**: 8-12일  
> **목적**: 품질 향상 및 확장 기능 추가

---

## 목차

1. [PDF 품질 개선](#1-pdf-품질-개선)
2. [썸네일 전략](#2-썸네일-전략)

---

## 1. PDF 품질 개선

### 1.1 현재 구현의 문제점

```java
// 현재: 단순 텍스트 → PDF
String[] paragraphs = content.split("\n\n");
for (String para : paragraphs) {
    document.add(new Paragraph(para.trim(), font));
}
```

**문제:**
- Markdown 구조 무시 (제목, 리스트, 코드 블록 등)
- 스타일링 부재
- 이미지 포함 불가
- 테이블 미지원

### 1.2 개선 전략: Markdown → HTML → PDF

#### 아키텍처

```
Markdown 텍스트
    ↓
CommonMark 파서
    ↓
HTML (구조화된)
    ↓
CSS 스타일링
    ↓
OpenHTMLToPDF
    ↓
PDF 바이너리
```

### 1.3 의존성 추가

```gradle
// build.gradle
dependencies {
    // Markdown 파싱
    implementation 'com.vladsch.flexmark:flexmark-all:0.64.8'
    
    // HTML → PDF 변환
    implementation 'com.openhtmltopdf:openhtmltopdf-pdfbox:1.1.24'
    implementation 'com.openhtmltopdf:openhtmltopdf-svg-support:1.1.24'
    implementation 'com.openhtmltopdf:openhtmltopdf-slf4j:1.1.24'
    
    // 한글 폰트 지원 (선택)
    implementation 'com.openhtmltopdf:openhtmltopdf-rtl-support:1.1.24'
}
```

### 1.4 MarkdownParser 인터페이스

```java
package org.example.sharedprompts.module.domain.production.service.format;

public interface MarkdownParser {
    /**
     * Markdown 텍스트를 HTML로 변환
     */
    String parse(String markdown);
}
```

### 1.5 FlexMark 구현

```java
package org.example.sharedprompts.module.domain.production.service.format;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FlexMarkMarkdownParser implements MarkdownParser {
    
    private final Parser parser;
    private final HtmlRenderer renderer;
    
    public FlexMarkMarkdownParser() {
        // Pegdown 호환 옵션 (GitHub Flavored Markdown 지원)
        DataHolder options = PegdownOptionsAdapter.flexmarkOptions(
                Extensions.ALL & ~Extensions.ANCHORLINKS
        );
        
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }
    
    @Override
    public String parse(String markdown) {
        try {
            var document = parser.parse(markdown);
            return renderer.render(document);
        } catch (Exception e) {
            log.error("Markdown parsing failed", e);
            throw new FormatConversionException("Markdown parsing failed: " + e.getMessage(), e);
        }
    }
}
```

### 1.6 HtmlToPdfConverter 인터페이스

```java
package org.example.sharedprompts.module.domain.production.service.format;

public interface HtmlToPdfConverter {
    /**
     * HTML과 CSS를 PDF로 변환
     */
    byte[] convert(String html, String css);
}
```

### 1.7 OpenHTMLToPDF 구현

```java
package org.example.sharedprompts.module.domain.production.service.format;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class OpenHtmlToPdfConverter implements HtmlToPdfConverter {
    
    @Override
    public byte[] convert(String html, String css) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            
            // HTML 콘텐츠 설정
            builder.withHtmlContent(html, null);
            
            // 페이지 크기 설정 (A4)
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            
            // 출력 스트림 설정
            builder.toStream(out);
            
            // PDF 생성
            builder.run();
            
            return out.toByteArray();
            
        } catch (IOException e) {
            log.error("PDF conversion failed", e);
            throw new FormatConversionException("PDF conversion failed: " + e.getMessage(), e);
        }
    }
}
```

### 1.8 CssProvider 인터페이스

```java
package org.example.sharedprompts.module.domain.production.service.format;

public interface CssProvider {
    /**
     * 기본 CSS 스타일 반환
     */
    String getDefaultCss();
    
    /**
     * 커스텀 CSS 스타일 반환
     */
    String getCustomCss(String styleName);
}
```

### 1.9 ReportCssProvider 구현

```java
package org.example.sharedprompts.module.domain.production.service.format;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReportCssProvider implements CssProvider {
    
    @Override
    public String getDefaultCss() {
        return """
            @page {
                margin: 2cm;
                @bottom-center {
                    content: "Page " counter(page) " of " counter(pages);
                    font-size: 9pt;
                    color: #666;
                }
            }
            
            body {
                font-family: 'Noto Sans KR', 'Malgun Gothic', sans-serif;
                font-size: 11pt;
                line-height: 1.6;
                color: #333;
            }
            
            h1 {
                font-size: 24pt;
                font-weight: bold;
                margin-top: 20pt;
                margin-bottom: 12pt;
                border-bottom: 2pt solid #333;
                padding-bottom: 6pt;
                page-break-after: avoid;
            }
            
            h2 {
                font-size: 18pt;
                font-weight: bold;
                margin-top: 16pt;
                margin-bottom: 10pt;
                page-break-after: avoid;
            }
            
            h3 {
                font-size: 14pt;
                font-weight: bold;
                margin-top: 12pt;
                margin-bottom: 8pt;
                page-break-after: avoid;
            }
            
            p {
                margin: 8pt 0;
                text-align: justify;
            }
            
            code {
                background-color: #f5f5f5;
                padding: 2pt 4pt;
                border-radius: 3pt;
                font-family: 'Courier New', 'Consolas', monospace;
                font-size: 10pt;
            }
            
            pre {
                background-color: #f5f5f5;
                padding: 10pt;
                border-left: 4pt solid #007acc;
                overflow-x: auto;
                page-break-inside: avoid;
            }
            
            pre code {
                background-color: transparent;
                padding: 0;
            }
            
            blockquote {
                border-left: 4pt solid #ddd;
                padding-left: 12pt;
                margin: 12pt 0;
                color: #666;
                font-style: italic;
            }
            
            ul, ol {
                margin: 8pt 0;
                padding-left: 24pt;
            }
            
            li {
                margin: 4pt 0;
            }
            
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 12pt 0;
                page-break-inside: avoid;
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
            
            tr:nth-child(even) {
                background-color: #f9f9f9;
            }
            
            img {
                max-width: 100%;
                height: auto;
                page-break-inside: avoid;
            }
            
            a {
                color: #007acc;
                text-decoration: none;
            }
            
            hr {
                border: none;
                border-top: 1pt solid #ddd;
                margin: 16pt 0;
            }
            """;
    }
    
    @Override
    public String getCustomCss(String styleName) {
        // 나중에 커스텀 스타일 추가 가능
        return getDefaultCss();
    }
}
```

### 1.10 EnhancedPdfFormatConverter 구현

```java
package org.example.sharedprompts.module.domain.production.service.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnhancedPdfFormatConverter implements FormatConverter {
    
    private final MarkdownParser markdownParser;
    private final HtmlToPdfConverter htmlToPdfConverter;
    private final CssProvider cssProvider;
    private final ImageResolver imageResolver;
    
    @Override
    public byte[] convert(String content, String fileName) {
        try {
            log.info("Starting PDF conversion - fileName: {}", fileName);
            
            // 1. Markdown → HTML
            String html = markdownParser.parse(content);
            log.debug("Markdown parsed to HTML - length: {}", html.length());
            
            // 2. 이미지 URL 해결
            html = resolveImagesInHtml(html);
            
            // 3. CSS 적용
            String css = cssProvider.getDefaultCss();
            
            // 4. HTML + CSS → PDF
            byte[] pdfBytes = htmlToPdfConverter.convert(html, css);
            log.info("PDF conversion completed - size: {} bytes", pdfBytes.length);
            
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("PDF conversion failed - fileName: {}", fileName, e);
            throw new FormatConversionException("PDF conversion failed: " + e.getMessage(), e);
        }
    }
    
    private String resolveImagesInHtml(String html) {
        // Markdown 이미지: ![alt](path)
        // HTML 이미지: <img src="path">
        // S3 경로인 경우 Presigned URL로 변환
        
        // 정규식으로 이미지 태그 찾기
        return html.replaceAll(
            "<img\\s+src=\"([^\"]+)\"",
            matcher -> {
                String imagePath = matcher.group(1);
                String resolvedUrl = imageResolver.resolveImageUrl(imagePath);
                return "<img src=\"" + resolvedUrl + "\"";
            }
        );
    }
    
    @Override
    public String getContentType() {
        return "application/pdf";
    }
    
    @Override
    public String getFileExtension() {
        return ".pdf";
    }
    
    @Override
    public boolean supports(String format) {
        return "pdf".equalsIgnoreCase(format);
    }
}
```

### 1.11 ImageResolver 구현

```java
package org.example.sharedprompts.module.domain.production.service.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageResolver {
    
    private final ArtifactAccessService artifactAccessService;
    
    private static final Pattern S3_PATH_PATTERN = Pattern.compile("s3://([^/]+)/(.+)");
    
    public String resolveImageUrl(String imagePath) {
        // S3 경로인 경우 Presigned URL 생성
        if (imagePath.startsWith("s3://")) {
            String s3Key = extractS3Key(imagePath);
            // 기본 1시간 TTL (PDF 생성 시간 고려)
            return artifactAccessService.generatePresignedUrl(
                    s3Key, 
                    java.time.Duration.ofHours(1),
                    null // PDF 생성 시점에는 userId가 없을 수 있음
            );
        }
        
        // 로컬 파일인 경우 base64 인코딩
        if (imagePath.startsWith("/") || imagePath.startsWith("file://")) {
            return encodeToBase64(imagePath);
        }
        
        // 이미 URL인 경우 그대로 반환
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        
        // 알 수 없는 형식
        log.warn("Unknown image path format: {}", imagePath);
        return imagePath;
    }
    
    private String extractS3Key(String s3Path) {
        var matcher = S3_PATH_PATTERN.matcher(s3Path);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return s3Path;
    }
    
    private String encodeToBase64(String filePath) {
        try {
            String cleanPath = filePath.replace("file://", "");
            byte[] imageBytes = Files.readAllBytes(Paths.get(cleanPath));
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = Files.probeContentType(Paths.get(cleanPath));
            if (mimeType == null) {
                mimeType = "image/png"; // 기본값
            }
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.error("Failed to encode image to base64: {}", filePath, e);
            return filePath; // 실패 시 원본 반환
        }
    }
}
```

### 1.12 FormatConverterRegistry 수정

```java
// 기존 PdfFormatConverter 대신 EnhancedPdfFormatConverter 사용
@Configuration
public class FormatConverterConfig {
    
    @Bean
    @Primary
    @ConditionalOnProperty(name = "pdf.converter.enhanced", havingValue = "true", matchIfMissing = true)
    public FormatConverter enhancedPdfConverter(
            MarkdownParser markdownParser,
            HtmlToPdfConverter htmlToPdfConverter,
            CssProvider cssProvider,
            ImageResolver imageResolver
    ) {
        return new EnhancedPdfFormatConverter(
                markdownParser,
                htmlToPdfConverter,
                cssProvider,
                imageResolver
        );
    }
    
    @Bean
    @ConditionalOnProperty(name = "pdf.converter.enhanced", havingValue = "false")
    public FormatConverter legacyPdfConverter() {
        return new PdfFormatConverter();
    }
}
```

### 1.13 설정

```yaml
# application.yml
pdf:
  converter:
    enhanced: true  # EnhancedPdfFormatConverter 사용
```

### 1.14 테스트

```java
@Test
void testEnhancedPdfConversion() {
    String markdown = """
        # 제목
        
        이것은 **굵은 글씨**입니다.
        
        - 리스트 항목 1
        - 리스트 항목 2
        
        ```java
        public class Test {
            // 코드 블록
        }
        ```
        
        ![이미지](s3://bucket/path/to/image.png)
        """;
    
    byte[] pdf = enhancedPdfFormatConverter.convert(markdown, "test.pdf");
    
    assertThat(pdf).isNotEmpty();
    assertThat(pdf.length).isGreaterThan(1000);
}
```

---

## 2. 썸네일 전략

### 2.1 아키텍처 개요

```
원본 이미지 업로드
    ↓
비동기 썸네일 생성 작업 큐에 추가
    ↓
썸네일 생성 (200x200, 400x400 등)
    ↓
S3에 썸네일 저장
    ↓
메타데이터 업데이트
```

### 2.2 ImageProcessor 인터페이스

```java
package org.example.sharedprompts.module.domain.production.service.image;

import java.io.IOException;

public interface ImageProcessor {
    /**
     * 이미지 리사이즈
     */
    byte[] resize(byte[] imageBytes, int width, int height) throws IOException;
    
    /**
     * 썸네일 생성 (정사각형)
     */
    byte[] generateThumbnail(byte[] imageBytes, int size) throws IOException;
    
    /**
     * 이미지 메타데이터 추출
     */
    ImageMetadata extractMetadata(byte[] imageBytes) throws IOException;
}
```

### 2.3 ImageMetadata 클래스

```java
package org.example.sharedprompts.module.domain.production.service.image;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImageMetadata {
    int width;
    int height;
    long fileSize;
    String format;  // PNG, JPEG, WEBP 등
    String mimeType;
}
```

### 2.4 JavaImageProcessor 구현

```java
package org.example.sharedprompts.module.domain.production.service.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class JavaImageProcessor implements ImageProcessor {
    
    @Override
    public byte[] resize(byte[] imageBytes, int width, int height) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IOException("Failed to read image");
        }
        
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        
        // 고품질 리샘플링
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        
        return encodeToJpeg(resized);
    }
    
    @Override
    public byte[] generateThumbnail(byte[] imageBytes, int size) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IOException("Failed to read image");
        }
        
        // 비율 유지하며 정사각형 썸네일 생성
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        int thumbWidth, thumbHeight;
        if (originalWidth > originalHeight) {
            thumbWidth = size;
            thumbHeight = (int) ((double) originalHeight / originalWidth * size);
        } else {
            thumbHeight = size;
            thumbWidth = (int) ((double) originalWidth / originalHeight * size);
        }
        
        BufferedImage thumbnail = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        
        // 흰색 배경
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        
        // 중앙 정렬
        int x = (size - thumbWidth) / 2;
        int y = (size - thumbHeight) / 2;
        
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(original, x, y, thumbWidth, thumbHeight, null);
        g.dispose();
        
        return encodeToJpeg(thumbnail);
    }
    
    @Override
    public ImageMetadata extractMetadata(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Failed to read image");
        }
        
        String format = detectFormat(imageBytes);
        String mimeType = format.equals("jpg") ? "image/jpeg" : "image/" + format;
        
        return ImageMetadata.builder()
                .width(image.getWidth())
                .height(image.getHeight())
                .fileSize((long) imageBytes.length)
                .format(format.toUpperCase())
                .mimeType(mimeType)
                .build();
    }
    
    private byte[] encodeToJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
    
    private String detectFormat(byte[] imageBytes) {
        // 간단한 형식 감지 (실제로는 더 정교한 로직 필요)
        if (imageBytes.length >= 4) {
            // PNG 시그니처
            if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == 0x50) {
                return "png";
            }
            // JPEG 시그니처
            if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8) {
                return "jpg";
            }
        }
        return "jpg"; // 기본값
    }
}
```

### 2.5 ThumbnailService 구현

```java
package org.example.sharedprompts.module.domain.production.service.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactDetailRepository;
import org.example.sharedprompts.module.domain.production.service.storage.StorageContext;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {
    
    private final ImageProcessor imageProcessor;
    private final StorageStrategy storageStrategy;
    private final ProductionArtifactDetailRepository detailRepository;
    
    // 썸네일 크기 정의
    private static final List<Integer> THUMBNAIL_SIZES = List.of(200, 400);
    
    @Async
    public void generateThumbnails(Long artifactId, String originalImagePath, Long userId, String jobId) {
        try {
            log.info("Starting thumbnail generation - artifactId: {}, path: {}", artifactId, originalImagePath);
            
            // 1. 원본 이미지 다운로드
            byte[] originalImage = storageStrategy.read(extractStorageKey(originalImagePath))
                    .readAllBytes();
            
            // 2. 이미지 메타데이터 추출
            ImageMetadata metadata = imageProcessor.extractMetadata(originalImage);
            
            // 3. 각 크기별 썸네일 생성
            Map<String, String> thumbnailKeys = new java.util.HashMap<>();
            
            for (Integer size : THUMBNAIL_SIZES) {
                byte[] thumbnail = imageProcessor.generateThumbnail(originalImage, size);
                
                // 4. 썸네일 저장
                String thumbnailKey = buildThumbnailKey(originalImagePath, size);
                storageStrategy.store(thumbnail, StorageContext.builder()
                        .userId(userId)
                        .jobId(jobId)
                        .fileName("thumbnail_" + size + "_" + extractFileName(originalImagePath))
                        .metadata(Map.of(
                                "contentType", "image/jpeg",
                                "thumbnailSize", String.valueOf(size),
                                "originalPath", originalImagePath
                        ))
                        .build());
                
                thumbnailKeys.put("thumbnail_" + size, thumbnailKey);
            }
            
            // 5. 메타데이터 업데이트
            updateArtifactMetadata(artifactId, metadata, thumbnailKeys);
            
            log.info("Thumbnail generation completed - artifactId: {}", artifactId);
            
        } catch (Exception e) {
            log.error("Thumbnail generation failed - artifactId: {}", artifactId, e);
            // 실패해도 원본은 사용 가능하므로 예외를 던지지 않음
        }
    }
    
    private String extractStorageKey(String filePath) {
        if (filePath.startsWith("s3://")) {
            return filePath.substring(5).substring(filePath.indexOf("/") + 1);
        }
        return filePath;
    }
    
    private String buildThumbnailKey(String originalPath, int size) {
        // production/{userId}/{jobId}/thumbnails/{size}/{fileName}
        String baseKey = originalPath.replace("/images/", "/thumbnails/");
        String fileName = extractFileName(baseKey);
        String dir = baseKey.substring(0, baseKey.lastIndexOf("/"));
        return dir + "/" + size + "/" + fileName;
    }
    
    private String extractFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }
    
    private void updateArtifactMetadata(Long artifactId, ImageMetadata metadata, Map<String, String> thumbnailKeys) {
        ProductionArtifactDetailEntity detail = detailRepository.findById(artifactId)
                .orElseThrow();
        
        // 메타데이터 JSON 생성
        Map<String, Object> metadataMap = new java.util.HashMap<>();
        metadataMap.put("width", metadata.getWidth());
        metadataMap.put("height", metadata.getHeight());
        metadataMap.put("fileSize", metadata.getFileSize());
        metadataMap.put("format", metadata.getFormat());
        metadataMap.put("thumbnails", thumbnailKeys);
        
        // JSON 문자열로 저장 (기존 metadata 필드 활용)
        String metadataJson = JsonUtils.toJson(metadataMap);
        detail.setMetadata(metadataJson);
        
        detailRepository.save(detail);
    }
}
```

### 2.6 ImageArtifactDto 확장

```java
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImageArtifactDto extends ArtifactDto {
    private String filePath;
    private String previewUrl;
    private Instant urlExpiresAt;
    private ImageMetadata metadata;
    
    // ✅ 썸네일 URL 추가
    private Map<String, String> thumbnailUrls;  // {"thumbnail_200": "url", "thumbnail_400": "url"}
}
```

### 2.7 ArtifactAccessService 확장

```java
@Override
public ImageArtifactDto buildImageArtifact(ProductionArtifactDetailEntity detail, Long userId) {
    String s3Key = extractS3Key(detail.getFilePath());
    String previewUrl = generatePresignedUrl(s3Key, defaultTtl, userId);
    
    // 썸네일 URL 생성
    Map<String, String> thumbnailUrls = new java.util.HashMap<>();
    if (detail.getMetadata() != null) {
        Map<String, Object> metadata = JsonUtils.fromJson(detail.getMetadata(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> thumbnailKeys = (Map<String, String>) metadata.get("thumbnails");
        
        if (thumbnailKeys != null) {
            thumbnailKeys.forEach((key, thumbnailKey) -> {
                String thumbnailUrl = generatePresignedUrl(thumbnailKey, defaultTtl, userId);
                thumbnailUrls.put(key, thumbnailUrl);
            });
        }
    }
    
    // 메타데이터 파싱
    ImageMetadata imageMetadata = null;
    if (detail.getMetadata() != null) {
        Map<String, Object> metadata = JsonUtils.fromJson(detail.getMetadata(), Map.class);
        imageMetadata = ImageMetadata.builder()
                .width((Integer) metadata.get("width"))
                .height((Integer) metadata.get("height"))
                .fileSize(((Number) metadata.get("fileSize")).longValue())
                .format((String) metadata.get("format"))
                .build();
    }
    
    return ImageArtifactDto.builder()
            .type(ArtifactType.IMAGE)
            .filePath(detail.getFilePath())
            .previewUrl(previewUrl)
            .urlExpiresAt(Instant.now().plus(defaultTtl))
            .thumbnailUrls(thumbnailUrls)
            .metadata(imageMetadata)
            .fileName(detail.getFileName())
            .contentType(detail.getContentType())
            .createdAt(detail.getArtifact().getCreatedAt())
            .build();
}
```

### 2.8 이미지 생성 후 썸네일 트리거

```java
// ImageAIService 또는 ProductionService에서
@Service
@RequiredArgsConstructor
public class ImageProductionService {
    
    private final ThumbnailService thumbnailService;
    
    public ProductionResult generateImage(...) {
        // 이미지 생성
        String imagePath = imageAIClient.generateImage(...);
        
        // Artifact 저장
        ProductionArtifactEntity artifact = saveArtifact(imagePath, ...);
        
        // 비동기 썸네일 생성 트리거
        thumbnailService.generateThumbnails(
                artifact.getId(),
                imagePath,
                artifact.getUserId(),
                artifact.getJobId()
        );
        
        return ProductionResult.success(artifact);
    }
}
```

### 2.9 설정

```yaml
# application.yml
thumbnail:
  sizes: [200, 400]  # 썸네일 크기 목록
  async:
    enabled: true
    thread-pool-size: 5
```

### 2.10 비동기 설정

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "thumbnailExecutor")
    public Executor thumbnailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("thumbnail-");
        executor.initialize();
        return executor;
    }
}
```

### 2.11 프론트엔드 사용 예시

```typescript
interface ImageArtifactDto {
  type: 'IMAGE';
  filePath: string;
  previewUrl: string;
  thumbnailUrls: {
    thumbnail_200?: string;
    thumbnail_400?: string;
  };
  metadata: {
    width: number;
    height: number;
    fileSize: number;
    format: string;
  };
}

// 사용
function ImageGallery({ artifact }: { artifact: ImageArtifactDto }) {
  // 썸네일이 있으면 썸네일 사용, 없으면 원본
  const imageUrl = artifact.thumbnailUrls.thumbnail_200 
    || artifact.previewUrl;
  
  return (
    <img 
      src={imageUrl} 
      alt={artifact.fileName}
      loading="lazy"
    />
  );
}
```

---

## 3. 적용 체크리스트

### 3.1 PDF 품질 개선

- [ ] 의존성 추가 (FlexMark, OpenHTMLToPDF)
- [ ] MarkdownParser 인터페이스 및 구현
- [ ] HtmlToPdfConverter 인터페이스 및 구현
- [ ] CssProvider 인터페이스 및 구현
- [ ] ImageResolver 구현
- [ ] EnhancedPdfFormatConverter 구현
- [ ] FormatConverterConfig 설정
- [ ] 테스트 작성

### 3.2 썸네일 전략

- [ ] ImageProcessor 인터페이스 및 구현
- [ ] ImageMetadata 클래스
- [ ] ThumbnailService 구현
- [ ] 비동기 설정
- [ ] ImageArtifactDto 확장
- [ ] ArtifactAccessService 확장
- [ ] 이미지 생성 후 썸네일 트리거
- [ ] 테스트 작성

---

## 4. 예상 효과

### 4.1 PDF 품질
- ✅ Markdown 구조 완벽 지원
- ✅ 스타일링 적용
- ✅ 이미지 포함 가능
- ✅ 테이블 지원

### 4.2 썸네일
- ✅ 빠른 이미지 로딩
- ✅ 대역폭 절감
- ✅ 사용자 경험 향상

