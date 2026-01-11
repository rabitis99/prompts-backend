package org.example.sharedprompts.global.google.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "google.gemini")
public class GoogleGeminiProperties {
    private String apiKey;
    private String model = "gemini-2.5-flash-lite";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1";
    private int timeoutSeconds = 30;
}
