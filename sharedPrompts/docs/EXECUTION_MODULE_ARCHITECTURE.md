# Content Production & Delivery 아키텍처

## ⚠️ 중요 주의사항

### Artifact 타입 구분 (반드시 확인!)

| Artifact 타입 | `getLocation()` 반환값 | 사용 예시 |
|--------------|----------------------|----------|
| **TEXT** | **실제 콘텐츠 문자열** (파일 경로 아님) | `String content = artifact.getLocation();` |
| **FILE** | **파일 경로** | `Path filePath = Paths.get(artifact.getLocation());` |
| **IMAGE** | **이미지 파일 경로** | `Path imagePath = Paths.get(artifact.getLocation());` |

**⚠️ 주의:** TEXT Artifact의 `getLocation()`은 파일 경로가 아닌 **콘텐츠 자체**를 반환합니다. FILE/IMAGE와 혼동하지 않도록 타입을 먼저 확인하세요!

```java
// 올바른 사용 예시
if (artifact.getType() == ArtifactType.TEXT) {
    String content = artifact.getLocation(); // 콘텐츠 자체
} else if (artifact.getType() == ArtifactType.FILE) {
    Path filePath = Paths.get(artifact.getLocation()); // 파일 경로
}
```

### Production / Delivery 책임 분리

**Production 계층:**
- ✅ 콘텐츠 생성만 수행
- ✅ AI 호출은 **Infra/Adapter 계층의 AI Client를 통해서만** 사용
- ❌ **ProductionModule에서 AI Client 직접 호출 절대 금지**
- ❌ Delivery 계층을 절대 알지 못함

**Delivery 계층:**
- ✅ 콘텐츠 전송/게시/업로드만 수행
- ✅ ProductionArtifact만 소비
- ❌ Production 계층을 절대 알지 못함

**Facade 계층:**
- ✅ Production과 Delivery를 조합하는 유일한 책임

---

## 문서 구조

이 아키텍처 문서는 다음과 같이 구성되어 있습니다:

- **[PRODUCTION_ARCHITECTURE.md](./PRODUCTION_ARCHITECTURE.md)**: Production 계층 상세 설명
- **[DELIVERY_ARCHITECTURE.md](./DELIVERY_ARCHITECTURE.md)**: Delivery 계층 상세 설명
- **[AI_INFRASTRUCTURE.md](./AI_INFRASTRUCTURE.md)**: AI Client 및 AWS 배포 전략

---

## 1. 개요

프로젝트는 Prompt → Production → Delivery 3단계 파이프라인을 가진다.

**핵심 원칙:**
- **Prompt**: 무엇을 생성할지 결정
- **Production**: 콘텐츠 산출물 생성 (AI 연동 허용, 프롬프트 결과 + 사용자 입력값 사용)
- **Delivery**: 콘텐츠 외부 전송/게시/배포 (ProductionArtifact를 입력으로 받음, 사용자 입력값 사용)

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
│   │   │   ├── email/             # 이메일 작성 (AI 연동, 프롬프트 결과 + 사용자 입력값 사용)
│   │   │   ├── blog/              # 블로그 본문 생성 (AI 연동, 프롬프트 결과 + 사용자 입력값 사용)
│   │   │   ├── text/              # 텍스트 파일 산출 (AI 연동, 프롬프트 결과 + 사용자 입력값 사용)
│   │   │   ├── image/             # 이미지 생성 (AI 연동, 프롬프트 결과 + 사용자 입력값 사용)
│   │   │   └── document/          # 문서 생성 (Excel, Word, HWP - Apache POI 사용)
│   │   └── exception/
│   │       ├── ProductionException.java
│   │       └── CommandValidationException.java
│   │
│   └── delivery/                  # 외부 연동 / 배포 전용
│       ├── github/                # GitHub 연동 (PR, Commit, Issue 등, 사용자 입력값 사용)
│       ├── notion/                 # Notion 페이지 생성 (사용자 입력값 사용)
│       ├── email-send/             # 이메일 전송 (사용자 입력값 사용)
│       └── blog/                   # 블로그 게시 (Velog, Tistory, Medium 등, 사용자 입력값 사용)
│
└── infra/
    ├── ai/                         # AI Client (Adapter 계층)
    │   ├── text/                   # 텍스트 생성 AI
    │   │   ├── TextAiClient.java        # 텍스트 생성 인터페이스
    │   │   ├── LlamaTextAiClient.java   # LLaMA 구현 (오픈소스)
    │   │   ├── VicunaTextAiClient.java  # Vicuna 구현 (오픈소스)
    │   │   ├── BloomTextAiClient.java   # BLOOM 구현 (오픈소스)
    │   │   ├── FalconTextAiClient.java  # Falcon 구현 (오픈소스)
    │   │   ├── GeminiTextAiClient.java  # Google Gemini 구현
    │   │   ├── OpenAiTextClient.java    # OpenAI 구현
    │   │   └── AnthropicTextClient.java # Anthropic 구현
    │   └── image/                  # 이미지 생성 AI
    │       ├── ImageAiClient.java       # 이미지 생성 인터페이스
    │       ├── StableDiffusionClient.java # Stable Diffusion 구현 (오픈소스)
    │       ├── DreamShaperClient.java    # DreamShaper 구현 (오픈소스)
    │       ├── OpenJourneyClient.java    # OpenJourney 구현 (오픈소스)
    │       ├── WaifuDiffusionClient.java # Waifu Diffusion 구현 (오픈소스)
    │       ├── DalleImageClient.java     # DALL-E 구현
    │       └── IdeogramClient.java      # Ideogram 구현 (선택사항)
    │
    ├── production/                 # Production 구현 상세
    │   ├── email/EmailComposer.java    # AI Client를 주입받아 사용
    │   ├── blog/BlogComposer.java      # AI Client를 주입받아 사용
    │   ├── text/TextFileWriter.java    # AI Client를 주입받아 사용
    │   ├── image/ImageGenerator.java   # AI Client를 주입받아 사용
    │   └── document/DocumentGenerator.java  # 문서 생성 (Apache POI 사용)
    │
    └── delivery/                   # Delivery 구현 상세
        ├── github/GitHubClient.java
        ├── notion/NotionClient.java
        ├── email-send/EmailSender.java
        └── blog/BlogPublisher.java
