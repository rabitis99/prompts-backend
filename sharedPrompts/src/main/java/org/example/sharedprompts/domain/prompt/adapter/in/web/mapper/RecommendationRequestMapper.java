package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.RecommendPromptRequest;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.springframework.stereotype.Component;

/**
 * Adapter mapper: request DTO → application command.
 * Keeps the web layer from directly constructing application command types.
 */
@Component
public class RecommendationRequestMapper {

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
                request.rawInput()
        );
    }
}
