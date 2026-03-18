package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.RecommendPromptRequest;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.springframework.stereotype.Component;

/**
 * 추천 요청 DTO → 추천 커맨드 변환 매퍼
 */
@Component
public class RecommendationRequestMapper {

    /**
     * API 요청 DTO를 유즈케이스 커맨드로 변환
     */
    public RecommendPromptCommand toCommand(RecommendPromptRequest request) {
        return new RecommendPromptCommand(
                request.requestMode(),
                request.category(),
                request.intent(),
                request.roleType(),
                request.actionType(),
                request.tone(),
                request.style(),
                request.language(),
                request.experience(),
                request.rawInput(),
                null,
                null
        );
    }
}