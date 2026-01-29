package org.example.sharedprompts.controller.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.service.core.PaymentService;
import org.example.sharedprompts.domain.payment.service.user.UserTierService;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 결제 컨트롤러
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserTierService userTierService;

    /**
     * 결제 요청
     * POST /payments
     */
    @PostMapping
    public ResponseEntity<CustomResponse<PaymentResponseDto>> requestPayment(
            @Valid @RequestBody PaymentRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        PaymentResponseDto response = paymentService.requestPayment(authUser.getId(), request);
        return CustomResponseHelper.created(response);
    }

    /**
     * 결제 상태 조회
     * GET /payments/{paymentId}/status
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<CustomResponse<PaymentStatusResponseDto>> checkPaymentStatus(
            @PathVariable String paymentId
    ) {
        PaymentStatusResponseDto response = paymentService.checkPaymentStatus(paymentId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 결제 취소
     * POST /payments/cancel
     */
    @PostMapping("/cancel")
    public ResponseEntity<CustomResponse<PaymentResponseDto>> cancelPayment(
            @Valid @RequestBody PaymentCancelRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        PaymentResponseDto response = paymentService.cancelPayment(authUser.getId(), request);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 결제 환불 (부분/전체)
     * POST /payments/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<CustomResponse<PaymentResponseDto>> refundPayment(
            @Valid @RequestBody PaymentRefundRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        PaymentResponseDto response = paymentService.refundPayment(authUser.getId(), request);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 티어 조회
     * GET /users/{userId}/tier
     */
    @GetMapping("/users/{userId}/tier")
    public ResponseEntity<CustomResponse<UserTier>> getTier(
            @PathVariable Long userId
    ) {
        UserTier tier = userTierService.getTier(userId);
        return CustomResponseHelper.ok(tier);
    }

    /**
     * 사용자 티어 정보 조회 (티어, 일일 제한, 오늘 사용한 횟수, 남은 횟수)
     * GET /users/{userId}/tier-info
     */
    @GetMapping("/users/{userId}/tier-info")
    public ResponseEntity<CustomResponse<TierInfoResponseDto>> getTierInfo(
            @PathVariable Long userId,
            @CurrentUser AuthUser authUser
    ) {
        // 자신의 정보만 조회 가능
        if (!authUser.getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        TierInfoResponseDto response = userTierService.getTierInfo(userId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자의 결제 내역 조회
     * GET /payments/history
     */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<List<PaymentResponseDto>>> getPaymentHistory(
            @CurrentUser AuthUser authUser
    ) {
        List<PaymentResponseDto> response = paymentService.getPaymentHistory(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자의 티어 변경 이력 조회
     * GET /payments/users/{userId}/tier-history
     */
    @GetMapping("/users/{userId}/tier-history")
    public ResponseEntity<CustomResponse<List<UserTierHistoryResponseDto>>> getTierHistory(
            @PathVariable Long userId,
            @CurrentUser AuthUser authUser
    ) {
        // 자신의 정보만 조회 가능
        if (!authUser.getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        List<UserTierHistoryResponseDto> response = userTierService.getTierHistory(userId);
        return CustomResponseHelper.ok(response);
    }
}

