package org.example.sharedprompts.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.service.core.PaymentService;
import org.example.sharedprompts.domain.payment.service.user.UserTierService;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 결제 컨트롤러
 */
@AdminOnly
@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final UserTierService userTierService;

    /**
     * 결제 상태 조회 (관리자용)
     * GET /admin/payments/{paymentId}/status
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<CustomResponse<PaymentStatusResponseDto>> checkPaymentStatus(
            @PathVariable Long paymentId
    ) {
        // 관리자는 모든 결제 상태 조회 가능 (소유권 검증 없음)
        PaymentStatusResponseDto response = paymentService.checkPaymentStatusForAdmin(paymentId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 결제 내역 조회 (관리자용)
     * GET /admin/payments/users/{userId}/history
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<CustomResponse<PageResponse<PaymentResponseDto>>> getPaymentHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        PageResponse<PaymentResponseDto> response = PageResponse.of(
                paymentService.getPaymentHistory(userId, pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 전체 결제 내역 조회 (관리자용)
     * GET /admin/payments/history
     */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<PageResponse<PaymentResponseDto>>> getAllPaymentHistory(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PaymentResponseDto> response = PageResponse.of(
                paymentService.getAllPaymentHistory(pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 결제 취소 (관리자용)
     * POST /admin/payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<CustomResponse<PaymentResponseDto>> cancelPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) PaymentCancelRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        // paymentId를 포함한 새로운 DTO 생성
        PaymentCancelRequestDto cancelRequest = PaymentCancelRequestDto.builder()
                .paymentId(paymentId.toString())
                .reason(request != null ? request.getReason() : "관리자 요청")
                .build();
        
        // 관리자는 모든 결제 취소 가능 (소유권 검증 없음)
        PaymentResponseDto response = paymentService.cancelPaymentForAdmin(paymentId, cancelRequest, authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 결제 환불 (관리자용)
     * POST /admin/payments/{paymentId}/refund
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<CustomResponse<PaymentResponseDto>> refundPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) PaymentRefundRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        // paymentId를 포함한 새로운 DTO 생성
        PaymentRefundRequestDto refundRequest = PaymentRefundRequestDto.builder()
                .paymentId(paymentId.toString())
                .amount(request != null ? request.getAmount() : null)
                .reason(request != null ? request.getReason() : "관리자 요청")
                .build();
        
        // 관리자는 모든 결제 환불 가능 (소유권 검증 없음)
        PaymentResponseDto response = paymentService.refundPaymentForAdmin(paymentId, refundRequest, authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 티어 조회 (관리자용)
     * GET /admin/payments/users/{userId}/tier
     */
    @GetMapping("/users/{userId}/tier")
    public ResponseEntity<CustomResponse<UserTier>> getTier(
            @PathVariable Long userId
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        UserTier tier = userTierService.getTier(userId);
        return CustomResponseHelper.ok(tier);
    }

    /**
     * 사용자 티어 정보 조회 (관리자용)
     * GET /admin/payments/users/{userId}/tier-info
     */
    @GetMapping("/users/{userId}/tier-info")
    public ResponseEntity<CustomResponse<TierInfoResponseDto>> getTierInfo(
            @PathVariable Long userId
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        TierInfoResponseDto response = userTierService.getTierInfo(userId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 티어 변경 이력 조회 (관리자용)
     * GET /admin/payments/users/{userId}/tier-history
     */
    @GetMapping("/users/{userId}/tier-history")
    public ResponseEntity<CustomResponse<PageResponse<UserTierHistoryResponseDto>>> getTierHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        PageResponse<UserTierHistoryResponseDto> response = PageResponse.of(
                userTierService.getTierHistory(userId, pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 티어 변경 (관리자용)
     * POST /admin/payments/users/{userId}/tier
     */
    @PostMapping("/users/{userId}/tier")
    public ResponseEntity<CustomResponse<Void>> changeTier(
            @PathVariable Long userId,
            @Valid @RequestBody TierChangeRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        userTierService.changeTier(userId, request, authUser.getId());
        return CustomResponseHelper.ok(null);
    }
}

