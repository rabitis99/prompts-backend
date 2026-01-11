package org.example.sharedprompts.global.google.gemini;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.google.gemini.request.GeminiRequest;
import org.example.sharedprompts.global.google.gemini.response.Candidate;
import org.example.sharedprompts.global.google.gemini.response.ChatResponse;
import org.example.sharedprompts.global.google.gemini.response.Content;
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

    @Override
    public Mono<String> chat(String prompt) {
        GeminiRequest request = GeminiRequest.fromUserPrompt(prompt);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .build(properties.getModel()))
                .header("x-goog-api-key", properties.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(this::extractFirstCandidate)
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
    }

    private String extractFirstCandidate(ChatResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            log.warn("No candidates returned from Gemini API");
            return "";
        }

        Candidate candidate = response.getCandidates().get(0);
        Content content = candidate.getContent();

        if (content == null || content.getParts() == null || content.getParts().isEmpty()) {
            log.warn("No content parts returned from Gemini API");
            return "";
        }

        return content.getParts().get(0).getText();
    }

}

