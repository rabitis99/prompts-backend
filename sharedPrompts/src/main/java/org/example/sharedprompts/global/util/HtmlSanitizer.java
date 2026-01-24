package org.example.sharedprompts.global.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * OWASP Java HTML Sanitizer 기반 XSS 방지 유틸리티.
 *
 * - 프롬프트 제목/내용, 댓글, 사용자 프로필 등 사용자 입력 문자열을 정제합니다.
 * - 기본적으로 텍스트 위주의 포맷팅/링크 정도만 허용하고 스크립트는 모두 제거합니다.
 */
@Component
public class HtmlSanitizer {

    /**
     * 기본 정책:
     * - FORMATTING: <b>, <i>, <strong>, <em> 등 텍스트 포맷팅
     * - BLOCKS: <p>, <div>, <ul>, <ol>, <li> 등 블록 요소
     * - LINKS: <a href> 링크 (javascript: 등 위험한 프로토콜은 라이브러리가 기본 차단)
     */
    private static final PolicyFactory POLICY =
            Sanitizers.FORMATTING
                    .and(Sanitizers.BLOCKS)
                    .and(Sanitizers.LINKS);

    /**
     * null-safe sanitize
     *
     * @param input 사용자 입력 문자열
     * @return 정제된 문자열 (input이 null이면 null 반환)
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return POLICY.sanitize(input);
    }
}


