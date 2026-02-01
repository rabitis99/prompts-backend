package org.example.sharedprompts.controller.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.facade.PaymentFacade;
import org.example.sharedprompts.domain.payment.service.user.UserTierService;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

/**
 * 결제 컨트롤러
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;
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
        PaymentResponseDto response = paymentFacade.requestPayment(authUser.getId(), request);
        return CustomResponseHelper.created(response);
    }

    /**
     * 결제 상태 조회
     * GET /payments/{paymentId}/status
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<CustomResponse<PaymentStatusResponseDto>> checkPaymentStatus(
            @PathVariable Long paymentId,
            @CurrentUser AuthUser authUser
    ) {
        PaymentStatusResponseDto response = paymentFacade.checkPaymentStatus(paymentId, authUser.getId());
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
        PaymentResponseDto response = paymentFacade.cancelPayment(authUser.getId(), request);
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
        PaymentResponseDto response = paymentFacade.refundPayment(authUser.getId(), request);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 내 티어 조회
     * GET /payments/me/tier
     */
    @GetMapping("/me/tier")
    public ResponseEntity<CustomResponse<UserTier>> getMyTier(
            @CurrentUser AuthUser authUser
    ) {
        UserTier tier = userTierService.getTier(authUser.getId());
        return CustomResponseHelper.ok(tier);
    }

    /**
     * 내 티어 정보 조회 (티어, 일일 제한, 오늘 사용한 횟수, 남은 횟수)
     * GET /payments/me/tier-info
     */
    @GetMapping("/me/tier-info")
    public ResponseEntity<CustomResponse<TierInfoResponseDto>> getMyTierInfo(
            @CurrentUser AuthUser authUser
    ) {
        TierInfoResponseDto response = userTierService.getTierInfo(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자의 결제 내역 조회
     * GET /payments/history
     */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<PageResponse<PaymentResponseDto>>> getPaymentHistory(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PaymentResponseDto> response = PageResponse.of(paymentFacade.getPaymentHistory(authUser.getId(), pageable));
        return CustomResponseHelper.ok(response);
    }

    /**
     * 내 티어 변경 이력 조회
     * GET /payments/me/tier-history
     */
    @GetMapping("/me/tier-history")
    public ResponseEntity<CustomResponse<PageResponse<UserTierHistoryResponseDto>>> getMyTierHistory(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<UserTierHistoryResponseDto> response = PageResponse.of(userTierService.getTierHistory(authUser.getId(), pageable));
        return CustomResponseHelper.ok(response);
    }

    /**
     * 결제 승인 (토스페이먼츠 등 결제사별 승인 처리)
     * POST /payments/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<CustomResponse<PaymentConfirmResponse>> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request,
            @CurrentUser AuthUser authUser
    ) {
        PaymentConfirmResponse response = paymentFacade.confirmPayment(authUser.getId(), request);
        return CustomResponseHelper.ok(response);
    }
}

