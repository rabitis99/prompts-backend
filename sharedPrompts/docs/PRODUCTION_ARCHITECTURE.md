# Production 계층 아키텍처

> 이 문서는 [EXECUTION_MODULE_ARCHITECTURE.md](./EXECUTION_MODULE_ARCHITECTURE.md)의 Production 계층 상세 설명입니다.

## 목차

1. [Production 계층 책임](#1-production-계층-책임)
2. [Production 공통 인터페이스](#2-production-공통-인터페이스)
3. [Production Coordinator](#3-production-coordinator)
4. [Production 모듈 예시](#4-production-모듈-예시)
5. [Artifact 구현](#5-artifact-구현)
6. [모듈 등록](#6-모듈-등록)
7. [실패 정책](#7-실패-정책)

---

## 1. Production 계층 책임

**허용:**
- 이메일 본문 작성
- 블로그 콘텐츠 생성
- 텍스트/이미지 산출물 생성
- ProductionArtifact 반환
- **프롬프트 결과와 사용자 입력값을 조합하여 콘텐츠 생성 (Compose 역할)**
- **Infra/Adapter 계층의 AI Client를 통한 콘텐츠 생성 (직접 AI 호출 금지)**

**금지:**
- **AI Client 직접 구현 및 토큰 관리 (Infra/Adapter 계층에서만 관리)**
- 외부 API 직접 호출 (AI Client를 통한 호출만 허용)
- 네트워크 통신 직접 수행 (AI Client를 통한 통신만 허용)
- 인증/토큰/계정 정보 관리
- 전송, 게시, 업로드 로직

**설계 원칙:**
- **ProductionModule은 단순히 "compose" 역할만 수행**
- **AI Client는 Infra/Adapter 계층에서만 관리** (테스트 용이성 확보 및 토큰 노출 최소화)
- **ProductionModule에서 AI Client 직접 호출 절대 금지** (Composer/Generator를 통해서만 사용)

---

## 2. Production 공통 인터페이스

### 2.1 ProductionCommandType

```java
package org.example.sharedprompts.domain.production.api;

public enum ProductionCommandType {
    EMAIL, BLOG, TEXT, IMAGE, DOCUMENT;
}
```

### 2.2 ProductionCommand

```java
package org.example.sharedprompts.domain.production.api;

public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();
}
```

### 2.3 ProductionArtifact

**ProductionArtifact는 모든 산출물을 동일하게 다루기 위한 핵심 개념이다.**
TEXT / FILE / IMAGE 타입은 UI, API, 저장소 계층에서 공통 처리하기 위함이다.

**⚠️ 중요: Artifact 타입별 getLocation() 의미**
- **TEXT Artifact**: `getLocation()`은 **실제 파일 경로가 아닌 문자열 콘텐츠 자체**를 반환
- **FILE Artifact**: `getLocation()`은 **실제 파일 경로**를 반환
- **IMAGE Artifact**: `getLocation()`은 **실제 이미지 파일 경로**를 반환

**주의:** TEXT Artifact와 FILE/IMAGE Artifact를 혼용 시 혼동 가능하므로, 타입을 먼저 확인한 후 사용해야 함.

```java
package org.example.sharedprompts.domain.production.api;

public enum ArtifactType {
    TEXT, FILE, IMAGE;
}

public interface ProductionArtifact {
    ArtifactType getType();
    String getLocation();
}
```

### 2.4 ProductionResult

산출 결과를 나타낸다. startedAt / completedAt은 운영 및 디버깅 목적이다.

```java
package org.example.sharedprompts.domain.production.api;

import java.time.Instant;

public interface ProductionResult {
    boolean isSuccess();
    String getErrorMessage();
    Instant getStartedAt();
    Instant getCompletedAt();
    ProductionArtifact getArtifact();
}
```

### 2.5 ProductionContext

산출 과정에 필요한 컨텍스트 정보를 담는다.
attributes는 보조적/임시 확장용이며, 인증/권한/외부 시스템 정보 저장 용도가 아니다.

**⚠️ 타입 안전성 주의:**
- `getAttribute(key, type)` 사용 시 **반드시 타입을 명시**해야 함
- 타입 캐스팅 위험 방지를 위해 `getAttribute(key, Class<T> type)` 메서드 사용 필수
- 임의로 `attributes.get(key)` 직접 사용 금지

```java
package org.example.sharedprompts.domain.production.api;

import java.util.Map;
import java.util.UUID;

public class ProductionContext {
    private final String productionId;
    private final Long userId;
    private final Map<String, Object> attributes;
    
    public ProductionContext(Long userId) {
        this.productionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.attributes = new java.util.HashMap<>();
    }
    
    public String getProductionId() { return productionId; }
    public Long getUserId() { return userId; }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * ⚠️ 타입 안전한 attribute 조회
     * 반드시 타입을 명시하여 사용해야 함
     * 
     * 올바른 사용:
     *   PromptResponseDto result = context.getAttribute("promptResult", PromptResponseDto.class);
     * 
     * 잘못된 사용:
     *   Object result = context.attributes.get("promptResult"); // ❌ 타입 캐스팅 위험
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException(
                String.format("Attribute '%s' is not of type %s, but %s", 
                    key, type.getName(), value.getClass().getName())
            );
        }
        return (T) value;
    }
}
```

### 2.6 ProductionModule

콘텐츠 산출 모듈 인터페이스. 각 모듈은 산출물 생성을 책임진다.

```java
package org.example.sharedprompts.domain.production.api;

public interface ProductionModule {
    ProductionCommandType getSupportedCommandType();
    ProductionResult produce(ProductionCommand command, ProductionContext context) 
            throws ProductionException;
}
```

---

## 3. Production Coordinator

### 3.1 ProductionCoordinator

CommandType enum 기반으로 적절한 모듈에 산출 요청을 라우팅한다.

```java
package org.example.sharedprompts.domain.production.coordinator;

@Component
@RequiredArgsConstructor
public class ProductionCoordinator {
    
    private final ProductionRegistry registry;
    
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        ProductionModule module = registry.findModule(command.getCommandType());
        
        if (module == null) {
            throw new ProductionModuleNotFoundException(command.getCommandType());
        }
        
        return module.produce(command, context);
    }
}
```

### 3.2 ProductionRegistry

```java
package org.example.sharedprompts.domain.production.coordinator;

@Component
public class ProductionRegistry {
    
    private final Map<ProductionCommandType, ProductionModule> modules = new ConcurrentHashMap<>();
    
    public void register(ProductionModule module) {
        modules.put(module.getSupportedCommandType(), module);
    }
    
    public ProductionModule findModule(ProductionCommandType commandType) {
        return modules.get(commandType);
    }
}
```

---

## 4. Production 모듈 예시

### 4.1 BlogProductionModule

블로그 글 본문 생성. 결과는 TEXT 타입 ProductionArtifact.
플랫폼(Velog, Tistory 등) 언급 없음.

**설계 원칙:** ProductionModule은 단순히 "compose" 역할만 수행하며, 실제 AI 호출은 Infra 계층의 BlogComposer가 AI Client를 통해 수행.

```java
package org.example.sharedprompts.domain.production.module.blog;

@Component
@RequiredArgsConstructor
public class BlogProductionModule implements ProductionModule {
    
    // BlogComposer는 Infra 계층에서 AI Client를 주입받아 사용
    private final BlogComposer blogComposer;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.BLOG;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof BlogCommand blogCommand)) {
            throw new CommandValidationException(
                "Expected BlogCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            UserInputDto userInput = context.getAttribute("userInput", UserInputDto.class);
            
            // BlogComposer가 Infra 계층의 AI Client를 통해 콘텐츠 생성
            // ProductionModule은 단순히 compose 역할만 수행
            String blogContent = blogComposer.compose(
                blogCommand.getTitle(),
                promptResult.getContent(),
                blogCommand.getTags(),
                userInput
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new TextArtifact(blogContent);
            return BlogResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return BlogResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}
```

### 4.2 TextProductionModule

**설계 원칙:** ProductionModule은 단순히 "compose" 역할만 수행하며, 실제 AI 호출은 Infra 계층의 TextFileWriter가 AI Client를 통해 수행.

```java
package org.example.sharedprompts.domain.production.module.text;

@Component
@RequiredArgsConstructor
public class TextProductionModule implements ProductionModule {
    
    // TextFileWriter는 Infra 계층에서 AI Client를 주입받아 사용
    private final TextFileWriter textFileWriter;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof TextCommand textCommand)) {
            throw new CommandValidationException(
                "Expected TextCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            UserInputDto userInput = context.getAttribute("userInput", UserInputDto.class);
            
            // TextFileWriter가 Infra 계층의 AI Client를 통해 콘텐츠 생성 후 파일 저장
            // ProductionModule은 단순히 compose 역할만 수행
            String filePath = textFileWriter.write(
                promptResult.getContent(),
                textCommand.getFileName(),
                textCommand.getFormat(),
                userInput
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new FileArtifact(filePath);
            return TextResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return TextResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}
```

### 4.3 EmailProductionModule

이메일 본문 생성. 결과는 TEXT 타입 ProductionArtifact.
전송 로직은 포함하지 않음.

**설계 원칙:** ProductionModule은 단순히 "compose" 역할만 수행하며, 실제 AI 호출은 Infra 계층의 EmailComposer가 AI Client를 통해 수행.

```java
package org.example.sharedprompts.domain.production.module.email;

@Component
@RequiredArgsConstructor
public class EmailProductionModule implements ProductionModule {
    
    // EmailComposer는 Infra 계층에서 AI Client를 주입받아 사용
    private final EmailComposer emailComposer;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.EMAIL;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof EmailCommand emailCommand)) {
            throw new CommandValidationException(
                "Expected EmailCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            if (promptResult == null) {
                return EmailResult.failure("PromptResult not found in context", startedAt, Instant.now());
            }
            
            String emailContent = emailComposer.compose(
                emailCommand.getSubject(),
                promptResult.getContent(),
                emailCommand.getRecipient()
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new TextArtifact(emailContent);
            return EmailResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return EmailResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}
```

### 4.4 ImageProductionModule

이미지 생성. 결과는 IMAGE 타입 ProductionArtifact.

**설계 원칙:** ProductionModule은 단순히 "compose" 역할만 수행하며, 실제 AI 호출은 Infra 계층의 ImageGenerator가 AI Client를 통해 수행.

```java
package org.example.sharedprompts.domain.production.module.image;

@Component
@RequiredArgsConstructor
public class ImageProductionModule implements ProductionModule {
    
    // ImageGenerator는 Infra 계층에서 AI Client를 주입받아 사용
    private final ImageGenerator imageGenerator;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.IMAGE;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof ImageCommand imageCommand)) {
            throw new CommandValidationException(
                "Expected ImageCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            UserInputDto userInput = context.getAttribute("userInput", UserInputDto.class);
            
            // ImageGenerator가 Infra 계층의 AI Client를 통해 이미지 생성
            // ProductionModule은 단순히 compose 역할만 수행
            String imagePath = imageGenerator.generate(
                promptResult.getContent(),
                imageCommand.getStyle(),
                imageCommand.getSize(),
                userInput
            );
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new ImageArtifact(imagePath);
            return ImageResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return ImageResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}
```

### 4.5 DocumentProductionModule

문서 생성 (Excel, Word, HWP). 결과는 FILE 타입 ProductionArtifact.

**설계 원칙:** 
- AI를 통해 문서 내용 생성 후, Apache POI를 사용하여 문서 포맷팅
- 파일 저장 후 FILE Artifact 반환

```java
package org.example.sharedprompts.domain.production.module.document;

@Component
@RequiredArgsConstructor
public class DocumentProductionModule implements ProductionModule {
    
    private final DocumentGenerator documentGenerator; // Apache POI 사용
    private final TextAiClient textAiClient; // 텍스트 생성 AI (문서 내용 생성용)
    private final DocumentStorage documentStorage; // 파일 저장용
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.DOCUMENT;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof DocumentCommand documentCommand)) {
            throw new CommandValidationException(
                "Expected DocumentCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            UserInputDto userInput = context.getAttribute("userInput", UserInputDto.class);
            
            // 1. AI를 통해 문서 내용 생성 (필요 시)
            String content = promptResult.getContent();
            if (userInput.requiresAiEnhancement()) {
                content = textAiClient.generateText(
                    buildDocumentPrompt(content, userInput), 
                    buildOptions(userInput)
                );
            }
            
            // 2. Apache POI를 사용하여 문서 생성
            byte[] documentBytes;
            String extension;
            switch (documentCommand.getFormat()) {
                case "docx":
                    documentBytes = documentGenerator.generateWordDocument(content);
                    extension = "docx";
                    break;
                case "xlsx":
                    documentBytes = documentGenerator.generateExcelDocument(parseToTable(content));
                    extension = "xlsx";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + documentCommand.getFormat());
            }
            
            // 3. 파일 저장
            DocumentStorage.FileMetadata metadata = documentStorage.saveFile(documentBytes, extension);
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new FileArtifact(metadata.filePath().toString());
            return DocumentResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            return DocumentResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}
```

> **참고:** AI Client 구현 및 AWS 배포 전략은 [AI_INFRASTRUCTURE.md](./AI_INFRASTRUCTURE.md)를 참조하세요.

---

## 5. Artifact 구현

```java
package org.example.sharedprompts.domain.production.api;

public class TextArtifact implements ProductionArtifact {
    private final String content;
    
    public TextArtifact(String content) {
        this.content = content;
    }
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.TEXT;
    }
    
    /**
     * ⚠️ 주의: TEXT Artifact의 getLocation()은 실제 파일 경로가 아닌
     * 문자열 콘텐츠 자체를 반환합니다.
     * FILE/IMAGE Artifact와 혼동하지 않도록 주의하세요.
     */
    @Override
    public String getLocation() {
        return content; // 파일 경로가 아닌 콘텐츠 자체
    }
}

public class FileArtifact implements ProductionArtifact {
    private final String filePath;
    
    public FileArtifact(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.FILE;
    }
    
    @Override
    public String getLocation() {
        return filePath;
    }
}

public class ImageArtifact implements ProductionArtifact {
    private final String imagePath;
    
    public ImageArtifact(String imagePath) {
        this.imagePath = imagePath;
    }
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.IMAGE;
    }
    
    @Override
    public String getLocation() {
        return imagePath;
    }
}
```

---

## 6. 모듈 등록

```java
package org.example.sharedprompts.domain.production.config;

@Configuration
public class ProductionModuleConfig {
    
    @Bean
    public ProductionRegistry productionRegistry(
            EmailProductionModule emailModule,
            BlogProductionModule blogModule,
            TextProductionModule textModule,
            ImageProductionModule imageModule,
            DocumentProductionModule documentModule
    ) {
        ProductionRegistry registry = new ProductionRegistry();
        registry.register(emailModule);
        registry.register(blogModule);
        registry.register(textModule);
        registry.register(imageModule);
        registry.register(documentModule);
        return registry;
    }
}
```

---

## 7. 실패 정책

### 7.1 실패 처리 정책 표

| 상황 | 처리 방식 | 예외/결과 타입 | 예시 |
|------|----------|--------------|------|
| **Command 검증 실패** | **Exception** | `CommandValidationException` | BlogCommand가 필요한데 EmailCommand 전달 |
| **ProductionModule 없음** | **Exception** | `ProductionModuleNotFoundException` | 등록되지 않은 CommandType 요청 |
| **AI 생성 실패** | **Result.failure** | `ProductionResult.failure()` | AI API 호출 실패, 타임아웃 |
| **파일 시스템 오류** | **Result.failure** | `ProductionResult.failure()` | 디스크 공간 부족, 권한 없음 |
| **콘텐츠 생성 실패** | **Result.failure** | `ProductionResult.failure()` | 모델 오류, 잘못된 프롬프트 |

### 7.2 실패 처리 흐름

```
요청
  │
  ├─► [검증 단계]
  │   │
  │   ├─► Command 타입 불일치
  │   │   └─► CommandValidationException ❌
  │   │
  │   └─► Module 없음
  │       └─► ProductionModuleNotFoundException ❌
  │
  └─► [Production 단계]
      │
      ├─► AI 호출 실패
      │   └─► ProductionResult.failure() ⚠️
      │
      └─► 파일 시스템 오류
          └─► ProductionResult.failure() ⚠️
```

**처리 원칙:**
- **Exception**: 복구 불가능한 오류 (검증 실패, 모듈 없음) → 즉시 중단
- **Result.failure**: 일시적 오류 또는 비즈니스 로직 실패 → 결과에 포함하여 반환

### 7.3 Exception 계층

```java
package org.example.sharedprompts.domain.production.exception;

public class ProductionException extends RuntimeException {
    public ProductionException(String message) {
        super(message);
    }
}

public class ProductionModuleNotFoundException extends ProductionException {
    public ProductionModuleNotFoundException(ProductionCommandType commandType) {
        super("Production module not found for type: " + commandType);
    }
}

public class CommandValidationException extends ProductionException {
    public CommandValidationException(String message) {
        super(message);
    }
}
```

---

## 모듈별 책임 요약

| 모듈 | 산출물 타입 | 산출물 |
|------|------------|--------|
| EmailProductionModule | TEXT | 이메일 본문 문자열 |
| BlogProductionModule | TEXT | 블로그 콘텐츠 문자열 |
| TextProductionModule | FILE | 텍스트 파일 경로 (TXT/MD) |
| ImageProductionModule | IMAGE | 이미지 파일 경로 (PNG/JPG) |
| DocumentProductionModule | FILE | 문서 파일 경로 (DOCX/XLSX) |

