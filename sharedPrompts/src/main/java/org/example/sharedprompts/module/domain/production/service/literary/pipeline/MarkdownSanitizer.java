package org.example.sharedprompts.module.domain.production.service.literary.pipeline;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MarkdownSanitizer {

    // Match only at the very start of the document (no MULTILINE) to avoid stripping mid-document lines
    private static final Pattern EXPLANATION_PREFIX = Pattern.compile(
            "^(다음과\\s*같이|아래와\\s*같이|다음은|아래는|작성\\s*결과|결과물|출력\\s*내용)[:\\s]*"
    );

    public String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        t = EXPLANATION_PREFIX.matcher(t).replaceFirst("");
        return t.trim();
    }
}
