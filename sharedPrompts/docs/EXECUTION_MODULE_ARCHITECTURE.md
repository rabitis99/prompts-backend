# Content Production & Delivery 아키텍처

## 1. 개요

프로젝트는 Prompt → Production → Delivery 3단계 파이프라인을 가진다.

**핵심 원칙:**
- **Prompt**: 무엇을 생성할지 결정
- **Production**: 콘텐츠 산출물 생성 (외부 시스템 통신 절대 금지)
- **Delivery**: 콘텐츠 외부 전송/게시/배포 (ProductionArtifact를 입력으로 받음)

**책임 분리:**
- Production은 Delivery를 절대 알지 못함
- Delivery는 Production 결과(ProductionArtifact)만 소비
- 둘을 조합하는 책임은 Application/Facade 계층에만 존재

---

## 2. 프로젝트 구조

```
org.example.sharedprompts/
├── domain/
│   ├── prompt/                    # Prompt Core
│   ├── production/                # Content Production Module (산출 전용)
│   │   ├── api/
│   │   │   ├── ProductionCommand.java
│   │   │   ├── ProductionResult.java
│   │   │   ├── ProductionModule.java
│   │   │   ├── ProductionContext.java
│   │   │   └── ProductionArtifact.java
│   │   ├── coordinator/
│   │   │   ├── ProductionCoordinator.java
│   │   │   └── ProductionRegistry.java
│   │   ├── module/
│   │   │   ├── email/             # 이메일 작성
│   │   │   ├── blog/              # 블로그 본문 생성
│   │   │   ├── text/              # 텍스트 파일 산출
│   │   │   └── image/             # 이미지 생성
│   │   └── exception/
│   │       ├── ProductionException.java
│   │       └── CommandValidationException.java
│   │
│   └── delivery/                  # 외부 연동 / 배포 전용
│       ├── github/                # GitHub 연동 (PR, Commit, Issue 등)
│       ├── notion/                 # Notion 페이지 생성
│       ├── email-send/             # 이메일 전송
│       └── blog/                   # 블로그 게시 (Velog, Tistory, Medium 등)
│
└── infra/
    ├── production/                 # Production 구현 상세
    │   ├── email/EmailComposer.java
    │   ├── blog/BlogComposer.java
    │   ├── text/TextFileWriter.java
    │   └── image/ImageGenerator.java
    │
    └── delivery/                   # Delivery 구현 상세
        ├── github/GitHubClient.java
        ├── notion/NotionClient.java
        ├── email-send/EmailSender.java
        └── blog/BlogPublisher.java
```

---

## 3. 계층별 책임

### 3.1 Production 계층 책임

**허용:**
- 이메일 본문 작성
- 블로그 콘텐츠 생성
- 텍스트/이미지 산출물 생성
- ProductionArtifact 반환

**금지:**
- 외부 API 호출
- 네트워크 통신
- 인증/토큰/계정 정보
- 전송, 게시, 업로드 로직

### 3.2 Delivery 계층 책임

**허용:**
- 이메일 전송
- 블로그 게시
- GitHub/Notion 연동
- 외부 API 호출
- 인증/토큰/계정 정보 관리

**입력:**
- ProductionArtifact (Production 계층에서 생성된 산출물)

---

## 4. Production 공통 인터페이스

### 4.1 ProductionCommandType

```java
package org.example.sharedprompts.domain.production.api;

public enum ProductionCommandType {
    EMAIL, BLOG, TEXT, IMAGE;
}
```

### 4.2 ProductionCommand

```java
package org.example.sharedprompts.domain.production.api;

public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();
}
```

### 4.3 ProductionArtifact

**ProductionArtifact는 모든 산출물을 동일하게 다루기 위한 핵심 개념이다.**
TEXT / FILE / IMAGE 타입은 UI, API, 저장소 계층에서 공통 처리하기 위함이다.

**중요:** TEXT Artifact에서 `getLocation()`은 저장 위치가 아닌 **문자열 콘텐츠 자체**를 의미한다.

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

### 4.4 ProductionResult

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

