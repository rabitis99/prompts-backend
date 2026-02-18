# Production Validation 개선 제안

## 현재 상태 분석

### 1. 두 가지 검증 시스템

#### ResponseValidator (AI 응답 검증)
- **위치**: `service/validator` 패키지
- **목적**: AI가 생성한 응답(JsonNode) 검증
- **사용 시점**: AI 응답 파싱 후
- **구현체**: `BlogValidator`, `DocumentValidator`, `EmailValidator`, `ContentResponseValidator`

#### ProductionValidator (Command 입력 검증)
- **위치**: `validation` 패키지
- **목적**: 사용자 입력 Command 검증
- **사용 시점**: Command 생성 직후, Job 큐에 추가 전
- **구현체**: `BlogCommandValidator`, `DocumentCommandValidator`, `EmailCommandValidator`, `TextCommandValidator`, `ImageCommandValidator`

## 개선 제안

### 1. 명명 규칙 개선

#### 문제점
- `BlogValidator`와 `BlogCommandValidator`가 혼동을 야기
- 두 시스템의 목적이 명확하지 않음

#### 제안
```
ResponseValidator 계열:
- BlogValidator → BlogResponseValidator
- DocumentValidator → DocumentResponseValidator  
- EmailValidator → EmailResponseValidator

또는 패키지명으로 구분:
- service.validator.response.*
- validation.command.*
```

### 2. 코드 중복 제거

#### 문제점
- 포맷 검증 로직이 `DocumentCommandValidator`와 `TextCommandValidator`에 중복
- 이메일 형식 검증 로직이 재사용 불가

#### 제안
```java
// 공통 유틸리티 클래스 생성
public class FormatValidator {
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        "markdown", "md", "json", "txt", "html", "xml", "csv", "pdf"
    );
    
    public static void validateFormat(String format) {
        // ...
    }
}

public class EmailFormatValidator {
    private static final Pattern EMAIL_PATTERN = ...;
    
    public static void validateEmail(String email) {
        // ...
    }
}
```

### 3. ValidatorRegistry 최적화

#### 현재 코드 문제
```java
// 비효율적: 각 validator마다 모든 CommandType 순회
for (ProductionValidator validator : validators) {
    for (ProductionCommandType commandType : ProductionCommandType.values()) {
        if (validator.supports(commandType)) {
            validatorMap.put(commandType, validator);
            break;
        }
    }
}
```

#### 개선안
```java
public ValidatorRegistry(List<ProductionValidator> validators) {
    for (ProductionValidator validator : validators) {
        // supports()를 한 번만 호출하여 직접 매핑
        ProductionCommandType supportedType = findSupportedType(validator);
        if (supportedType != null) {
            validatorMap.put(supportedType, validator);
            log.info("Registered Validator: {} for CommandType: {}", 
                    validator.getClass().getSimpleName(), supportedType);
        }
    }
}

private ProductionCommandType findSupportedType(ProductionValidator validator) {
    return Arrays.stream(ProductionCommandType.values())
            .filter(validator::supports)
            .findFirst()
            .orElse(null);
}
```

### 4. 포맷 검증 일관성

#### 문제점
- `DocumentCommandValidator`: `pdf` 지원
- `TextCommandValidator`: `pdf` 미지원
- 포맷 목록이 하드코딩되어 있음

#### 제안
```java
// 설정 기반 포맷 관리
public enum SupportedFormat {
    MARKDOWN("markdown", "md"),
    JSON("json"),
    TXT("txt"),
    HTML("html"),
    XML("xml"),
    CSV("csv"),
    PDF("pdf"); // Document 전용
    
    private final Set<String> aliases;
    
    public static boolean isSupported(String format, boolean includePdf) {
        // ...
    }
}
```

### 5. 검증 예외 처리 일관성

#### 현재 상태
- `ResponseValidator`: `ParseException` 사용
- `ProductionValidator`: `ValidationException` 사용

#### 제안
- 두 시스템 모두 `ValidationException` 사용하거나
- 목적에 맞게 구분 유지 (ParseException은 파싱 오류, ValidationException은 검증 오류)

### 6. 테스트 커버리지

#### 제안
- 각 Validator에 대한 단위 테스트 추가
- 경계값 테스트 (최소/최대 크기, 포맷 등)
- 예외 케이스 테스트

## 분리 가능한 작업

### Phase 1: 리팩토링 (기능 변경 없음)
1. ✅ 명명 규칙 개선
2. ✅ 공통 유틸리티 추출
3. ✅ ValidatorRegistry 최적화

### Phase 2: 기능 개선
1. ✅ 포맷 검증 일관성 확보
2. ✅ 설정 기반 포맷 관리
3. ✅ 테스트 추가

### Phase 3: 문서화
1. ✅ 검증 시스템 아키텍처 문서화
2. ✅ 각 Validator의 책임 명확화

## 우선순위

1. **높음**: ValidatorRegistry 최적화 (성능 영향)
2. **중간**: 포맷 검증 중복 제거 (유지보수성)
3. **낮음**: 명명 규칙 개선 (가독성)