```

---

## 3. 계층별 책임 요약

### 3.1 Production 계층 책임

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

> **상세 내용**: [PRODUCTION_ARCHITECTURE.md](./PRODUCTION_ARCHITECTURE.md) 참조

### 3.2 Delivery 계층 책임

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

> **상세 내용**: [DELIVERY_ARCHITECTURE.md](./DELIVERY_ARCHITECTURE.md) 참조

---

## 4. 연결점 (Facade)

### 4.1 전체 흐름도

```
┌─────────────┐
│   Client    │
│  (Request)  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│     PromptProductionDeliveryFacade │
│         (Facade 계층)               │
└──────┬──────────────────────────────┘
       │
       ├─► ┌──────────────────┐
       │   │ PromptCreationFlow│
       │   │   (Prompt 생성)   │
       │   └────────┬──────────┘
       │            │
       │            ▼
       │   ┌──────────────────┐
       │   │ PromptResponseDto│
       │   └──────────────────┘
       │
       ├─► ┌──────────────────────┐
       │   │ ProductionCoordinator │
       │   │  (Production 실행)    │
       │   └──────────┬────────────┘
       │              │
       │              ▼
       │   ┌──────────────────────┐
       │   │  ProductionModule    │
       │   │  (Blog/Email/Image 등)│
       │   └──────────┬─────────────┘
       │              │
       │              ▼
       │   ┌──────────────────────┐
       │   │  ProductionArtifact  │
       │   │  (TEXT/FILE/IMAGE)   │
       │   └──────────────────────┘
       │
       └─► ┌──────────────────────┐
           │  DeliveryRegistry     │
           │  (Delivery 실행)      │
           └──────────┬─────────────┘
                     │
                     ▼
           ┌──────────────────────┐
           │  DeliveryService      │
           │  (Blog/Email/GitHub 등)│
           └──────────────────────┘
```

### 4.2 시퀀스 다이어그램

```
Client → Facade → PromptFlow → ProductionCoordinator → ProductionModule → DeliveryService
  │        │          │              │                    │                  │
  │        │          │              │                    │                  │
  │        │    [Prompt 생성]        │                    │                  │
  │        │◄─────────┘              │                    │                  │
  │        │                          │                    │                  │
  │        │    [Production 실행]     │                    │                  │
  │        │──────────►               │                    │                  │
  │        │                          │                    │                  │
  │        │                    [모듈 선택]                 │                  │
  │        │                          │──────────►        │                  │
  │        │                          │                    │                  │
  │        │                          │         [AI Client 호출]              │
  │        │                          │                    │──► Infra/AI     │
  │        │                          │                    │◄───              │
  │        │                          │                    │                  │
  │        │                          │         [Artifact 생성]              │
  │        │                          │◄───────────────────┘                  │
  │        │                          │                    │                  │
  │        │    [ProductionResult]     │                    │                  │
  │        │◄─────────────────────────┘                    │                  │
  │        │                          │                    │                  │
  │        │    [Delivery 실행]       │                    │                  │
  │        │──────────►               │                    │                  │
  │        │                          │                    │                  │
  │        │                    [서비스 선택]               │                  │
  │        │                          │                    │                  │
  │        │                          │                    │         [전송/게시]
  │        │                          │                    │                  │──► 외부 API
  │        │                          │                    │                  │◄───
  │        │                          │                    │                  │
  │        │                          │                    │    [DeliveryResult]
  │        │                          │                    │                  │
  │        │    [최종 응답]           │                    │                  │
  │◄───────┘                          │                    │                  │
