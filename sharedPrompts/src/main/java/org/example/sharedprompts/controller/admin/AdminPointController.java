package org.example.sharedprompts.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.dto.payment.request.PointUseRequestDto;
import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 포인트 컨트롤러
 */
@AdminOnly
@RestController
@RequestMapping("/admin/points")
@RequiredArgsConstructor
public class AdminPointController {

    private final PointService pointService;

    /**
     * 사용자 포인트 잔액 조회 (관리자용)
     * GET /admin/points/users/{userId}/balance
     */
    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<CustomResponse<PointBalanceResponseDto>> getBalance(
            @PathVariable Long userId
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        PointBalanceResponseDto response = pointService.getBalanceDetail(userId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 포인트 내역 조회 (관리자용)
     * GET /admin/points/users/{userId}/history
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<CustomResponse<PageResponse<PointResponseDto>>> getPointHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        PageResponse<PointResponseDto> response = PageResponse.of(
                pointService.getPointHistory(userId, pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 포인트 차감 (관리자용)
     * POST /admin/points/users/{userId}/use
     */
    @PostMapping("/users/{userId}/use")
    public ResponseEntity<CustomResponse<PointBalanceResponseDto>> usePoints(
            @PathVariable Long userId,
            @Valid @RequestBody PointUseRequestDto request
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        // 관리자 요청으로 포인트 차감 (description에 관리자 정보 포함 권장)
        String description = request.getDescription() != null 
                ? "[관리자] " + request.getDescription()
                : "[관리자] 관리자 요청";
        
        pointService.usePoints(userId, request.getAmount(), description);
        PointBalanceResponseDto response = pointService.getBalanceDetail(userId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 결제별 포인트 조회 (관리자용)
     * GET /admin/points/payments/{paymentId}
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<CustomResponse<PageResponse<PointResponseDto>>> getPointsByPayment(
            @PathVariable Long paymentId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // 관리자는 모든 결제의 포인트 조회 가능 (소유권 검증 없음)
        // userId를 null로 전달하여 소유권 검증을 건너뛰도록 서비스 수정 필요
        // 일단 임시로 userId를 null로 전달 (서비스 레이어에서 null 체크 필요)
        PageResponse<PointResponseDto> response = PageResponse.of(
                pointService.getPointsByPaymentForAdmin(paymentId, pageable)
        );
        return CustomResponseHelper.ok(response);
    }
}