### 4.5 ProductionContext

산출 과정에 필요한 컨텍스트 정보를 담는다.
attributes는 보조적/임시 확장용이며, 인증/권한/외부 시스템 정보 저장 용도가 아니다.

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
    
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }
}
```

### 4.6 ProductionModule

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

## 5. Production Coordinator

### 5.1 ProductionCoordinator

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

### 5.2 ProductionRegistry

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

## 6. Production 모듈 예시

### 6.1 BlogProductionModule

블로그 글 본문 생성. 결과는 TEXT 타입 ProductionArtifact.
플랫폼(Velog, Tistory 등) 언급 없음.

```java
package org.example.sharedprompts.domain.production.module.blog;

@Component
@RequiredArgsConstructor
public class BlogProductionModule implements ProductionModule {
    
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
            
            String blogContent = blogComposer.compose(
                blogCommand.getTitle(),
                promptResult.getContent(),
                blogCommand.getTags()
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

### 6.2 TextProductionModule

```java
package org.example.sharedprompts.domain.production.module.text;

@Component
@RequiredArgsConstructor
public class TextProductionModule implements ProductionModule {
    
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
            
            String filePath = textFileWriter.write(
                promptResult.getContent(),
                textCommand.getFileName(),
                textCommand.getFormat()
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

### 6.3 Artifact 구현 예시

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
    
    @Override
    public String getLocation() {
        return content;
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

## 7. Delivery 계층

### 7.1 Delivery 인터페이스

```java
package org.example.sharedprompts.domain.delivery.api;

import org.example.sharedprompts.domain.production.api.ProductionArtifact;

public enum DeliveryType {
    EMAIL, BLOG, GITHUB, NOTION;
}

public interface DeliveryService {
    DeliveryType getSupportedDeliveryType();
    DeliveryResult deliver(ProductionArtifact artifact, DeliveryContext context) 
            throws DeliveryException;
}
```

### 7.2 DeliveryRegistry

```java
package org.example.sharedprompts.domain.delivery.coordinator;

@Component
public class DeliveryRegistry {
    
    private final Map<DeliveryType, DeliveryService> services = new ConcurrentHashMap<>();
    
    public void register(DeliveryService service) {
        services.put(service.getSupportedDeliveryType(), service);
    }
    
    public DeliveryService find(DeliveryType deliveryType) {
        return services.get(deliveryType);
    }
}
```

### 7.3 Delivery 모듈 예시

#### BlogDeliveryService

Production에서 생성된 TEXT Artifact를 받아 실제 블로그 플랫폼에 게시.
플랫폼별 구현은 delivery 하위에서만 관리.

```java
package org.example.sharedprompts.domain.delivery.blog;

@Component
@RequiredArgsConstructor
public class BlogDeliveryService implements DeliveryService {
    
    private final BlogPublisher blogPublisher;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.BLOG;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        if (artifact.getType() != ArtifactType.TEXT) {
            throw new DeliveryException("Blog delivery requires TEXT artifact");
        }
        
        String blogContent = artifact.getLocation();
        String platform = context.getPlatform();
        
        return blogPublisher.publish(blogContent, platform, context);
    }
}
```

#### EmailDeliveryService

```java
package org.example.sharedprompts.domain.delivery.email;

@Component
@RequiredArgsConstructor
public class EmailDeliveryService implements DeliveryService {
    
    private final EmailSender emailSender;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.EMAIL;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        if (artifact.getType() != ArtifactType.TEXT) {
            throw new DeliveryException("Email delivery requires TEXT artifact");
        }
        
        String emailContent = artifact.getLocation();
        
        return emailSender.send(emailContent, context);
    }
}
```

---

## 8. 연결점 (Facade)

```java
package org.example.sharedprompts.domain.prompt.facade;

@Component
@RequiredArgsConstructor
public class PromptProductionDeliveryFacade {
    
    private final PromptCreationFlow promptCreationFlow;
    private final ProductionCoordinator productionCoordinator;
    private final DeliveryRegistry deliveryRegistry;
    
