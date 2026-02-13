# Production Application Service 정밀 리팩토링 문서

## 📋 목차

1. [개선 전 문제점 요약](#1-개선-전-문제점-요약)
2. [개선 설계 설명](#2-개선-설계-설명)
3. [권장 패키지 구조](#3-권장-패키지-구조)
4. [수정된 전체 파일 코드](#4-수정된-전체-파일-코드)
5. [예외 처리 구조 개선](#5-예외-처리-구조-개선)

---

## 1. 개선 전 문제점 요약

### 1.1 타입 안정성 문제

**문제점:**
- `ProductionApplicationService.produce()` 메서드가 `Object request` 파라미터 사용
- 컴파일 타임 타입 체크 불가
- 런타임에 타입 오류 발생 가능

```java
// ❌ 기존 코드
@Transactional
public JobResponseDto produce(Long promptId, Long userId, Object request) {
    // Object 타입으로 인해 컴파일 타임 체크 불가
    ProductionCommandFactory factory = factoryRegistry.getFactory(request.getClass());
    ProductionCommand command = factory.createCommand(request);
}
```

### 1.2 리플렉션 사용

**문제점:**
- `extractUserInput()` 메서드에서 리플렉션으로 `userInput()` 메서드 호출
- 런타임 오류 가능성
- 성능 저하
- 코드 가독성 저하

```java
// ❌ 기존 코드
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
```

### 1.3 런타임 계약 (Hidden Contract)

**문제점:**
- RequestDto가 `userInput()` 메서드를 가져야 한다는 암묵적 계약
- 컴파일 타임에 검증 불가
- 문서화되지 않은 의존성

### 1.4 FactoryRegistry 동적 탐색

**문제점:**
- `supports()` 메서드를 반복 호출하여 Factory 탐색
- O(n) 시간 복잡도
- 런타임 오버헤드

```java
// ❌ 기존 코드
public ProductionCommandFactory getFactory(Class<?> requestType) {
    // supports() 메서드로 동적 확인
    for (ProductionCommandFactory factory : factoryByCommandType.values()) {
        if (factory.supports(requestType)) {
            factoryByRequestType.put(requestType, factory);
            return factory;
        }
    }
    throw new IllegalArgumentException(...);
}
```

### 1.5 예외 처리 부족

**문제점:**
- `IllegalArgumentException` 남용
- 도메인 예외 부재
- 예외 계층 구조 없음

---

## 2. 개선 설계 설명

### 2.1 타입 안정성 확보

#### 2.1.1 ProductionRequest 인터페이스 도입

**설계:**
- 모든 RequestDto가 구현해야 하는 공통 인터페이스 정의
- `userInput()` 메서드를 명시적 계약으로 정의

```java
public interface ProductionRequest {
    String userInput();
}
```

**장점:**
- 컴파일 타임 타입 체크 가능
- 리플렉션 제거
- 명시적 계약으로 문서화 효과

#### 2.1.2 ApplicationService 타입 안전 시그니처

**개선:**
```java
// ✅ 개선된 코드
@Transactional
public JobResponseDto produce(Long promptId, Long userId, ProductionRequest request) {
    // ProductionRequest 타입으로 컴파일 타임 체크
    String userInput = request.userInput(); // 리플렉션 없이 직접 호출
}
```

### 2.2 명시적 매핑 구조

#### 2.2.1 Factory 인터페이스 개선

**개선:**
- `getSupportedRequestType()` 메서드 추가
- `supports()` 메서드 제거
- 명시적 타입 계약

```java
public interface ProductionCommandFactory {
    ProductionCommandType getSupportedCommandType();
    Class<? extends ProductionRequest> getSupportedRequestType(); // 명시적 계약
    ProductionCommand createCommand(ProductionRequest request); // 타입 안전
}
```

#### 2.2.2 FactoryRegistry 명시적 매핑

**개선:**
- 초기화 시 `getSupportedRequestType()`으로 명시적 매핑 생성
- O(1) 조회 성능 보장

```java
// ✅ 개선된 코드
public ProductionCommandFactoryRegistry(List<ProductionCommandFactory> factories) {
    for (ProductionCommandFactory factory : factories) {
        Class<? extends ProductionRequest> requestType = factory.getSupportedRequestType();
        factoryByRequestType.put(requestType, factory); // 명시적 매핑
    }
}

public ProductionCommandFactory getFactory(Class<? extends ProductionRequest> requestType) {
    ProductionCommandFactory factory = factoryByRequestType.get(requestType); // O(1) 조회
    if (factory == null) {
        throw new ProductionCommandFactoryNotFoundException(requestType);
    }
    return factory;
}
```

### 2.3 예외 처리 개선

#### 2.3.1 도메인 예외 정의

**계층 구조:**
```
ProductionApplicationException (기본 예외)
    └── ProductionCommandFactoryNotFoundException (Factory 미발견)
```

**장점:**
- 명확한 예외 의미
- 예외 핸들러에서 구체적 처리 가능
- 운영 환경에서 적절한 HTTP 응답 매핑

---

## 3. 권장 패키지 구조

```
src/main/java/org/example/sharedprompts/module/
├── dto/
│   └── request/
│       └── production/
│           ├── ProductionRequest.java              # 공통 인터페이스
│           ├── TextProductionRequestDto.java
│           ├── ImageProductionRequestDto.java
│           ├── EmailProductionRequestDto.java
│           ├── BlogProductionRequestDto.java
│           └── DocumentProductionRequestDto.java
│
├── domain/
│   └── production/
│       ├── application/
│       │   ├── ProductionApplicationService.java
│       │   ├── exception/
│       │   │   ├── ProductionApplicationException.java
│       │   │   └── ProductionCommandFactoryNotFoundException.java
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
│       └── ...
```

---

## 4. 수정된 전체 파일 코드

### 4.1 ProductionRequest.java

```java
package org.example.sharedprompts.module.dto.request.production;

/**
 * Production 요청 공통 인터페이스
 * 
 * <p>모든 Production RequestDto는 이 인터페이스를 구현해야 한다.
 * 컴파일 타임 타입 안정성을 보장하고, 리플렉션 없이 userInput을 추출할 수 있다.</p>
 */
public interface ProductionRequest {
    
    /**
     * 사용자 입력값을 반환한다.
     * 
     * @return 사용자 입력값 (없으면 null)
     */
    String userInput();
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
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 Production Application Service
 * 
 * <p>타입 안전한 구조로 Production 요청을 처리한다.
 * 리플렉션 없이 컴파일 타임 타입 체크가 가능하다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionApplicationService {
    
    private final ProductionCommandFactoryRegistry factoryRegistry;
    private final ValidatorRegistry validatorRegistry;
    private final JobQueueService jobQueueService;
    
    /**
     * Production 요청을 처리하고 Job을 생성한다.
     * 
     * @param promptId 프롬프트 ID
     * @param userId 사용자 ID
     * @param request ProductionRequest (타입 안전)
     * @return JobResponseDto
     */
    @Transactional
    public JobResponseDto produce(Long promptId, Long userId, ProductionRequest request) {
        log.info("Production requested - promptId: {}, userId: {}, requestType: {}", 
                promptId, userId, request.getClass().getSimpleName());
        
        // Factory를 통해 Command 생성 (명시적 타입 매핑)
        ProductionCommandFactory factory = factoryRegistry.getFactory(request.getClass());
        ProductionCommand command = factory.createCommand(request);
        
        log.debug("Command created - commandType: {}, commandId: {}", 
                command.getCommandType(), command.getCommandId());
        
        // Command 검증
        validatorRegistry.validate(command);
        
        // userInput 추출 (인터페이스 메서드로 타입 안전하게 추출)
        String userInput = request.userInput();
        
        // Job 큐에 추가
        String jobId = jobQueueService.enqueueJob(
                promptId,
                userId,
                command,
                userInput
        );
        
        log.info("Production job enqueued - jobId: {}, userId: {}, commandType: {}", 
                jobId, userId, command.getCommandType());
        
        // Job 조회하여 응답 생성
        Job job = jobQueueService.getJob(jobId);
        return JobResponseDto.from(job);
    }
}
```

### 4.3 ProductionCommandFactory.java

```java
package org.example.sharedprompts.module.domain.production.application.factory;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;

/**
 * ProductionCommand 생성 전략 인터페이스
 * 
 * <p>각 Production 타입별로 ProductionRequest를 Command로 변환하는 책임을 가진다.
 * 명시적 타입 계약을 통해 컴파일 타임 타입 안정성을 보장한다.</p>
 */
public interface ProductionCommandFactory {
    
    /**
     * 이 Factory가 지원하는 Command 타입을 반환한다.
     */
    ProductionCommandType getSupportedCommandType();
    
    /**
     * 이 Factory가 지원하는 Request 타입을 반환한다.
     * 명시적 계약을 통해 런타임 탐색 없이 Factory를 찾을 수 있다.
     */
    Class<? extends ProductionRequest> getSupportedRequestType();
    
    /**
     * ProductionRequest를 ProductionCommand로 변환한다.
     * 
     * @param request ProductionRequest 인스턴스
     * @return ProductionCommand 인스턴스
     * @throws IllegalArgumentException request가 지원하지 않는 타입이거나 null인 경우
     */
    ProductionCommand createCommand(ProductionRequest request);
}
```

### 4.4 ProductionCommandFactoryRegistry.java

```java
package org.example.sharedprompts.module.domain.production.application.factory;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionCommandFactoryNotFoundException;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductionCommandFactory 레지스트리
 * 
 * <p>명시적 매핑 기반으로 Factory를 등록하고 조회한다.
 * getSupportedRequestType()을 통해 런타임 탐색 없이 O(1) 조회가 가능하다.</p>
 */
@Component
@Slf4j
public class ProductionCommandFactoryRegistry {
    
    private final Map<Class<? extends ProductionRequest>, ProductionCommandFactory> factoryByRequestType;
    private final Map<ProductionCommandType, ProductionCommandFactory> factoryByCommandType;
    
    /**
     * 모든 Factory 구현체를 자동 주입받아 명시적으로 등록한다.
     */
    public ProductionCommandFactoryRegistry(List<ProductionCommandFactory> factories) {
        this.factoryByRequestType = new ConcurrentHashMap<>();
        this.factoryByCommandType = new ConcurrentHashMap<>();
        
        for (ProductionCommandFactory factory : factories) {
            ProductionCommandType commandType = factory.getSupportedCommandType();
            Class<? extends ProductionRequest> requestType = factory.getSupportedRequestType();
            
            // 명시적 매핑 등록
            factoryByCommandType.put(commandType, factory);
            factoryByRequestType.put(requestType, factory);
            
            log.info("Registered ProductionCommandFactory: {} for CommandType: {}, RequestType: {}", 
                    factory.getClass().getSimpleName(), commandType, requestType.getSimpleName());
        }
    }
    
    /**
     * Request 타입에 따라 Factory를 조회한다.
     * 명시적 매핑을 사용하므로 O(1) 조회 성능을 보장한다.
     * 
     * @param requestType ProductionRequest의 Class
     * @return 해당하는 Factory
     * @throws ProductionCommandFactoryNotFoundException 지원하지 않는 Request 타입인 경우
     */
    public ProductionCommandFactory getFactory(Class<? extends ProductionRequest> requestType) {
        ProductionCommandFactory factory = factoryByRequestType.get(requestType);
        if (factory == null) {
            throw new ProductionCommandFactoryNotFoundException(requestType);
        }
        return factory;
    }
    
    /**
     * Command 타입에 따라 Factory를 조회한다.
     * 
     * @param commandType ProductionCommandType
     * @return 해당하는 Factory
     * @throws ProductionCommandFactoryNotFoundException 지원하지 않는 Command 타입인 경우
     */
    public ProductionCommandFactory getFactory(ProductionCommandType commandType) {
        ProductionCommandFactory factory = factoryByCommandType.get(commandType);
        if (factory == null) {
            throw new ProductionCommandFactoryNotFoundException(
                    "ProductionCommandFactory not found for command type: " + commandType);
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
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.example.sharedprompts.module.dto.request.production.TextProductionRequestDto;
import org.springframework.stereotype.Component;

/**
 * TextCommand 생성 Factory
 */
@Component
@Slf4j
public class TextCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return TextProductionRequestDto.class;
    }
    
    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
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
}
```

### 4.6 RequestDto 예시 (TextProductionRequestDto.java)

```java
package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.NotBlank;

public record TextProductionRequestDto(
    @NotBlank String fileName,
    @NotBlank String format,
    String userInput
) implements ProductionRequest {}
```

### 4.7 예외 클래스

#### 4.7.1 ProductionApplicationException.java

```java
package org.example.sharedprompts.module.domain.production.application.exception;

/**
 * Production Application 계층에서 발생하는 예외의 기본 클래스
 */
public class ProductionApplicationException extends RuntimeException {
    
    public ProductionApplicationException(String message) {
        super(message);
    }
    
    public ProductionApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### 4.7.2 ProductionCommandFactoryNotFoundException.java

```java
package org.example.sharedprompts.module.domain.production.application.exception;

/**
 * ProductionCommandFactory를 찾을 수 없을 때 발생하는 예외
 */
public class ProductionCommandFactoryNotFoundException extends ProductionApplicationException {
    
    public ProductionCommandFactoryNotFoundException(String message) {
        super(message);
    }
    
    public ProductionCommandFactoryNotFoundException(Class<?> requestType) {
        super("ProductionCommandFactory not found for request type: " + requestType.getName());
    }
}
```

---

## 5. 예외 처리 구조 개선

### 5.1 예외 계층 구조

```
RuntimeException
    └── ProductionApplicationException
        └── ProductionCommandFactoryNotFoundException
```

### 5.2 예외 핸들러 등록 (권장)

```java
@RestControllerAdvice(basePackages = "org.example.sharedprompts.module")
public class ModuleExceptionHandler {
    
    @ExceptionHandler(ProductionCommandFactoryNotFoundException.class)
    public ResponseEntity<?> handleProductionCommandFactoryNotFoundException(
            ProductionCommandFactoryNotFoundException e) {
        log.warn("ProductionCommandFactory not found: {}", e.getMessage());
        return CustomResponseHelper.fail(
                new ApiException(ErrorCode.INVALID_INPUT_VALUE, 
                        "지원하지 않는 Production 타입입니다."));
    }
    
    @ExceptionHandler(ProductionApplicationException.class)
    public ResponseEntity<?> handleProductionApplicationException(
            ProductionApplicationException e) {
        log.error("Production application error: {}", e.getMessage(), e);
        return CustomResponseHelper.fail(
                new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, 
                        "Production 처리 중 오류가 발생했습니다."));
    }
}
```

### 5.3 개선 효과

**Before:**
- `IllegalArgumentException` 남용
- 예외 의미 불명확
- 예외 핸들러에서 구체적 처리 불가

**After:**
- 도메인 예외로 명확한 의미 전달
- 예외 핸들러에서 구체적 처리 가능
- 운영 환경에서 적절한 HTTP 응답 매핑

---

## 6. 개선 효과 요약

### 6.1 타입 안정성

| 항목 | Before | After |
|------|--------|-------|
| **컴파일 타임 타입 체크** | ❌ 불가 (Object 사용) | ✅ 가능 (ProductionRequest) |
| **리플렉션 사용** | ❌ 사용 (extractUserInput) | ✅ 제거 |
| **런타임 타입 오류** | ⚠️ 가능 | ✅ 방지 |

### 6.2 성능

| 항목 | Before | After |
|------|--------|-------|
| **Factory 조회 시간 복잡도** | O(n) (supports() 반복) | O(1) (명시적 매핑) |
| **리플렉션 오버헤드** | ⚠️ 있음 | ✅ 없음 |

### 6.3 코드 품질

| 항목 | Before | After |
|------|--------|-------|
| **명시적 계약** | ❌ 암묵적 (userInput 메서드) | ✅ 명시적 (ProductionRequest) |
| **예외 처리** | ⚠️ IllegalArgumentException 남용 | ✅ 도메인 예외 |
| **코드 가독성** | ⚠️ 리플렉션으로 인한 복잡도 | ✅ 명확한 인터페이스 |

### 6.4 확장성

| 항목 | Before | After |
|------|--------|-------|
| **OCP 준수** | ✅ 유지 | ✅ 유지 |
| **새 타입 추가 시** | Factory + supports() 구현 | Factory + getSupportedRequestType() 구현 |
| **타입 안정성** | ❌ 런타임 체크 | ✅ 컴파일 타임 체크 |

---

## 7. 마이그레이션 가이드

### 7.1 단계별 마이그레이션

1. **ProductionRequest 인터페이스 생성**
2. **모든 RequestDto에 implements ProductionRequest 추가**
3. **ProductionCommandFactory 인터페이스 수정**
4. **모든 Factory 구현체 수정**
5. **ProductionCommandFactoryRegistry 수정**
6. **ProductionApplicationService 수정**
7. **예외 클래스 생성 및 적용**

### 7.2 호환성

- Controller는 변경 불필요 (이미 ProductionRequest를 구현한 DTO 사용)
- 기존 확장성(OCP) 유지
- 새로운 타입 추가 시에도 동일한 패턴 적용

---

**작성일:** 2024년
**작성자:** 시니어 백엔드 아키텍트
**버전:** 2.0


