# Production Controller 리팩토링 문서

## 📋 목차

1. [현재 구조 문제 요약](#1-현재-구조-문제-요약)
2. [리팩토링 설계 설명](#2-리팩토링-설계-설명)
3. [권장 패키지 구조](#3-권장-패키지-구조)
4. [리팩토링 후 전체 파일 코드](#4-리팩토링-후-전체-파일-코드)
5. [확장 시 변경 지점 설명](#5-확장-시-변경-지점-설명)
6. [운영 리스크 포인트](#6-운영-리스크-포인트)

---

## 1. 현재 구조 문제 요약

### 1.1 Controller 책임 침범

**문제점:**
- `EmailProductionController`, `DocumentProductionController`, `TextProductionController`, `ImageProductionController`에서:
  - Command 생성 로직이 Controller에 포함됨
  - Validator 호출이 Controller에서 직접 수행됨
  - `JobQueueService`를 Controller에서 직접 호출함

**예시 코드:**
```java
// EmailProductionController.java (기존)
@PostMapping("/email")
public ResponseEntity<CustomResponse<JobResponseDto>> produceEmail(...) {
    // ❌ Command 생성이 Controller에 있음
    EmailCommand command = new EmailCommand(
        request.subject(),
        request.recipient()
    );
    
    // ❌ Validator 호출이 Controller에 있음
    validatorRegistry.validate(command);
    
    // ❌ JobQueueService 직접 호출
    String jobId = jobQueueService.enqueueJob(...);
}
```

### 1.2 중복 코드 및 OCP 위반

**문제점:**
- 타입별로 Controller가 분리되어 있어 중복 코드 발생
- 새로운 타입 추가 시 Controller 복사-붙여넣기 필요
- OCP (Open-Closed Principle) 위반: 확장 시 기존 코드 수정 필요

**영향:**
- Blog, Email, Document, Text, Image 각각의 Controller가 거의 동일한 로직 반복
- 새로운 타입 (예: Video, Audio) 추가 시:
  1. 새로운 Controller 생성 (복사-붙여넣기)
  2. 동일한 로직 재작성
  3. 유지보수 비용 증가

### 1.3 일관성 부족

**문제점:**
- `BlogProductionController`만 `BlogProductionApplicationService`를 사용
- 나머지 Controller들은 Application Service 없이 직접 Domain Service 호출
- 아키텍처 일관성 부족

---

## 2. 리팩토링 설계 설명

### 2.1 설계 원칙

#### 2.1.1 계층별 책임 분리

```
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  - HTTP 요청/응답 처리                   │
│  - @Valid 검증                          │
│  - Application Service 호출             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Application Service Layer           │
│  - 비즈니스 흐름 제어                    │
│  - Factory를 통한 Command 생성           │
│  - Validator를 통한 검증                 │
│  - JobQueueService 호출                  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Domain Service Layer               │
│  - JobQueueService                      │
│  - ValidatorRegistry                    │
│  - JobProcessor                         │
└─────────────────────────────────────────┘
```

#### 2.1.2 전략 패턴 기반 확장 구조

**핵심 아이디어:**
- `ProductionCommandFactory` 인터페이스 정의
- 각 타입별 Factory 구현체 생성
- `ProductionCommandFactoryRegistry`를 통한 자동 등록 및 조회
- 새로운 타입 추가 시 Factory만 추가하면 됨

**장점:**
- OCP 준수: 기존 코드 수정 없이 확장 가능
- SRP 준수: 각 Factory는 단일 타입만 담당
- 테스트 용이성: Factory 단위 테스트 가능

### 2.2 아키텍처 흐름

```
1. HTTP Request
   ↓
2. Controller (@Valid 검증)
   ↓
3. ProductionApplicationService.produce()
   ↓
4. ProductionCommandFactoryRegistry.getFactory()
   ↓
5. ProductionCommandFactory.createCommand()
   ↓
6. ValidatorRegistry.validate()
   ↓
7. JobQueueService.enqueueJob()
   ↓
8. JobResponseDto 반환
```

### 2.3 전략 패턴 구현

#### 2.3.1 Factory 인터페이스

```java
public interface ProductionCommandFactory {
    ProductionCommandType getSupportedCommandType();
    ProductionCommand createCommand(Object request);
    boolean supports(Class<?> requestType);
}
```

#### 2.3.2 Factory 구현체 예시

```java
@Component
public class TextCommandFactory implements ProductionCommandFactory {
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public ProductionCommand createCommand(Object request) {
        TextProductionRequestDto textRequest = (TextProductionRequestDto) request;
        return new TextCommand(textRequest.fileName(), textRequest.format());
    }
    
    @Override
    public boolean supports(Class<?> requestType) {
        return TextProductionRequestDto.class.isAssignableFrom(requestType);
    }
}
```

#### 2.3.3 Registry를 통한 자동 등록

```java
@Component
public class ProductionCommandFactoryRegistry {
    private final Map<Class<?>, ProductionCommandFactory> factoryByRequestType;
    
    public ProductionCommandFactoryRegistry(List<ProductionCommandFactory> factories) {
        // Spring이 자동으로 모든 Factory 구현체를 주입
        // 자동 등록 및 캐싱
    }
}
```

---

## 3. 권장 패키지 구조

```
src/main/java/org/example/sharedprompts/module/
├── controller/
│   └── production/
│       ├── ProductionController.java          # 통합 Controller
│       └── ProductionResultController.java     # 결과 조회 Controller
│
├── domain/
│   └── production/
│       ├── application/
│       │   ├── ProductionApplicationService.java
│       │   └── factory/
│       │       ├── ProductionCommandFactory.java
│       │       ├── ProductionCommandFactoryRegistry.java
│       │       └── impl/
│       │           ├── TextCommandFactory.java
│       │           ├── ImageCommandFactory.java
│       │           ├── EmailCommandFactory.java
│       │           ├── BlogCommandFactory.java
│       │           └── DocumentCommandFactory.java
│       │
│       ├── model/
│       │   ├── contract/
│       │   │   └── command/
│       │   │       ├── ProductionCommand.java
│       │   │       └── ProductionCommandType.java
│       │   └── executor/
│       │       ├── text/TextCommand.java
│       │       ├── image/ImageCommand.java
│       │       ├── email/EmailCommand.java
│       │       ├── blog/BlogCommand.java
│       │       └── document/DocumentCommand.java
│       │
│       ├── service/
│       │   ├── job/
│       │   │   ├── JobQueueService.java
│       │   │   └── JobProcessor.java
│       │   └── validation/
│       │       ├── ValidatorRegistry.java
│       │       └── ProductionValidator.java
│       │
│       └── validation/
│           ├── ValidatorRegistry.java
│           └── ProductionValidator.java
│
└── dto/
    ├── request/
    │   └── production/
    │       ├── TextProductionRequestDto.java
    │       ├── ImageProductionRequestDto.java
    │       ├── EmailProductionRequestDto.java
    │       ├── BlogProductionRequestDto.java
    │       └── DocumentProductionRequestDto.java
    │
    └── response/
        └── production/
            └── JobResponseDto.java
```

---

## 4. 리팩토링 후 전체 파일 코드

### 4.1 ProductionController.java

```java
package org.example.sharedprompts.module.controller.production;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.ProductionApplicationService;
import org.example.sharedprompts.module.dto.request.production.*;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompts/{promptId}/production")
@RequiredArgsConstructor
@Slf4j
public class ProductionController {
    
    private final ProductionApplicationService applicationService;
    
    @PostMapping("/text")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceText(
            @PathVariable Long promptId,
            @Valid @RequestBody TextProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Text production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    @PostMapping("/image")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceImage(
            @PathVariable Long promptId,
            @Valid @RequestBody ImageProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Image production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    @PostMapping("/email")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceEmail(
            @PathVariable Long promptId,
            @Valid @RequestBody EmailProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Email production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    @PostMapping("/blog")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceBlog(
            @PathVariable Long promptId,
            @Valid @RequestBody BlogProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Blog production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    @PostMapping("/document")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceDocument(
            @PathVariable Long promptId,
            @Valid @RequestBody DocumentProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Document production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
}
```

### 4.2 ProductionApplicationService.java

```java
package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactoryRegistry;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.example.sharedprompts.module.domain.production.service.job.JobQueueService;
import org.example.sharedprompts.module.domain.production.validation.ValidatorRegistry;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionApplicationService {
    
    private final ProductionCommandFactoryRegistry factoryRegistry;
    private final ValidatorRegistry validatorRegistry;
    private final JobQueueService jobQueueService;
    
    @Transactional
    public JobResponseDto produce(Long promptId, Long userId, Object request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        log.info("Production requested - promptId: {}, userId: {}, requestType: {}", 
                promptId, userId, request.getClass().getSimpleName());
        
        // 1️⃣ Factory를 통해 Command 생성
        ProductionCommandFactory factory = factoryRegistry.getFactory(request.getClass());
        ProductionCommand command = factory.createCommand(request);
        
        log.debug("Command created - commandType: {}, commandId: {}", 
                command.getCommandType(), command.getCommandId());
        
        // 2️⃣ Command 검증
        validatorRegistry.validate(command);
        
        // 3️⃣ userInput 추출
        String userInput = extractUserInput(request);
        
        // 4️⃣ Job 큐에 추가
        String jobId = jobQueueService.enqueueJob(
                promptId,
                userId,
                command,
                userInput
        );
        
        log.info("Production job enqueued - jobId: {}, userId: {}, commandType: {}", 
                jobId, userId, command.getCommandType());
        
        // 5️⃣ Job 조회하여 응답 생성
        Job job = jobQueueService.getJob(jobId);
        return JobResponseDto.from(job);
    }
    
    private String extractUserInput(Object request) {
        try {
            java.lang.reflect.Method method = request.getClass().getMethod("userInput");
            Object result = method.invoke(request);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to extract userInput from request: {}", e.getMessage());
            return null;
        }
    }
}
```

### 4.3 ProductionCommandFactory.java

```java
package org.example.sharedprompts.module.domain.production.application.factory;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

public interface ProductionCommandFactory {
    ProductionCommandType getSupportedCommandType();
    ProductionCommand createCommand(Object request);
    boolean supports(Class<?> requestType);
}
```

### 4.4 ProductionCommandFactoryRegistry.java

```java
package org.example.sharedprompts.module.domain.production.application.factory;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ProductionCommandFactoryRegistry {
    
    private final Map<Class<?>, ProductionCommandFactory> factoryByRequestType = new ConcurrentHashMap<>();
    private final Map<ProductionCommandType, ProductionCommandFactory> factoryByCommandType = new ConcurrentHashMap<>();
    
    public ProductionCommandFactoryRegistry(List<ProductionCommandFactory> factories) {
        for (ProductionCommandFactory factory : factories) {
            ProductionCommandType commandType = factory.getSupportedCommandType();
            factoryByCommandType.put(commandType, factory);
            
            log.info("Registered ProductionCommandFactory: {} for CommandType: {}", 
                    factory.getClass().getSimpleName(), commandType);
        }
    }
    
    public ProductionCommandFactory getFactory(Class<?> requestType) {
        ProductionCommandFactory cached = factoryByRequestType.get(requestType);
        if (cached != null) {
            return cached;
        }
        
        for (ProductionCommandFactory factory : factoryByCommandType.values()) {
            if (factory.supports(requestType)) {
                factoryByRequestType.put(requestType, factory);
                return factory;
            }
        }
        
        throw new IllegalArgumentException(
                "No ProductionCommandFactory found for request type: " + requestType.getName());
    }
    
    public ProductionCommandFactory getFactory(ProductionCommandType commandType) {
        ProductionCommandFactory factory = factoryByCommandType.get(commandType);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "No ProductionCommandFactory found for command type: " + commandType);
        }
        return factory;
    }
}
```

### 4.5 Factory 구현체 예시 (TextCommandFactory.java)

```java
package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.example.sharedprompts.module.dto.request.production.TextProductionRequestDto;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TextCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public ProductionCommand createCommand(Object request) {
        if (!(request instanceof TextProductionRequestDto textRequest)) {
            throw new IllegalArgumentException(
                    "Expected TextProductionRequestDto, but got: " + 
                    (request != null ? request.getClass().getName() : "null"));
        }
        
        log.debug("Creating TextCommand from request - fileName: {}, format: {}", 
                textRequest.fileName(), textRequest.format());
        
        return new TextCommand(
                textRequest.fileName(),
                textRequest.format()
        );
    }
    
    @Override
    public boolean supports(Class<?> requestType) {
        return TextProductionRequestDto.class.isAssignableFrom(requestType);
    }
}
```

---

## 5. 확장 시 변경 지점 설명

### 5.1 새로운 Production 타입 추가 시 (예: Video)

#### ✅ 수정 불필요한 파일
- `ProductionController.java` (엔드포인트만 추가)
- `ProductionApplicationService.java`
- `ProductionCommandFactoryRegistry.java`
- 기존 Factory 구현체들

#### 📝 추가/수정 필요한 파일

**1. VideoProductionRequestDto.java (새로 생성)**
```java
public record VideoProductionRequestDto(
    @NotBlank String title,
    @NotNull Integer duration,
    String userInput
) {}
```

**2. VideoCommand.java (새로 생성)**
```java
public record VideoCommand(
    @JsonIgnore String commandId,
    String title,
    Integer duration
) implements ProductionCommand {
    // 구현...
}
```

**3. VideoCommandFactory.java (새로 생성)**
```java
@Component
public class VideoCommandFactory implements ProductionCommandFactory {
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.VIDEO;
    }
    
    @Override
    public ProductionCommand createCommand(Object request) {
        VideoProductionRequestDto videoRequest = (VideoProductionRequestDto) request;
        return new VideoCommand(videoRequest.title(), videoRequest.duration());
    }
    
    @Override
    public boolean supports(Class<?> requestType) {
        return VideoProductionRequestDto.class.isAssignableFrom(requestType);
    }
}
```

**4. VideoCommandValidator.java (새로 생성)**
```java
@Component
public class VideoCommandValidator implements ProductionValidator {
    @Override
    public void validate(ProductionCommand command) {
        // 검증 로직...
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.VIDEO;
    }
}
```

**5. ProductionCommandType.java (수정)**
```java
public enum ProductionCommandType {
    TEXT, EMAIL, BLOG, IMAGE, DOCUMENT, VIDEO  // VIDEO 추가
}
```

**6. ProductionController.java (엔드포인트만 추가)**
```java
@PostMapping("/video")
public ResponseEntity<CustomResponse<JobResponseDto>> produceVideo(
        @PathVariable Long promptId,
        @Valid @RequestBody VideoProductionRequestDto request,
        @CurrentUser AuthUser authUser
) {
    log.info("Video production requested - promptId: {}, userId: {}", 
            promptId, authUser.getId());
    
    JobResponseDto response = applicationService.produce(
            promptId,
            authUser.getId(),
            request
    );
    
    return CustomResponseHelper.ok(response);
}
```

### 5.2 변경 지점 요약

| 항목 | 기존 구조 | 리팩토링 후 |
|------|----------|------------|
| **새 타입 추가 시 Controller 수정** | ❌ 새로운 Controller 파일 생성 필요 | ✅ 엔드포인트만 추가 (1개 메서드) |
| **새 타입 추가 시 ApplicationService 수정** | ❌ 수정 필요 | ✅ 수정 불필요 |
| **새 타입 추가 시 Factory 수정** | ❌ 없음 (Factory 패턴 미사용) | ✅ 새로운 Factory만 추가 |
| **코드 중복** | ❌ 타입별 Controller 중복 | ✅ 중복 제거 |
| **OCP 준수** | ❌ 위반 | ✅ 준수 |

---

## 6. 운영 리스크 포인트

### 6.1 리플렉션 사용

**리스크:**
- `ProductionApplicationService.extractUserInput()`에서 리플렉션 사용
- 런타임 오류 가능성

**완화 방안:**
- RequestDto에 공통 인터페이스 도입 고려:
  ```java
  public interface ProductionRequest {
      String userInput();
  }
  ```
- 또는 RequestDto 추상 클래스 도입

**현재 상태:**
- 모든 RequestDto가 `userInput()` 메서드를 가지므로 안전
- 향후 리팩토링 고려

### 6.2 Factory 등록 실패

**리스크:**
- `@Component` 어노테이션 누락 시 Factory가 등록되지 않음
- 런타임에 `IllegalArgumentException` 발생

**완화 방안:**
- 통합 테스트로 모든 Factory 등록 확인
- 애플리케이션 시작 시 로그로 등록 상태 확인

### 6.3 타입 안전성

**리스크:**
- `ProductionApplicationService.produce()`의 `Object request` 파라미터
- 컴파일 타임 타입 체크 불가

**완화 방안:**
- Controller에서 `@Valid` 검증으로 기본 검증 수행
- Factory에서 타입 체크 및 명확한 예외 메시지 제공

### 6.4 성능 고려사항

**리스크:**
- Factory 조회 시 `supports()` 메서드 반복 호출 가능

**완화 방안:**
- `ProductionCommandFactoryRegistry`에서 캐싱 구현
- 첫 조회 후 캐시에 저장하여 성능 최적화

### 6.5 마이그레이션 전략

**단계별 마이그레이션:**
1. ✅ 새로운 구조 구현
2. ✅ 기존 Controller와 병행 운영 (선택적)
3. ✅ 통합 테스트 수행
4. ✅ 기존 Controller 제거
5. ✅ 모니터링 및 검증

**롤백 계획:**
- 기존 Controller 파일을 백업
- 필요 시 즉시 롤백 가능

---

## 7. 결론

### 7.1 달성한 목표

✅ **Controller 책임 한정**
- HTTP 레이어 책임만 수행
- Command 생성, 검증, 비즈니스 로직 제거

✅ **중복 제거**
- 타입별 Controller 중복 제거
- 단일 Controller로 통합

✅ **확장성 확보**
- 전략 패턴 기반 구조
- OCP 준수
- 새로운 타입 추가 시 기존 코드 수정 최소화

✅ **일관성 확보**
- 모든 타입이 동일한 Application Service 사용
- 아키텍처 일관성 유지

### 7.2 향후 개선 사항

1. **RequestDto 공통 인터페이스 도입**
   - 리플렉션 제거
   - 타입 안전성 향상

2. **Factory 조회 성능 최적화**
   - 더 효율적인 캐싱 전략

3. **통합 테스트 강화**
   - 모든 Factory 등록 확인
   - 엔드포인트 통합 테스트

---

**작성일:** 2024년
**작성자:** 시니어 백엔드 아키텍트
**버전:** 1.0