```

### 4.3 코드 구현

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
        // 사용자 입력값을 ProductionContext에 설정
        productionContext.setAttribute("userInput", request.getUserInput());
        
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

## 5. 요약

**핵심 설계 원칙:**
1. Prompt → Production → Delivery 3단계 파이프라인
2. Production은 Delivery를 절대 알지 못함
3. Delivery는 ProductionArtifact만 소비
4. Application/Facade 계층이 둘을 조합
5. **AI Client는 Infra/Adapter 계층에서만 관리 (토큰 노출 최소화, 테스트 용이성)**
6. **ProductionModule은 단순히 "compose" 역할만 수행 (AI Client 직접 호출 금지)**
7. Production은 프롬프트 결과와 사용자 입력값을 조합하여 콘텐츠 생성
8. Delivery는 타입 안전한 설정 객체를 통해 전송/게시/배포 수행
9. **Artifact 타입별 getLocation() 의미 구분 필수 (TEXT는 콘텐츠, FILE/IMAGE는 경로)**
10. 검증 실패 → Exception, 생성 실패 → Result.failure

**AI Client 설계 원칙:**
- 텍스트 생성과 이미지 생성을 별도 인터페이스로 분리
- 각 AI 제공자별로 독립적인 구현 클래스 작성
- 필요 시 AI 제공자 교체 가능 (의존성 주입으로 변경)
- CircuitBreaker 패턴으로 장애 대응
- 별도 스레드 풀 사용으로 Tomcat 스레드 보호

**AWS 클라우드 환경 배포 원칙:**
- **하이브리드 구성 권장**: 오픈소스 모델(비용 절감) + 클라우드 API(고품질)
- **인프라 선택**: ECS/Fargate(애플리케이션), ECS/Fargate 또는 SageMaker(AI 모델)
- **네트워크 보안**: Private Subnet 배치, Security Group 최소 권한 원칙
- **설정 관리**: AWS Systems Manager Parameter Store 활용
- **모니터링**: CloudWatch Metrics 및 Logs 통합
- **비용 최적화**: Auto Scaling, Spot Instance(개발), Reserved Instance(프로덕션)
- **Fallback 전략**: 클라우드 API 실패 시 오픈소스 모델로 자동 전환

> **상세 내용**: [AI_INFRASTRUCTURE.md](./AI_INFRASTRUCTURE.md) 참조

**설계 기준:**
"콘텐츠를 만든다"와 "콘텐츠를 보낸다"는 다른 문제다.
만든 것은 production, 보낸 것은 delivery다.

---

## 관련 문서

- **[PRODUCTION_ARCHITECTURE.md](./PRODUCTION_ARCHITECTURE.md)**: Production 계층 상세 설명
  - **Production 계층 책임**: 허용/금지 사항, 설계 원칙
  - **Production 공통 인터페이스**: ProductionCommand, ProductionArtifact, ProductionResult, ProductionContext, ProductionModule
  - **Production Coordinator**: ProductionCoordinator, ProductionRegistry (모듈 라우팅 및 등록)
  - **Production 모듈 예시**: BlogProductionModule, TextProductionModule, EmailProductionModule, ImageProductionModule, DocumentProductionModule
  - **Artifact 구현**: TextArtifact, FileArtifact, ImageArtifact (타입별 getLocation() 의미 구분)
  - **모듈 등록**: ProductionModuleConfig를 통한 모듈 등록
  - **실패 정책**: 검증 실패(Exception) vs 생성 실패(Result.failure) 구분

- **[DELIVERY_ARCHITECTURE.md](./DELIVERY_ARCHITECTURE.md)**: Delivery 계층 상세 설명 (TODO: 실제 코드 구현 필요)
  - Delivery 인터페이스
  - DeliveryContext 및 플랫폼별 설정
  - Delivery 모듈 예시
  - 실패 정책

- **[AI_INFRASTRUCTURE.md](./AI_INFRASTRUCTURE.md)**: AI Client 및 인프라 배포 전략 (TODO: 실제 코드 구현 필요)
  - AI Client 인터페이스 및 구현
  - AI 모델별 특징 및 권장 사용처
  - AWS 클라우드 환경 배포 시나리오
  - 비용 최적화 및 모니터링 전략
