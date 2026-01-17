package org.example.sharedprompts.controller.statistics;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.statistics.service.StatisticsService;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.MyStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.PromptStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.StatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.UserStatisticsResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 API 컨트롤러
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 전체 통계 조회
     */
    @GetMapping
    public ResponseEntity<CustomResponse<StatisticsResponseDto>> getAllStatistics() {
        StatisticsResponseDto statistics = statisticsService.getAllStatistics();
        return CustomResponseHelper.ok(statistics);
    }

    /**
     * 사용자 통계 조회
     */
    @GetMapping("/users")
    public ResponseEntity<CustomResponse<UserStatisticsResponseDto>> getUserStatistics() {
        UserStatisticsResponseDto statistics = statisticsService.getUserStatistics();
        return CustomResponseHelper.ok(statistics);
    }

    /**
     * 프롬프트 통계 조회
     */
    @GetMapping("/prompts")
    public ResponseEntity<CustomResponse<PromptStatisticsResponseDto>> getPromptStatistics() {
        PromptStatisticsResponseDto statistics = statisticsService.getPromptStatistics();
        return CustomResponseHelper.ok(statistics);
    }

    /**
     * AI 호출 통계 조회
     */
    @GetMapping("/ai-calls")
    public ResponseEntity<CustomResponse<AiCallStatisticsResponseDto>> getAiCallStatistics() {
        AiCallStatisticsResponseDto statistics = statisticsService.getAiCallStatistics();
        return CustomResponseHelper.ok(statistics);
    }

    /**
     * 내 통계 조회
     */
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<MyStatisticsResponseDto>> getMyStatistics(
            @CurrentUser AuthUser authUser
    ) {
        MyStatisticsResponseDto statistics = statisticsService.getMyStatistics(authUser.getId());
        return CustomResponseHelper.ok(statistics);
    }
}
