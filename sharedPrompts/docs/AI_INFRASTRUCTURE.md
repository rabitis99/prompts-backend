# AI Infrastructure 아키텍처

> 이 문서는 [EXECUTION_MODULE_ARCHITECTURE.md](./EXECUTION_MODULE_ARCHITECTURE.md)의 AI Client 및 인프라 배포 전략 상세 설명입니다.

## 목차

1. [AI Client 설계 원칙](#1-ai-client-설계-원칙)
2. [AI Client 인터페이스 및 구현](#2-ai-client-인터페이스-및-구현)
3. [AI 모델별 특징 및 권장 사용처](#3-ai-모델별-특징-및-권장-사용처)
4. [AWS 클라우드 환경 배포](#4-aws-클라우드-환경-배포)

---

## 1. AI Client 설계 원칙

**중요 원칙:**
- **텍스트 생성과 이미지 생성은 서로 다른 AI를 사용할 수 있음**
- 텍스트 생성: LLM (LLaMA, Vicuna, BLOOM, Falcon, MPT, Google Gemini, OpenAI, Anthropic 등)
- 이미지 생성: 이미지 생성 전용 AI (Stable Diffusion, DreamShaper, OpenJourney, Waifu Diffusion, DALL-E, Midjourney 등)
- 각 AI Client는 독립적으로 구현 및 교체 가능

**설계 원칙:**
- 텍스트 생성과 이미지 생성을 별도 인터페이스로 분리
- 각 AI 제공자별로 독립적인 구현 클래스 작성
- CircuitBreaker 패턴으로 장애 대응
- 별도 스레드 풀 사용으로 Tomcat 스레드 보호
- 필요 시 AI 제공자 교체 가능 (의존성 주입으로 변경)

---

## 2. AI Client 인터페이스 및 구현

### 2.1 인터페이스 정의

```java
package org.example.sharedprompts.infra.ai;

// 텍스트 생성용 AI Client 인터페이스
public interface TextAiClient {
    String generateText(String prompt, Map<String, Object> options);
}

// 이미지 생성용 AI Client 인터페이스
public interface ImageAiClient {
    String generateImage(String prompt, Map<String, Object> options);
}
```

### 2.2 텍스트 생성 AI 구현 예시

#### 오픈소스 모델 (로컬 실행 또는 API 서버)

```java
@Component
@RequiredArgsConstructor
public class LlamaTextAiClient implements TextAiClient {
    
    private final LlamaModel llamaModel; // 로컬 모델 또는 API 클라이언트
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        // LLaMA 모델 호출 (로컬 실행 또는 API)
        return llamaModel.generate(prompt, options);
    }
}

@Component
@RequiredArgsConstructor
public class VicunaTextAiClient implements TextAiClient {
    
    private final VicunaModel vicunaModel;
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        // Vicuna 모델 호출
        return vicunaModel.generate(prompt, options);
    }
}
```

#### 클라우드 API

```java
@Component
@RequiredArgsConstructor
public class GeminiTextAiClient implements TextAiClient {
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        // Google Gemini API 호출
        // CircuitBreaker, Retry, Timeout 적용
        return webClient.post()
            .uri("/models/{model}:generateContent", options.get("model"))
            .bodyValue(buildRequest(prompt))
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .map(response -> extractContent(response))
            .block();
    }
}
```

### 2.3 이미지 생성 AI 구현 예시

#### 오픈소스 모델

```java
@Component
@RequiredArgsConstructor
public class StableDiffusionImageAiClient implements ImageAiClient {
    
    private final StableDiffusionModel stableDiffusionModel; // 로컬 모델 또는 API 클라이언트
    
    @Override
    public String generateImage(String prompt, Map<String, Object> options) {
        // Stable Diffusion 모델 호출
        // 이미지 생성 후 파일 경로 반환
        return stableDiffusionModel.generate(prompt, options);
    }
}

@Component
@RequiredArgsConstructor
public class DreamShaperImageAiClient implements ImageAiClient {
    
    private final DreamShaperModel dreamShaperModel;
    
    @Override
    public String generateImage(String prompt, Map<String, Object> options) {
        // DreamShaper 모델 호출 (고품질 이미지)
        return dreamShaperModel.generate(prompt, options);
    }
}

@Component
@RequiredArgsConstructor
public class OpenJourneyImageAiClient implements ImageAiClient {
    
    private final OpenJourneyModel openJourneyModel;
    
    @Override
    public String generateImage(String prompt, Map<String, Object> options) {
        // OpenJourney 모델 호출
        return openJourneyModel.generate(prompt, options);
    }
}

@Component
@RequiredArgsConstructor
public class WaifuDiffusionImageAiClient implements ImageAiClient {
    
    private final WaifuDiffusionModel waifuDiffusionModel;
    
    @Override
    public String generateImage(String prompt, Map<String, Object> options) {
        // Waifu Diffusion 모델 호출
        return waifuDiffusionModel.generate(prompt, options);
    }
}
```

#### 클라우드 API

```java
@Component
@RequiredArgsConstructor
public class DalleImageAiClient implements ImageAiClient {
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public String generateImage(String prompt, Map<String, Object> options) {
        // DALL-E API 호출
        return webClient.post()
            .uri("/v1/images/generations")
            .bodyValue(buildImageRequest(prompt, options))
            .retrieve()
            .bodyToMono(ImageResponse.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .map(response -> saveImageAndReturnPath(response))
            .block();
    }
}
```

### 2.4 Infra 계층의 Composer 예시

```java
@Component
@RequiredArgsConstructor
public class BlogComposer {
    
    // 텍스트 생성용 AI Client 주입
    private final TextAiClient textAiClient;
    
    public String compose(String title, String promptContent, List<String> tags, UserInputDto userInput) {
        // 프롬프트 결과와 사용자 입력값을 조합하여 텍스트 생성 AI에 전달
        String combinedPrompt = buildPrompt(title, promptContent, tags, userInput);
        return textAiClient.generateText(combinedPrompt, buildOptions(userInput));
    }
}

@Component
@RequiredArgsConstructor
public class ImageGenerator {
    
    // 이미지 생성용 AI Client 주입
    private final ImageAiClient imageAiClient;
    
    public String generate(String prompt, String style, String size, UserInputDto userInput) {
        // 이미지 생성 AI에 전달
        Map<String, Object> options = Map.of(
            "style", style,
            "size", size,
            "userInput", userInput
        );
        return imageAiClient.generateImage(prompt, options);
    }
}
```

---

## 3. AI 모델별 특징 및 권장 사용처

### 3.1 텍스트 생성 AI 모델 비교

| 모델 | 타입 | 라이선스 | 비용 (월 10K 요청 기준) | 추천도 | 권장 사용처 | 특징 |
|------|------|---------|----------------------|--------|-----------|------|
| **LLaMA 7B/13B** | 오픈소스 | 커뮤니티 | **$50-200** (EC2 인프라) | ⭐⭐⭐⭐⭐ | 로컬 실행, 비용 절감 | 높은 성능, 다양한 크기 |
| **Vicuna** | 오픈소스 | Apache-2.0 | **$50-200** (EC2 인프라) | ⭐⭐⭐⭐ | 대화형 콘텐츠 | LLaMA 기반, 대화 최적화 |
| **BLOOM** | 오픈소스 | RAIL | **$100-300** (EC2 인프라) | ⭐⭐⭐ | 다국어 콘텐츠 | 46개 언어 지원 |
| **Falcon** | 오픈소스 | Apache-2.0 | **$40-150** (EC2 인프라) | ⭐⭐⭐⭐ | 경량 요구사항 | 경량 모델, 상업용 친화적 |
| **MPT** | 오픈소스 | Apache-2.0 | **$40-150** (EC2 인프라) | ⭐⭐⭐ | 빠른 추론 필요 | 경량, 빠른 추론 |
| **Google Gemini Pro** | 클라우드 API | 사용량 기반 | **$50-100** (API 호출) | ⭐⭐⭐⭐⭐ | 실시간 생성, 고품질 | API 기반, CircuitBreaker 적용 |
| **OpenAI GPT-4** | 클라우드 API | 사용량 기반 | **$300-600** (API 호출) | ⭐⭐⭐⭐⭐ | 최고 품질 필요 | 최고 성능, 다양한 모델 |
| **OpenAI GPT-3.5 Turbo** | 클라우드 API | 사용량 기반 | **$20-50** (API 호출) | ⭐⭐⭐⭐ | 실시간 생성, 비용 효율 | API 기반, 빠른 응답 |
| **Anthropic Claude 3** | 클라우드 API | 사용량 기반 | **$150-300** (API 호출) | ⭐⭐⭐⭐⭐ | 긴 콘텐츠 생성 | 긴 컨텍스트 지원 (200K 토큰) |

**비용 상세:**
- **오픈소스 모델**: EC2 인프라 비용 (인스턴스 타입, 사용 시간에 따라 변동)
  - LLaMA 7B: g4dn.xlarge (약 $0.526/시간) → 월 100시간 사용 시 약 $52.6
  - LLaMA 13B: g4dn.2xlarge (약 $0.752/시간) → 월 200시간 사용 시 약 $150.4
- **클라우드 API**: 토큰 기반 과금 (입력/출력 토큰 수에 따라 변동)
  - GPT-4: 입력 $30/1M 토큰, 출력 $60/1M 토큰
  - GPT-3.5 Turbo: 입력 $0.50/1M 토큰, 출력 $1.50/1M 토큰
  - Gemini Pro: 입력 $0.50/1M 토큰, 출력 $1.50/1M 토큰
  - Claude 3: 입력 $15/1M 토큰, 출력 $75/1M 토큰

**추천도 기준:**
- ⭐⭐⭐⭐⭐ (5점): 최고 추천 - 성능/비용/사용성 균형 우수
- ⭐⭐⭐⭐ (4점): 추천 - 특정 용도에 최적화
- ⭐⭐⭐ (3점): 보통 - 특수한 요구사항에 적합

**선택 가이드:**
- **비용 절감 + 로컬 실행**: LLaMA 7B (⭐⭐⭐⭐⭐), Falcon (⭐⭐⭐⭐)
- **고품질 + 실시간**: Google Gemini Pro (⭐⭐⭐⭐⭐), GPT-3.5 Turbo (⭐⭐⭐⭐)
- **최고 품질**: GPT-4 (⭐⭐⭐⭐⭐), Claude 3 (⭐⭐⭐⭐⭐)
- **다국어 지원**: BLOOM (⭐⭐⭐)
- **대화형 콘텐츠**: Vicuna (⭐⭐⭐⭐)
- **긴 문서 처리**: Claude 3 (⭐⭐⭐⭐⭐) - 200K 토큰 컨텍스트

**사용 사례별 추천:**

| 사용 사례 | 1순위 추천 | 2순위 추천 | 비고 |
|----------|----------|----------|------|
| **일반 블로그/이메일 작성** | GPT-3.5 Turbo | Gemini Pro | 비용 효율적, 빠른 응답 |
| **고품질 콘텐츠 생성** | GPT-4 | Claude 3 | 최고 품질 필요 시 |
| **대량 생성 (비용 중요)** | LLaMA 7B | Falcon | 오픈소스, 인프라 비용만 |
| **다국어 콘텐츠** | BLOOM | GPT-3.5 Turbo | 46개 언어 지원 |
| **긴 문서 요약/생성** | Claude 3 | GPT-4 | 200K 토큰 컨텍스트 |
| **대화형 챗봇** | Vicuna | GPT-3.5 Turbo | 대화 최적화 |
| **실시간 API 통합** | Gemini Pro | GPT-3.5 Turbo | 빠른 응답, 안정적 |

### 3.2 이미지 생성 AI 모델 비교

| 모델 | 타입 | 라이선스 | 비용 (월 1K 이미지 기준) | 추천도 | 권장 사용처 | 특징 |
|------|------|---------|------------------------|--------|-----------|------|
| **Stable Diffusion 1.5** | 오픈소스 | 커뮤니티 | **$30-100** (EC2 인프라) | ⭐⭐⭐⭐⭐ | 범용 이미지 생성 | 오픈소스, 커스터마이징 가능 |
| **DreamShaper** | 오픈소스 | 커뮤니티 | **$30-100** (EC2 인프라) | ⭐⭐⭐⭐ | 고품질 이미지 | Stable Diffusion 기반, 고품질 |
| **OpenJourney** | 오픈소스 | 커뮤니티 | **$30-100** (EC2 인프라) | ⭐⭐⭐ | 여행 관련 이미지 | 여행 이미지 최적화 |
| **Waifu Diffusion** | 오픈소스 | 커뮤니티 | **$30-100** (EC2 인프라) | ⭐⭐⭐ | 애니메이션 스타일 | 애니메이션 스타일 전용 |
| **DALL-E 3** | 클라우드 API | 사용량 기반 | **$40-80** (API 호출) | ⭐⭐⭐⭐⭐ | 실시간 생성, 고품질 | OpenAI API, 최고 품질 |
| **DALL-E 2** | 클라우드 API | 사용량 기반 | **$20-40** (API 호출) | ⭐⭐⭐⭐ | 실시간 생성, 비용 효율 | OpenAI API, 빠른 생성 |
| **Ideogram** | 클라우드 API | 사용량 기반 | **$30-60** (API 호출) | ⭐⭐⭐⭐ | 텍스트 포함 이미지 | 텍스트 렌더링 우수 |
| **Midjourney** | 클라우드 API | 구독 기반 | **$10-60/월** (구독) | ⭐⭐⭐⭐⭐ | 아트워크, 고품질 | 최고 품질 아트 생성 |

**비용 상세:**
- **오픈소스 모델**: EC2 인프라 비용 (GPU 인스턴스 필요)
  - g4dn.xlarge (NVIDIA T4): 약 $0.526/시간
  - 이미지 생성 시간: 약 5-10초/이미지
  - 월 1K 이미지 생성 시: 약 1.5-3시간 → $0.8-1.6
  - 인스턴스 유지 비용 (24/7): 약 $380/월
  - **실제 비용**: 인스턴스 유지 시 $380/월, 필요 시에만 실행 시 $30-100/월
- **클라우드 API**: 이미지당 과금
  - DALL-E 3: $0.040/이미지 (1024x1024) → 1K 이미지 = $40
  - DALL-E 2: $0.020/이미지 (1024x1024) → 1K 이미지 = $20
  - Ideogram: $0.030/이미지 → 1K 이미지 = $30
  - Midjourney: 구독 기반 ($10/월 ~ $60/월)

**추천도 기준:**
- ⭐⭐⭐⭐⭐ (5점): 최고 추천 - 성능/비용/사용성 균형 우수
- ⭐⭐⭐⭐ (4점): 추천 - 특정 용도에 최적화
- ⭐⭐⭐ (3점): 보통 - 특수한 요구사항에 적합

**선택 가이드:**
- **비용 절감 + 로컬 실행**: Stable Diffusion 1.5 (⭐⭐⭐⭐⭐) - 가장 범용적
- **고품질 + 실시간**: DALL-E 3 (⭐⭐⭐⭐⭐), Midjourney (⭐⭐⭐⭐⭐)
- **비용 효율 + 실시간**: DALL-E 2 (⭐⭐⭐⭐)
- **애니메이션 스타일**: Waifu Diffusion (⭐⭐⭐)
- **텍스트 포함 이미지**: Ideogram (⭐⭐⭐⭐)
- **아트워크/창의적 이미지**: Midjourney (⭐⭐⭐⭐⭐)

**사용 사례별 추천:**

| 사용 사례 | 1순위 추천 | 2순위 추천 | 비고 |
|----------|----------|----------|------|
| **일반 이미지 생성** | Stable Diffusion 1.5 | DALL-E 2 | 범용, 비용 효율 |
| **고품질 이미지** | DALL-E 3 | Midjourney | 최고 품질 필요 시 |
| **대량 생성 (비용 중요)** | Stable Diffusion 1.5 | DreamShaper | 오픈소스, 인프라 비용만 |
| **텍스트 포함 이미지** | Ideogram | DALL-E 3 | 텍스트 렌더링 우수 |
| **아트워크/창의적** | Midjourney | DALL-E 3 | 예술적 품질 최고 |
| **애니메이션 스타일** | Waifu Diffusion | Stable Diffusion 1.5 | 애니메이션 전용 |
| **여행/랜드스케이프** | OpenJourney | Stable Diffusion 1.5 | 여행 이미지 최적화 |

---

## 4. AWS 클라우드 환경 배포

### 4.1 인프라 구성 옵션

| 구성 방식 | 사용 사례 | 장점 | 단점 |
|----------|---------|------|------|
| **AWS SageMaker** | 프로덕션 환경, 관리형 서비스 | 자동 스케일링, 모델 버전 관리, 모니터링 | 비용이 높음, 벤더 종속 |
| **ECS/Fargate + 자체 API 서버** | 오픈소스 모델, 비용 최적화 | 유연성, 비용 절감, 모델 제어 | 운영 부담, 스케일링 수동 관리 |
| **EC2 + 자체 서버** | 개발/테스트, 소규모 운영 | 완전한 제어, 낮은 비용 | 인프라 관리 필요 |
| **Lambda + API Gateway** | 간헐적 사용, 서버리스 | 비용 효율적, 자동 스케일링 | 콜드 스타트, 실행 시간 제한 |
| **EKS (Kubernetes)** | 대규모 운영, 마이크로서비스 | 확장성, 오케스트레이션 | 복잡도 높음 |

### 4.2 권장 아키텍처: 하이브리드 구성

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  (Spring Boot Application - ECS/Fargate 또는 EC2)      │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Production Module (Domain Layer)          │  │
│  │  BlogComposer, EmailComposer, ImageGenerator 등   │  │
│  └──────────────────┬───────────────────────────────┘  │
│                     │                                    │
│  ┌──────────────────▼───────────────────────────────┐  │
│  │         AI Client (Infra/Adapter Layer)          │  │
│  │  TextAiClient, ImageAiClient 인터페이스          │  │
│  └──────────────────┬───────────────────────────────┘  │
└─────────────────────┼──────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  오픈소스 모델  │ │  AWS SageMaker │ │  클라우드 API  │
│  (ECS/Fargate)│ │  (관리형 서비스) │ │ (OpenAI 등)   │
│              │ │              │ │              │
│ - LLaMA      │ │ - 커스텀 모델 │ │ - GPT-4      │
│ - Vicuna     │ │ - 자동 스케일링│ │ - Claude     │
│ - Stable     │ │              │ │ - Gemini     │
│   Diffusion  │ │              │ │              │
└──────────────┘ └──────────────┘ └──────────────┘
```

### 4.3 시나리오 1: 오픈소스 모델 자체 서빙 (ECS/Fargate)

**구성:**
- **Application**: ECS/Fargate에서 Spring Boot 실행
- **AI Model Server**: 별도 ECS Task에서 모델 서빙 (vLLM, TGI 등)
- **네트워크**: Application Load Balancer + 내부 네트워크 통신

**예시 코드:**

```java
package org.example.sharedprompts.infra.ai;

// AWS 환경에서 오픈소스 모델 API 서버 호출
@Component
@RequiredArgsConstructor
public class LlamaTextAiClient implements TextAiClient {
    
    // ECS/Fargate에서 실행 중인 모델 서버 URL
    // 환경 변수 또는 AWS Systems Manager Parameter Store에서 주입
    @Value("${ai.model.llama.api-url:http://llama-model-service:8000}")
    private final String modelApiUrl;
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        // 내부 네트워크를 통한 모델 서버 호출
        return webClient.post()
            .uri(modelApiUrl + "/v1/completions")
            .bodyValue(buildLlamaRequest(prompt, options))
            .retrieve()
            .bodyToMono(LlamaResponse.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .timeout(Duration.ofSeconds(60)) // 타임아웃 설정
            .retry(3) // 재시도
            .map(response -> extractContent(response))
            .block();
    }
}

// 이미지 생성 모델 서버 호출
@Component
@RequiredArgsConstructor
public class StableDiffusionImageAiClient implements ImageAiClient {
    
    @Value("${ai.model.stable-diffusion.api-url:http://sd-model-service:7860}")
    private final String modelApiUrl;
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final S3Client s3Client; // 생성된 이미지 저장용
    
    @Override
    public String generateImage(String prompt, Map<String, Object> options) {
        // Stable Diffusion API 서버 호출
        ImageResponse response = webClient.post()
            .uri(modelApiUrl + "/api/v1/txt2img")
            .bodyValue(buildStableDiffusionRequest(prompt, options))
            .retrieve()
            .bodyToMono(ImageResponse.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .timeout(Duration.ofMinutes(5)) // 이미지 생성은 시간이 오래 걸림
            .block();
        
        // 생성된 이미지를 S3에 저장
        String s3Key = "generated-images/" + UUID.randomUUID() + ".png";
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket("ai-generated-images")
                .key(s3Key)
                .build(),
            RequestBody.fromBytes(response.getImageBytes())
        );
        
        // S3 URL 반환
        return "s3://ai-generated-images/" + s3Key;
    }
}
```

**ECS Task Definition 예시:**

```json
{
  "family": "llama-model-server",
  "networkMode": "awsvpc",
  "containerDefinitions": [
    {
      "name": "llama-model",
      "image": "vllm/vllm-openai:latest",
      "memory": 16384,
      "cpu": 4096,
      "environment": [
        {
          "name": "MODEL_NAME",
          "value": "meta-llama/Llama-2-7b-chat-hf"
        }
      ],
      "portMappings": [
        {
          "containerPort": 8000,
          "protocol": "tcp"
        }
      ]
    }
  ]
}
```

### 4.4 시나리오 2: AWS SageMaker 통합

**구성:**
- **Application**: ECS/Fargate에서 Spring Boot 실행
- **AI Model**: SageMaker Endpoint에서 모델 서빙
- **인증**: IAM Role 기반 인증

**예시 코드:**

```java
package org.example.sharedprompts.infra.ai;

import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;

@Component
@RequiredArgsConstructor
public class SageMakerTextAiClient implements TextAiClient {
    
    @Value("${ai.model.sagemaker.endpoint-name:llama-7b-endpoint}")
    private final String endpointName;
    
    private final SageMakerRuntimeClient sagemakerClient;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        // SageMaker Endpoint 호출
        InvokeEndpointRequest request = InvokeEndpointRequest.builder()
            .endpointName(endpointName)
            .contentType("application/json")
            .body(SdkBytes.fromUtf8String(buildSageMakerRequest(prompt, options)))
            .build();
        
        try {
            InvokeEndpointResponse response = circuitBreaker.executeSupplier(() ->
                sagemakerClient.invokeEndpoint(request)
            );
            
            return parseSageMakerResponse(response.body().asUtf8String());
        } catch (Exception e) {
            throw new ProductionException("SageMaker inference failed", e);
        }
    }
}
```

### 4.5 시나리오 3: 하이브리드 구성 (오픈소스 + 클라우드 API)

**전략:**
- **일반 요청**: 오픈소스 모델 (비용 절감)
- **고품질 요청**: 클라우드 API (OpenAI, Anthropic 등)
- **Fallback**: 클라우드 API 장애 시 오픈소스 모델로 전환

**예시 코드:**

```java
@Component
@RequiredArgsConstructor
public class HybridTextAiClient implements TextAiClient {
    
    private final LlamaTextAiClient llamaClient; // 오픈소스 모델
    private final OpenAiTextClient openAiClient; // 클라우드 API
    private final AiClientSelector clientSelector; // 요청별 클라이언트 선택
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        AiClientType preferredType = clientSelector.selectClient(options);
        
        try {
            if (preferredType == AiClientType.OPEN_SOURCE) {
                return llamaClient.generateText(prompt, options);
            } else {
                return openAiClient.generateText(prompt, options);
            }
        } catch (Exception e) {
            // Fallback: 클라우드 API 실패 시 오픈소스 모델로 전환
            if (preferredType == AiClientType.CLOUD_API) {
                log.warn("Cloud API failed, falling back to open source model", e);
                return llamaClient.generateText(prompt, options);
            }
            throw e;
        }
    }
}

@Component
public class AiClientSelector {
    
    public AiClientType selectClient(Map<String, Object> options) {
        // 사용자 요청 품질 레벨에 따라 선택
        String quality = (String) options.getOrDefault("quality", "standard");
        
        if ("high".equals(quality) || "premium".equals(quality)) {
            return AiClientType.CLOUD_API;
        }
        
        // 비용 절감을 위해 기본적으로 오픈소스 모델 사용
        return AiClientType.OPEN_SOURCE;
    }
}
```

### 4.6 AWS 환경별 설정 관리

**환경 변수 및 시크릿 관리:**

```java
@Configuration
public class AiClientConfiguration {
    
    // AWS Systems Manager Parameter Store에서 설정 조회
    @Bean
    public TextAiClient textAiClient(
            @Value("${ai.text.provider:llama}") String provider,
            ParameterStoreClient parameterStore
    ) {
        String apiUrl = parameterStore.getParameter("/ai/model/" + provider + "/api-url");
        String apiKey = parameterStore.getSecureParameter("/ai/model/" + provider + "/api-key");
        
        switch (provider) {
            case "llama":
                return new LlamaTextAiClient(apiUrl, webClient, circuitBreaker);
            case "openai":
                return new OpenAiTextClient(apiKey, webClient, circuitBreaker);
            case "sagemaker":
                return new SageMakerTextAiClient(endpointName, sagemakerClient, circuitBreaker);
            default:
                throw new IllegalArgumentException("Unknown AI provider: " + provider);
        }
    }
}
```

### 4.7 네트워크 및 보안 구성

**VPC 구성:**
- **Public Subnet**: Application Load Balancer, NAT Gateway
- **Private Subnet**: Application 서버, AI 모델 서버
- **Security Group**: 최소 권한 원칙 적용

**보안 그룹 규칙 예시:**
- Application → AI Model Server: 포트 8000 (HTTP)
- ALB → Application: 포트 8080 (HTTP)
- AI Model Server → S3: HTTPS (이미지 저장)

### 4.8 비용 최적화 전략

| 전략 | 설명 | 예상 절감률 | 월 예상 절감액 (예시) |
|------|------|-----------|-------------------|
| **Spot Instance 사용** | 개발/테스트 환경에서 Spot Instance 활용 | 70-90% | $300 → $30-90 |
| **Auto Scaling** | 트래픽에 따라 자동 스케일링 | 30-50% | $500 → $250-350 |
| **Reserved Instance** | 프로덕션 환경에서 RI 활용 | 30-40% | $1000 → $600-700 |
| **하이브리드 구성** | 일반 요청은 오픈소스, 고품질만 클라우드 API | 50-70% | $1000 → $300-500 |
| **S3 Intelligent-Tiering** | 생성된 이미지 자동 스토리지 최적화 | 10-20% | $100 → $80-90 |

**비용 비교 시나리오 (월 10K 텍스트 생성 요청 기준):**

| 구성 방식 | 월 예상 비용 | 특징 |
|----------|------------|------|
| **100% 클라우드 API (GPT-4)** | $300-600 | 최고 품질, 높은 비용 |
| **100% 클라우드 API (GPT-3.5 Turbo)** | $20-50 | 좋은 품질, 합리적 비용 |
| **100% 오픈소스 (LLaMA 7B, 24/7)** | $380 | 낮은 비용, 인프라 관리 필요 |
| **하이브리드 (90% LLaMA + 10% GPT-4)** | $100-150 | 균형잡힌 품질/비용 |
| **하이브리드 (80% LLaMA + 20% GPT-3.5)** | $60-90 | 최적 비용 효율 |

**비용 비교 시나리오 (월 1K 이미지 생성 기준):**

| 구성 방식 | 월 예상 비용 | 특징 |
|----------|------------|------|
| **100% 클라우드 API (DALL-E 3)** | $40-80 | 최고 품질, 빠른 생성 |
| **100% 클라우드 API (DALL-E 2)** | $20-40 | 좋은 품질, 합리적 비용 |
| **100% 오픈소스 (Stable Diffusion, 24/7)** | $380 | 낮은 비용, 인프라 관리 필요 |
| **하이브리드 (90% Stable Diffusion + 10% DALL-E 3)** | $80-120 | 균형잡힌 품질/비용 |
| **하이브리드 (80% Stable Diffusion + 20% DALL-E 2)** | $100-140 | 최적 비용 효율 |

**추천 전략:**
1. **소규모 운영 (월 1K-5K 요청)**: 클라우드 API (GPT-3.5 Turbo, DALL-E 2) - 운영 부담 최소화
2. **중규모 운영 (월 10K-50K 요청)**: 하이브리드 구성 - 비용 절감 + 품질 유지
3. **대규모 운영 (월 100K+ 요청)**: 오픈소스 모델 + 필요 시 클라우드 API - 최대 비용 절감

### 4.9 모니터링 및 로깅

```java
@Component
@RequiredArgsConstructor
public class MonitoredTextAiClient implements TextAiClient {
    
    private final TextAiClient delegate;
    private final CloudWatchMetricsClient metricsClient;
    private final CloudWatchLogsClient logsClient;
    
    @Override
    public String generateText(String prompt, Map<String, Object> options) {
        long startTime = System.currentTimeMillis();
        
        try {
            String result = delegate.generateText(prompt, options);
            
            // 성공 메트릭 기록
            metricsClient.putMetricData(
                MetricData.builder()
                    .metricName("AiGenerationSuccess")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .build()
            );
            
            // 응답 시간 기록
            long duration = System.currentTimeMillis() - startTime;
            metricsClient.putMetricData(
                MetricData.builder()
                    .metricName("AiGenerationLatency")
                    .value((double) duration)
                    .unit(StandardUnit.MILLISECONDS)
                    .build()
            );
            
            return result;
        } catch (Exception e) {
            // 실패 메트릭 기록
            metricsClient.putMetricData(
                MetricData.builder()
                    .metricName("AiGenerationFailure")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .build()
            );
            
            // 에러 로그 기록
            logsClient.putLogEvents(
                PutLogEventsRequest.builder()
                    .logGroupName("/aws/ai/generation")
                    .logStreamName("errors")
                    .logEvents(LogEvent.builder()
                        .message("AI generation failed: " + e.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build())
                    .build()
            );
            
            throw e;
        }
    }
}
```

### 4.10 배포 파이프라인

**CI/CD 구성:**
1. **CodeBuild**: 애플리케이션 빌드 및 Docker 이미지 생성
2. **ECR**: Docker 이미지 저장소
3. **ECS**: 자동 배포 및 롤링 업데이트
4. **CloudFormation/CDK**: 인프라 코드화

**환경별 배포 전략:**
- **개발**: 수동 배포, Spot Instance
- **스테이징**: 자동 배포, Reserved Instance
- **프로덕션**: Blue/Green 배포, Multi-AZ 구성

