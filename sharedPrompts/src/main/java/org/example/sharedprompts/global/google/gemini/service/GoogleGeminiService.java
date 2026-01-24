package org.example.sharedprompts.global.google.gemini.service;

import reactor.core.publisher.Mono;

public interface GoogleGeminiService {
    
    Mono<String> chat(String prompt);
}
