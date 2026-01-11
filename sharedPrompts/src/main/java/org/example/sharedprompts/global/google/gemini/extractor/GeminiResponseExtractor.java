package org.example.sharedprompts.global.google.gemini.extractor;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.google.gemini.response.Candidate;
import org.example.sharedprompts.global.google.gemini.response.ChatResponse;
import org.example.sharedprompts.global.google.gemini.response.Content;
import org.springframework.stereotype.Component;

/**
 * Gemini API 응답에서 텍스트를 추출하는 클래스
 */
@Component
@Slf4j
public class GeminiResponseExtractor {

    /**
     * ChatResponse에서 첫 번째 후보의 텍스트를 추출합니다.
     *
     * @param response Gemini API 응답
     * @return 추출된 텍스트, 추출할 수 없는 경우 빈 문자열
     */
    public String extractFirstCandidate(ChatResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            log.warn("No candidates returned from Gemini API");
            return "";
        }

        Candidate candidate = response.getCandidates().get(0);
        Content content = candidate.getContent();

        if (content == null || content.getParts() == null || content.getParts().isEmpty()) {
            log.warn("No content parts returned from Gemini API");
            return "";
        }

        return content.getParts().get(0).getText();
    }
}

