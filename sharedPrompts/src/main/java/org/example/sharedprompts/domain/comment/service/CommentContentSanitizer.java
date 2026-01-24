package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.util.HtmlSanitizer;
import org.springframework.stereotype.Component;

/**
 * 댓글 콘텐츠 Sanitization 전용 컴포넌트
 * 
 * 현재는 HtmlSanitizer.sanitize()를 단순 위임하는 thin wrapper 패턴입니다.
 * 향후 댓글 전용 sanitization 규칙(예: 최대 길이 제한, 특정 태그 허용 등)을
 * 추가할 수 있도록 확장 포인트로 설계되었습니다.
 */
@Component
@RequiredArgsConstructor
public class CommentContentSanitizer {

    private final HtmlSanitizer htmlSanitizer;

    public String sanitize(String content) {
        return htmlSanitizer.sanitize(content);
    }
}


