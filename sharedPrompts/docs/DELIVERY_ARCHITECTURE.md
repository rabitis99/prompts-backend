# Delivery 계층 아키텍처

> 이 문서는 [EXECUTION_MODULE_ARCHITECTURE.md](./EXECUTION_MODULE_ARCHITECTURE.md)의 Delivery 계층 상세 설명입니다.

## 목차

1. [Delivery 계층 책임](#1-delivery-계층-책임)
2. [Delivery 인터페이스](#2-delivery-인터페이스)
3. [DeliveryContext](#3-deliverycontext)
4. [DeliveryRegistry](#4-deliveryregistry)
5. [Delivery 모듈 예시](#5-delivery-모듈-예시)
6. [실패 정책](#6-실패-정책)

---

## 1. Delivery 계층 책임

**허용:**
- 이메일 전송
- 블로그 게시
- GitHub/Notion 연동
- 외부 API 호출
- 인증/토큰/계정 정보 관리
- **사용자 입력값 수신 및 활용**

**입력:**
- ProductionArtifact (Production 계층에서 생성된 산출물)
- **사용자 입력값 (DeliveryContext를 통해 전달)**

**금지:**
- Production 계층을 절대 알지 못함
- 콘텐츠 생성 로직 포함 금지

---

## 2. Delivery 인터페이스

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

---

## 3. DeliveryContext

Delivery 과정에 필요한 컨텍스트 정보를 담는다.
사용자 입력값을 포함하여 전송/게시/배포에 필요한 정보를 제공한다.

**⚠️ 타입 안전성 주의:**
- `getAttribute(key, type)` 사용 시 **반드시 타입을 명시**해야 함
- 타입 캐스팅 위험 방지를 위해 `getAttribute(key, Class<T> type)` 메서드 사용 필수
- 임의로 `attributes.get(key)` 직접 사용 금지
- 플랫폼별 설정은 타입 안전한 getter 메서드 사용 권장

**개선 사항:** 플랫폼별 설정을 타입 안전하게 처리하기 위해 강제 타입 검사 메서드 제공.

```java
package org.example.sharedprompts.domain.delivery.api;

import java.util.Map;
import java.util.UUID;

public class DeliveryContext {
    private final String deliveryId;
    private final Long userId;
    private final DeliveryType deliveryType;
    private final UserInputDto userInput;
    private final Map<String, Object> attributes;
    
    public DeliveryContext(Long userId, DeliveryType deliveryType, UserInputDto userInput) {
        this.deliveryId = UUID.randomUUID().toString();
        this.userId = userId;
        this.deliveryType = deliveryType;
        this.userInput = userInput;
        this.attributes = new java.util.HashMap<>();
    }
    
    public String getDeliveryId() { return deliveryId; }
    public Long getUserId() { return userId; }
    public DeliveryType getDeliveryType() { return deliveryType; }
    public UserInputDto getUserInput() { return userInput; }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * ⚠️ 타입 안전한 attribute 조회
     * 반드시 타입을 명시하여 사용해야 함
     * 
     * 올바른 사용:
     *   BlogDeliverySettings settings = context.getAttribute("blogSettings", BlogDeliverySettings.class);
     * 
     * 잘못된 사용:
     *   Object settings = context.attributes.get("blogSettings"); // ❌ 타입 캐스팅 위험
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
    
    // 플랫폼별 타입 안전한 메서드들
    public String getPlatform() {
        return getAttribute("platform", String.class);
    }
    
    // 블로그 전용 설정 (타입 안전)
    public BlogDeliverySettings getBlogSettings() {
        BlogDeliverySettings settings = getAttribute("blogSettings", BlogDeliverySettings.class);
        if (settings == null) {
            throw new IllegalStateException("Blog settings not found in context");
        }
        return settings;
    }
    
    // 이메일 전용 설정 (타입 안전)
    public EmailDeliverySettings getEmailSettings() {
        EmailDeliverySettings settings = getAttribute("emailSettings", EmailDeliverySettings.class);
        if (settings == null) {
            throw new IllegalStateException("Email settings not found in context");
        }
        return settings;
    }
    
    // GitHub 전용 설정 (타입 안전)
    public GitHubDeliverySettings getGitHubSettings() {
        GitHubDeliverySettings settings = getAttribute("githubSettings", GitHubDeliverySettings.class);
        if (settings == null) {
            throw new IllegalStateException("GitHub settings not found in context");
        }
        return settings;
    }
    
    // Notion 전용 설정 (타입 안전)
    public NotionDeliverySettings getNotionSettings() {
        NotionDeliverySettings settings = getAttribute("notionSettings", NotionDeliverySettings.class);
        if (settings == null) {
            throw new IllegalStateException("Notion settings not found in context");
        }
        return settings;
    }
}
```

### 플랫폼별 설정 클래스

```java
// 블로그 전용 설정
public class BlogDeliverySettings {
    private final String platform; // "velog", "tistory", "medium" 등
    private final BlogVisibility visibility; // PUBLIC, PRIVATE, DRAFT
    private final List<String> tags;
    private final String category;
    
    public BlogDeliverySettings(String platform, BlogVisibility visibility, 
                               List<String> tags, String category) {
        this.platform = requireNonNull(platform, "platform must not be null");
        this.visibility = requireNonNull(visibility, "visibility must not be null");
        this.tags = tags != null ? List.copyOf(tags) : List.of();
        this.category = category; // nullable
    }
    
    public String getPlatform() { return platform; }
    public BlogVisibility getVisibility() { return visibility; }
    public List<String> getTags() { return tags; }
    public String getCategory() { return category; }
    
    // 기본값 제공 팩토리 메서드
    public static BlogDeliverySettings of(String platform) {
        return new BlogDeliverySettings(platform, BlogVisibility.PUBLIC, List.of(), null);
    }
}

// 이메일 전용 설정
public class EmailDeliverySettings {
    private final List<String> recipients;
    private final EmailPriority priority; // HIGH, NORMAL, LOW
    private final boolean isHtml;
    private final String subject; // nullable
    
    public EmailDeliverySettings(List<String> recipients, EmailPriority priority, 
                                 boolean isHtml, String subject) {
        this.recipients = requireNonNull(recipients, "recipients must not be null");
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("recipients must not be empty");
        }
        this.priority = requireNonNull(priority, "priority must not be null");
        this.isHtml = isHtml;
        this.subject = subject; // nullable
    }
    
    public List<String> getRecipients() { return recipients; }
    public EmailPriority getPriority() { return priority; }
    public boolean isHtml() { return isHtml; }
    public String getSubject() { return subject; }
    
    // 기본값 제공 팩토리 메서드
    public static EmailDeliverySettings of(List<String> recipients) {
        return new EmailDeliverySettings(recipients, EmailPriority.NORMAL, true, null);
    }
}

// GitHub 전용 설정
public class GitHubDeliverySettings {
    private final String repository; // "owner/repo" 형식
    private final String branch; // 기본값: "main"
    private final String commitMessage;
    private final String prTitle; // nullable (PR 생성 시에만 필요)
    
    public GitHubDeliverySettings(String repository, String branch, 
                                 String commitMessage, String prTitle) {
        this.repository = requireNonNull(repository, "repository must not be null");
        if (!repository.contains("/")) {
            throw new IllegalArgumentException("repository must be in format 'owner/repo'");
        }
        this.branch = branch != null ? branch : "main"; // 기본값
        this.commitMessage = requireNonNull(commitMessage, "commitMessage must not be null");
        this.prTitle = prTitle; // nullable
    }
    
    public String getRepository() { return repository; }
    public String getBranch() { return branch; }
    public String getCommitMessage() { return commitMessage; }
    public String getPrTitle() { return prTitle; }
    
    // 기본값 제공 팩토리 메서드
    public static GitHubDeliverySettings of(String repository, String commitMessage) {
        return new GitHubDeliverySettings(repository, "main", commitMessage, null);
    }
}

// Notion 전용 설정
public class NotionDeliverySettings {
    private final String parentPageId; // nullable (루트 페이지에 생성 시)
    private final String pageTitle;
    private final boolean isPublic;
    
    public NotionDeliverySettings(String parentPageId, String pageTitle, boolean isPublic) {
        this.parentPageId = parentPageId; // nullable
        this.pageTitle = requireNonNull(pageTitle, "pageTitle must not be null");
        this.isPublic = isPublic;
    }
    
    public String getParentPageId() { return parentPageId; }
    public String getPageTitle() { return pageTitle; }
    public boolean isPublic() { return isPublic; }
    
    // 기본값 제공 팩토리 메서드
    public static NotionDeliverySettings of(String pageTitle) {
        return new NotionDeliverySettings(null, pageTitle, false);
    }
}
```

---

## 4. DeliveryRegistry

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

---

## 5. Delivery 모듈 예시

### 5.1 BlogDeliveryService

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
        // 타입 안전한 설정 조회
        BlogDeliverySettings settings = context.getBlogSettings();
        
        return blogPublisher.publish(blogContent, settings, context);
    }
}
```

### 5.2 EmailDeliveryService

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
        // 타입 안전한 설정 조회
        EmailDeliverySettings settings = context.getEmailSettings();
        
        return emailSender.send(emailContent, settings, context);
    }
}
```

### 5.3 GitHubDeliveryService

```java
package org.example.sharedprompts.domain.delivery.github;

@Component
@RequiredArgsConstructor
public class GitHubDeliveryService implements DeliveryService {
    
    private final GitHubClient gitHubClient;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.GITHUB;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        if (artifact.getType() != ArtifactType.FILE && artifact.getType() != ArtifactType.IMAGE) {
            throw new DeliveryException("GitHub delivery requires FILE or IMAGE artifact");
        }
        
        String filePath = artifact.getLocation();
        // 타입 안전한 설정 조회
        GitHubDeliverySettings settings = context.getGitHubSettings();
        
        return gitHubClient.upload(filePath, settings, context);
    }
}
```

### 5.4 NotionDeliveryService

```java
package org.example.sharedprompts.domain.delivery.notion;

@Component
@RequiredArgsConstructor
public class NotionDeliveryService implements DeliveryService {
    
    private final NotionClient notionClient;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.NOTION;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        if (artifact.getType() != ArtifactType.TEXT && artifact.getType() != ArtifactType.FILE) {
            throw new DeliveryException("Notion delivery requires TEXT or FILE artifact");
        }
        
        String content = artifact.getLocation();
        // 타입 안전한 설정 조회
        NotionDeliverySettings settings = context.getNotionSettings();
        
        return notionClient.createPage(content, settings, context);
    }
}
```

---

## 6. 실패 정책

### 6.1 실패 처리 정책 표

| 상황 | 처리 방식 | 예외/결과 타입 | 예시 |
|------|----------|--------------|------|
| **DeliveryService 없음** | **Exception** | `DeliveryServiceNotFoundException` | 등록되지 않은 DeliveryType 요청 |
| **Artifact 타입 불일치** | **Exception** | `DeliveryException` | TEXT Artifact 필요한데 FILE Artifact 전달 |
| **외부 API 호출 실패** | **Result.failure** | `DeliveryResult.failure()` | 블로그 플랫폼 API 오류, 네트워크 오류 |

### 6.2 실패 처리 흐름

```
요청
  │
  └─► [Delivery 단계]
      │
      ├─► Artifact 타입 불일치
      │   └─► DeliveryException ❌
      │
      └─► 외부 API 호출 실패
          └─► DeliveryResult.failure() ⚠️
```

**처리 원칙:**
- **Exception**: 복구 불가능한 오류 (검증 실패, 서비스 없음) → 즉시 중단
- **Result.failure**: 일시적 오류 또는 외부 시스템 실패 → 결과에 포함하여 반환

### 6.3 Exception 계층

```java
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

## 모듈별 책임 요약

| 모듈 | 입력 | 책임 |
|------|------|------|
| EmailDeliveryService | TEXT Artifact | 이메일 전송 |
| BlogDeliveryService | TEXT Artifact | 블로그 게시 (Velog, Tistory, Medium 등) |
| GitHubDeliveryService | FILE/IMAGE Artifact | GitHub 연동 (PR, Commit, Issue 등) |
| NotionDeliveryService | TEXT/FILE Artifact | Notion 페이지 생성 |

