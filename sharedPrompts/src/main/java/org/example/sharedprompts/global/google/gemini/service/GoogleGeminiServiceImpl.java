package org.example.sharedprompts.global.google.gemini.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import org.example.sharedprompts.global.google.gemini.GoogleGeminiProperties;
import org.example.sharedprompts.global.google.gemini.extractor.GeminiResponseExtractor;
import org.example.sharedprompts.global.google.gemini.fallback.GeminiFallbackHandler;
import org.example.sharedprompts.global.google.gemini.metrics.GeminiMetricsRecorder;
import org.example.sharedprompts.global.google.gemini.request.GeminiRequest;
import org.example.sharedprompts.global.google.gemini.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GoogleGeminiServiceImpl implements GoogleGeminiService {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeminiServiceImpl.class);

    private final WebClient webClient;
    private final GoogleGeminiProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;
    private final GeminiResponseExtractor responseExtractor;
    private final GeminiFallbackHandler fallbackHandler;
    private final GeminiMetricsRecorder metricsRecorder;

    @Override
    public Mono<String> chat(String prompt) {
        GeminiRequest request = GeminiRequest.fromUserPrompt(prompt);
        Timer.Sample sample = Timer.start(meterRegistry);

        Mono<String> chatCall = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .build(properties.getModel()))
                .header("x-goog-api-key", properties.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(responseExtractor::extractFirstCandidate)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(e -> {
                            // 5xx 서버 오류 및 네트워크 오류만 재시도
                            if (e instanceof WebClientResponseException wcre) {
                                if (wcre.getStatusCode().is4xxClientError()) {
                                    log.error("Client error, not retrying: {}", e.getMessage());
                                    return false;
                                }
                            }
                            log.warn("Retry due to: {}", e.getMessage());
                            return true;
                        }))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .doOnError(e -> log.error("GoogleGemini API error", e));

        // CircuitBreaker 적용 (timeout/retry 이후)
        Mono<String> protectedCall = chatCall.transformDeferred(
                CircuitBreakerOperator.of(circuitBreaker)
        );

        // Fallback 적용: 모든 에러와 빈 응답에 대해 Fallback 반환
        return protectedCall
                .onErrorResume(e -> {
                    log.warn("AI 호출 실패, fallback 사용. Error: {}", e.getClass().getSimpleName(), e);
                    String fallbackMessage = fallbackHandler.createFallbackMessage(prompt);
                    metricsRecorder.recordMetrics(sample, "fallback", e.getClass().getSimpleName());
                    return Mono.just(fallbackMessage);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("AI 응답이 비어있음, fallback 사용");
                    String fallbackMessage = fallbackHandler.createFallbackMessage(prompt);
                    metricsRecorder.recordMetrics(sample, "fallback", "EmptyResponse");
                    return Mono.just(fallbackMessage);
                }))
                .doOnSuccess(result -> {
                    // Fallback이 아닌 정상 응답인 경우에만 메트릭 기록
                    // (Fallback인 경우는 이미 onErrorResume/switchIfEmpty에서 기록됨)
                    if (!fallbackHandler.isFallbackMessage(result)) {
                        metricsRecorder.recordMetrics(sample, "success", null);
                    }
                });
    }

}

