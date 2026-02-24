package org.example.sharedprompts.github.port.out;

import org.example.sharedprompts.github.adapter.out.client.TextAiClientAdapter;

import java.util.Optional;

/**
 * GitHub 본문 AI 생성 포트.
 *
 * 구현 책임:
 * - {@link TextAiClientAdapter}
 *
 * LSP 계약:
 * - AI 미사용 (client 미주입/disabled): Optional.empty() 반환
 * - AI 호출 실패: Optional.empty() 반환 (로깅, 재시도 시 template fallback)
 * - AI 호출 성공: Optional.of(생성된 텍스트) 반환
 */
public interface BodyGenerationPort {

  /**
   * AI로 마크다운 본문 생성 (System + User prompt).
   *
   * @param systemPrompt AI 시스템 프롬프트 (역할/제약 정의)
   * @param userPrompt AI 사용자 프롬프트 (입력 데이터)
   * @return 생성된 마크다운 또는 empty (AI 미사용 또는 실패)
   * @throws IllegalArgumentException prompt null
   */
  Optional<String> generateBody(String systemPrompt, String userPrompt);
}
