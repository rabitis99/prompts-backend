package org.example.sharedprompts.global.google.gemini;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.google.gemini.request.ChatRequest;
import org.example.sharedprompts.global.google.gemini.request.Message;
import org.example.sharedprompts.global.google.gemini.response.ChatResponse;
import org.example.sharedprompts.global.google.gemini.response.Candidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleGeminiService {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeminiService.class);

    private final WebClient webClient;
    private final GoogleGeminiProperties properties;

    public Mono<String> chat(String prompt) {
        ChatRequest request = new ChatRequest(new Message(prompt));

        return webClient.post()
                .uri("/models/{model}:generateMessage", properties.getModel())
                .header("x-goog-api-key", properties.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(this::extractFirstCandidate)
                .timeout(Duration.ofSeconds(5)) // 응답 대기 5초
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 최대 3회 재시도
                        .filter(e -> {
                            log.warn("Retry due to: {}", e.getMessage());
                            return true;
                        }))
                .doOnError(e -> log.error("GoogleGemini API error", e));
    }

    private String extractFirstCandidate(ChatResponse response) {
        List<Candidate> candidates = response.getCandidates();
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0).getContent();
        } else {
            log.warn("No candidates returned from GoogleGemini API");
            return "";
        }
    }
}
