package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.util.HtmlSanitizer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentContentSanitizer {

    private final HtmlSanitizer htmlSanitizer;

    public String sanitize(String content) {
        return htmlSanitizer.sanitize(content);
    }
}


