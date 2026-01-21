package org.example.sharedprompts.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.admin.service.AdminMaintenanceService;
import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminOnly
@RestController
@RequestMapping("/admin/maintenance")
@RequiredArgsConstructor
public class AdminMaintenanceController {

    private final AdminMaintenanceService adminMaintenanceService;

    /**
     * DB에 저장된 like_count(프롬프트/댓글)를 기준으로
     * Redis의 좋아요 카운트를 재설정한다.
     *
     * - Redis 장애/초기화 이후, 관리자가 수동으로 호출하는 용도
     * - 대량 데이터를 스캔하므로 빈번하게 호출하지 않도록 주의
     */
    @PostMapping("/likes/rebuild")
    public ResponseEntity<CustomResponse<Void>> rebuildLikeCountsFromDb() {
        adminMaintenanceService.rebuildLikeCountsFromDbAsync();
        // 즉시 응답을 반환하여 HTTP 타임아웃을 방지한다.
        return CustomResponseHelper.ok(null);
    }

    /**
     * 좋아요 카운트 재빌드 작업 상태 조회
     */
    @GetMapping("/likes/rebuild/status")
    public ResponseEntity<CustomResponse<RebuildLikeCountsStatusResponseDto>> getRebuildLikeCountsStatus() {
        RebuildLikeCountsStatusResponseDto status = adminMaintenanceService.getRebuildLikeCountsStatus();
        return CustomResponseHelper.ok(status);
    }
}


