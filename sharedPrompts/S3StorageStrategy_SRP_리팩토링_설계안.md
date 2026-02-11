# S3StorageStrategy SRP 리팩토링 설계안

## 1️⃣ 현재 클래스 문제점

### 책임 집중 (SRP 위반)
- **S3 업로드 로직**: AWS S3 클라이언트를 통한 파일 업로드
- **체크섬 생성**: SHA-256 해시 생성 (S3와 무관한 범용 기능)
- **Content-Type 추측**: 파일 확장자 기반 MIME 타입 추측 (S3와 무관한 범용 기능)
- **S3 키 경로 생성**: 사용자/작업 기반 경로 생성 (S3 전용이지만 전략으로 분리 가능)

### 중복 코드
- `generateChecksum()` 메서드가 `LocalStorageStrategy`와 중복 구현됨
- 체크섬 생성 로직은 저장 전략과 무관한 범용 유틸리티

### 테스트 용이성 저하
- 체크섬, Content-Type 추측 등 범용 로직이 S3 전략에 결합되어 단위 테스트가 복잡함
- 각 책임을 독립적으로 테스트하기 어려움

---

## 2️⃣ 권장 분리 구조

```
src/main/java/org/example/sharedprompts/
├── module/domain/production/service/storage/
│   ├── StorageStrategy.java (인터페이스)
│   ├── StorageType.java
│   ├── S3StorageStrategy.java (리팩토링 후)
│   ├── LocalStorageStrategy.java (리팩토링 후)
│   └── path/
│       └── S3KeyBuilder.java (신규 - S3 키 경로 생성 전략)
│
└── global/util/
    ├── ChecksumUtils.java (신규 - 체크섬 생성 유틸리티)
    └── ContentTypeUtils.java (신규 - Content-Type 추측 유틸리티)
```

---

## 3️⃣ 분리 포인트 & 제안

### 3.1 체크섬 생성 → `ChecksumUtils` (공통 유틸리티)
**이유**: 
- 저장 전략과 무관한 범용 기능
- `LocalStorageStrategy`와 중복 제거
- 다른 모듈에서도 재사용 가능

**위치**: `org.example.sharedprompts.global.util.ChecksumUtils`

**메서드**:
```java
public static String generateSha256(String content)
public static String generateSha256(byte[] data)
```

### 3.2 Content-Type 추측 → `ContentTypeUtils` (공통 유틸리티)
**이유**:
- 파일 확장자 기반 MIME 타입 추측은 범용 기능
- HTTP 응답, 파일 다운로드 등 다양한 곳에서 재사용 가능

**위치**: `org.example.sharedprompts.global.util.ContentTypeUtils`

**메서드**:
```java
public static String guessContentType(String fileName)
```

### 3.3 S3 키 경로 생성 → `S3KeyBuilder` (선택적 분리)
**이유**:
- S3 전용이지만 경로 생성 전략을 분리하면 테스트 및 확장 용이
- 다양한 경로 전략 적용 가능 (예: 날짜 기반, 해시 기반 등)

**위치**: `org.example.sharedprompts.module.domain.production.service.storage.path.S3KeyBuilder`

**메서드**:
```java
public String buildKey(Long userId, String jobId, String fileName)
```

**참고**: 현재는 단순한 경로 생성이므로 `S3StorageStrategy` 내부 private 메서드로 유지해도 무방. 향후 확장 가능성을 고려하여 분리 제안.

### 3.4 예외 처리
- `S3StorageException`은 `S3StorageStrategy` 내부 static 클래스로 유지 (S3 전용 예외)
- 또는 `storage.exception` 패키지로 분리 가능 (선택적)

---

## 4️⃣ 기대 효과

### 유지보수 용이성
- ✅ 체크섬 알고리즘 변경 시 한 곳만 수정
- ✅ Content-Type 매핑 추가/수정 시 한 곳만 수정
- ✅ S3 업로드 로직 변경 시 `S3StorageStrategy`만 수정

### 테스트 용이성
- ✅ `ChecksumUtils`, `ContentTypeUtils`는 독립적으로 단위 테스트 가능
- ✅ `S3StorageStrategy`는 Mock을 통한 S3 업로드 로직만 테스트
- ✅ 각 책임별 테스트 코드 작성 및 유지보수 용이

### 재사용성
- ✅ `ChecksumUtils`: 파일 무결성 검증, 데이터 비교 등 다양한 곳에서 재사용
- ✅ `ContentTypeUtils`: HTTP 응답 헤더, 파일 다운로드 등에서 재사용
- ✅ `LocalStorageStrategy`도 동일한 유틸리티 사용하여 중복 제거

### SRP 준수
- ✅ `S3StorageStrategy`: S3 업로드 전략만 담당
- ✅ `ChecksumUtils`: 체크섬 생성만 담당
- ✅ `ContentTypeUtils`: Content-Type 추측만 담당

---

## 5️⃣ 리팩토링 후 클래스 구조

### S3StorageStrategy (리팩토링 후)
```java
@Component
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
public class S3StorageStrategy implements StorageStrategy {
    
    private final S3Client s3Client;
    private final S3KeyBuilder keyBuilder; // 또는 내부 private 메서드
    
    @Override
    public String store(String content, Long userId, String jobId, String fileName) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        String contentType = ContentTypeUtils.guessContentType(fileName);
        return store(data, contentType, userId, jobId, fileName);
    }
    
    @Override
    public String generateChecksum(String content) {
        return ChecksumUtils.generateSha256(content);
    }
    
    // S3 업로드 로직만 집중
}
```

### ChecksumUtils
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChecksumUtils {
    public static String generateSha256(String content) { ... }
    public static String generateSha256(byte[] data) { ... }
}
```

### ContentTypeUtils
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentTypeUtils {
    public static String guessContentType(String fileName) { ... }
}
```

---

## 6️⃣ 마이그레이션 계획

1. ✅ `ChecksumUtils` 생성 및 체크섬 로직 이동
2. ✅ `ContentTypeUtils` 생성 및 Content-Type 추측 로직 이동
3. ✅ `S3StorageStrategy` 리팩토링 (유틸리티 사용)
4. ✅ `LocalStorageStrategy` 리팩토링 (중복 제거)
5. ✅ `StorageStrategy` 인터페이스의 `generateChecksum` 메서드 유지 (하위 호환성)