    @Transactional
    public PromptProductionDeliveryResponse createProduceAndDeliver(
            PromptRequestDto request, 
            Long userId,
            ProductionCommand productionCommand,
            DeliveryContext deliveryContext
    ) {
        PromptResponseDto promptResult = promptCreationFlow.create(request, userId);
        
        ProductionContext productionContext = new ProductionContext(userId);
        productionContext.setAttribute("promptResult", promptResult);
        
        ProductionResult productionResult = productionCoordinator.produce(
            productionCommand, 
            productionContext
        );
        
        if (!productionResult.isSuccess()) {
            return PromptProductionDeliveryResponse.of(
                promptResult, 
                productionResult, 
                null
            );
        }
        
        DeliveryService deliveryService = deliveryRegistry.find(deliveryContext.getDeliveryType());
        
        if (deliveryService == null) {
            throw new DeliveryServiceNotFoundException(deliveryContext.getDeliveryType());
        }
        
        DeliveryResult deliveryResult = deliveryService.deliver(
            productionResult.getArtifact(),
            deliveryContext
        );
        
        return PromptProductionDeliveryResponse.of(
            promptResult, 
            productionResult, 
            deliveryResult
        );
    }
}
```

---

## 9. 모듈 등록

```java
package org.example.sharedprompts.domain.production.config;

@Configuration
public class ProductionModuleConfig {
    
    @Bean
    public ProductionRegistry productionRegistry(
            EmailProductionModule emailModule,
            BlogProductionModule blogModule,
            TextProductionModule textModule,
            ImageProductionModule imageModule
    ) {
        ProductionRegistry registry = new ProductionRegistry();
        registry.register(emailModule);
        registry.register(blogModule);
        registry.register(textModule);
        registry.register(imageModule);
        return registry;
    }
}
```

---

## 10. 실패 정책

**검증 실패 / 잘못된 요청 → Exception**
- CommandValidationException
- ProductionModuleNotFoundException
- DeliveryException
- DeliveryServiceNotFoundException

**콘텐츠 생성 실패 → Result.failure**
- 파일 시스템 오류
- 콘텐츠 생성 실패

### Exception 계층

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

package org.example.sharedprompts.domain.delivery.exception;

public class DeliveryException extends RuntimeException {
    public DeliveryException(String message) {
        super(message);
    }
}

public class DeliveryServiceNotFoundException extends DeliveryException {
    public DeliveryServiceNotFoundException(DeliveryType deliveryType) {
        super("Delivery service not found for type: " + deliveryType);
    }
}
```

---

## 11. 모듈별 책임 요약

### Production 계층

| 모듈 | 산출물 타입 | 산출물 |
|------|------------|--------|
| EmailProductionModule | TEXT | 이메일 본문 문자열 |
| BlogProductionModule | TEXT | 블로그 콘텐츠 문자열 |
| TextProductionModule | FILE | 텍스트 파일 경로 (TXT/MD) |
| ImageProductionModule | IMAGE | 이미지 파일 경로 (PNG/JPG) |

### Delivery 계층

| 모듈 | 입력 | 책임 |
|------|------|------|
| EmailDeliveryService | TEXT Artifact | 이메일 전송 |
| BlogDeliveryService | TEXT Artifact | 블로그 게시 (Velog, Tistory, Medium 등) |
| GitHubDeliveryService | FILE/IMAGE Artifact | GitHub 연동 (PR, Commit, Issue 등) |
| NotionDeliveryService | TEXT/FILE Artifact | Notion 페이지 생성 |

---

## 12. 요약

**핵심 설계 원칙:**
1. Prompt → Production → Delivery 3단계 파이프라인
2. Production은 Delivery를 절대 알지 못함
3. Delivery는 ProductionArtifact만 소비
4. Application/Facade 계층이 둘을 조합
5. Production은 외부 시스템 통신 절대 금지
6. 검증 실패 → Exception, 생성 실패 → Result.failure

**설계 기준:**
"콘텐츠를 만든다"와 "콘텐츠를 보낸다"는 다른 문제다.
만든 것은 production, 보낸 것은 delivery다.
