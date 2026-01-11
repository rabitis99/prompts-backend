package org.example.sharedprompts.global.google.gemini.extractor;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.google.gemini.response.Candidate;
import org.example.sharedprompts.global.google.gemini.response.ChatResponse;
import org.example.sharedprompts.global.google.gemini.response.Content;
import org.example.sharedprompts.global.google.gemini.response.Part;
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
     * @return 추출된 텍스트
     * @throws ApiException 추출할 수 없는 경우 발생
     */
    public String extractFirstCandidate(ChatResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            log.warn("No candidates returned from Gemini API");
            throw new ApiException(ErrorCode.AI_RESPONSE_NO_CANDIDATES);
        }

        Candidate candidate = response.getCandidates().get(0);
        if (candidate == null) {
            log.warn("First candidate is null in Gemini API response");
            throw new ApiException(ErrorCode.AI_RESPONSE_CANDIDATE_NULL);
        }
        Content content = candidate.getContent();

        if (content == null || content.getParts() == null || content.getParts().isEmpty()) {
            log.warn("No content parts returned from Gemini API");
            throw new ApiException(ErrorCode.AI_RESPONSE_NO_CONTENT_PARTS);
        }

        Part part = content.getParts().get(0);
        if (part == null || part.getText() == null) {
            log.warn("First part or its text is null in Gemini API response");
            throw new ApiException(ErrorCode.AI_RESPONSE_PART_TEXT_NULL);
        }
        return part.getText();
    }
}

