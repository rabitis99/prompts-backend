package org.example.sharedprompts.github.adapter.out.client;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.text.TextAiClient;
import org.example.sharedprompts.github.port.out.BodyGenerationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * TextAiClient 기반 BodyGeneration 포트 구현.
 * AI가 없거나 실패하면 Optional.empty() 반환 (template fallback으로 처리).
 */
@Component
@Slf4j
public class TextAiClientAdapter implements BodyGenerationPort {

  private final TextAiClient textAiClient;

  public TextAiClientAdapter(
      @Autowired(required = false) @Nullable TextAiClient textAiClient) {
    this.textAiClient = textAiClient;
  }

  @Override
  public Optional<String> generateBody(String systemPrompt, String userPrompt) {
    if (systemPrompt == null || userPrompt == null) {
      throw new IllegalArgumentException("prompts cannot be null");
    }

    if (textAiClient == null) {
      log.debug("TextAiClient not available, falling back to template");
      return Optional.empty();
    }

    try {
      String combined = systemPrompt + "\n\n---\n\n" + userPrompt;
      String generated = textAiClient.generateText(combined, null, "markdown");
      if (generated != null && !generated.isBlank()) {
        log.debug("GitHub body generated via AI");
        return Optional.of(generated.trim());
      }
      log.debug("AI returned empty, falling back to template");
      return Optional.empty();
    } catch (Exception e) {
      log.warn("AI generation failed, falling back to template: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
