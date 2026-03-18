package org.example.sharedprompts.domain.prompt.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.RecommendPromptRequest;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.facade.PromptRecommendationFacade;
import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.RecommendationRequestMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프롬프트 추천 API. UX 흐름(Category → Intent → Action → Role)에 맞는 응답을 반환한다.
 * 내부 결과는 facade에서 assembler를 통해 외부 DTO로 변환 후 반환.
 */
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class RecommendationController {

    private final PromptRecommendationFacade recommendationFacade;
    private final RecommendationRequestMapper requestMapper;

    /** 추천 축 반환 (Category → Intent → recommendedActions[] → recommendedRoles[]). */
    @PostMapping("/recommend")
    public ResponseEntity<CustomResponse<PromptRecommendationResponse>> recommend(
            @Valid @RequestBody RecommendPromptRequest request
    ) {
        RecommendPromptCommand command = requestMapper.toCommand(request);
        PromptRecommendationResponse response = recommendationFacade.recommend(command);
        return CustomResponseHelper.ok(response);
    }
}