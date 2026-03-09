package org.example.sharedprompts.domain.prompt.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.RecommendPromptRequest;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendPromptResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.RecommendPromptResponseMapper;
import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.RecommendationRequestMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.RecommendPromptAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recommendation-only endpoint: returns recommended semantic axes without generating a prompt.
 */
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendPromptAxesUseCase recommendPromptAxesUseCase;
    private final RecommendationRequestMapper requestMapper;
    private final RecommendPromptResponseMapper responseMapper;

    @PostMapping("/recommend")
    public ResponseEntity<CustomResponse<RecommendPromptResponse>> recommend(
            @Valid @RequestBody RecommendPromptRequest request
    ) {
        RecommendPromptCommand command = requestMapper.toCommand(request);
        RecommendPromptResult result = recommendPromptAxesUseCase.recommend(command);
        RecommendPromptResponse response = responseMapper.toResponse(result);
        return CustomResponseHelper.ok(response);
    }
}
