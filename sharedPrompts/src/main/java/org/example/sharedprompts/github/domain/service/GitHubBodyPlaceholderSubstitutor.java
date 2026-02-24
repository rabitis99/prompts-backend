package org.example.sharedprompts.github.domain.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {{PLACEHOLDER}} 형태의 문자열 치환. 템플릿/생성 결과에 공통 적용.
 */
public final class GitHubBodyPlaceholderSubstitutor {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z_]+)\\}\\}");

    private GitHubBodyPlaceholderSubstitutor() {}

    public static String substitute(String template, Map<String, String> vars) {
        if (template == null) return "";
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = vars.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
