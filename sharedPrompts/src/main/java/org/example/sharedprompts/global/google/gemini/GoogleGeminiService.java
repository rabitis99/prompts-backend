package org.example.sharedprompts.global.google.gemini;

import reactor.core.publisher.Mono;

public interface GoogleGeminiService {
    
    Mono<String> chat(String prompt);
}
