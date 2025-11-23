package org.example.sharedprompts.global.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.google.gemini.GoogleGeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class GoogleGeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeminiConfig.class);

    private final GoogleGeminiProperties properties;

    @Bean
    public WebClient googleGeminiWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    // 요청 로깅
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("[GoogleGemini] Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((k,v) -> {
                if (!k.equalsIgnoreCase("Authorization") && !k.equalsIgnoreCase("X-API-Key")) {
                    log.info("{}={}", k, v);
                }
            });
            return Mono.just(clientRequest);
        });
    }

    // 응답 로깅
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("[GoogleGemini] Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
