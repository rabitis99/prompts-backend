package org.example.sharedprompts.controller.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentCommandUseCase;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentQueryUseCase;
import org.example.sharedprompts.domain.payment.adapter.in.web.mapper.PaymentControllerMapper;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.service.user.tier.UserTierService;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.request.ConsumeModuleUsageRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentApprovalResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.example.sharedprompts.dto.payment.response.ConsumeModuleUsageResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;

import java.util.Optional;

/**
 * 결제 컨트롤러
 * 헥사고날 아키텍처로 리팩토링되었습니다.
 * 직접 UseCase 인터페이스를 호출하고, DTO 변환은 PaymentControllerMapper를 통해 수행합니다.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCommandUseCase paymentCommandUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;
    private final PaymentControllerMapper mapper;
    private final UserTierService userTierService;

    /**
     * 결제 요청
     * POST /payments
     */
    @PostMapping
    public ResponseEntity<CustomResponse<PaymentApprovalResponseDto>> requestPayment(
            @Valid @RequestBody PaymentRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        var command = mapper.toApproveCommand(request, authUser.getId());
        var result = paymentCommandUseCase.approve(command);
        var response = mapper.toApprovalResponse(result);
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
        var query = mapper.toStatusCheckQuery(paymentId, authUser.getId());
        var result = paymentQueryUseCase.checkStatus(query);
        var response = mapper.toStatusResponse(result);
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
        var command = mapper.toCancelCommand(request, authUser.getId());
        var result = paymentCommandUseCase.cancel(command);
        var response = mapper.toCancellationResponse(result);
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
        var command = mapper.toRefundCommand(request, authUser.getId());
        var result = paymentCommandUseCase.refund(command);
        var response = mapper.toRefundResponse(result);
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
     * 내 티어 정보 조회 (티어, 일일 제한, 오늘 사용한 횟수, 남은 횟수, 모듈별 남은 횟수).
     * GET /payments/me/tier-info
     * GET /payments/me/tier-info?moduleType=LITERARY — 해당 모듈만 remaining 반환
     */
    @GetMapping("/me/tier-info")
    public ResponseEntity<CustomResponse<TierInfoResponseDto>> getMyTierInfo(
            @CurrentUser AuthUser authUser,
            @RequestParam(required = false) String moduleType
    ) {
        TierInfoResponseDto response = parseModuleType(moduleType)
                .map(mt -> userTierService.getTierInfo(authUser.getId(), mt))
                .orElseGet(() -> userTierService.getTierInfo(authUser.getId()));
        return CustomResponseHelper.ok(response);
    }

    /**
     * 모듈 사용 1회 차감. moduleType 필수. 해당 모듈 일일 한도만 검증.
     * 남은 횟수 0이면 400 MODULE_DAILY_LIMIT_EXCEEDED.
     * POST /payments/usage/consume
     */
    @PostMapping("/usage/consume")
    public ResponseEntity<CustomResponse<ConsumeModuleUsageResponseDto>> consumeModuleUsage(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody ConsumeModuleUsageRequestDto request
    ) {
        ModuleType mt = parseModuleTypeRequired(request.getModuleType());
        ConsumeModuleUsageResponseDto response = userTierService.consumeModuleUsage(authUser.getId(), mt);
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
        var query = mapper.toHistoryQuery(authUser.getId(), pageable);
        var resultPage = paymentQueryUseCase.getHistory(query);

        Page<PaymentResponseDto> responsePage = resultPage.map(mapper::toHistoryItemResponse);
        PageResponse<PaymentResponseDto> response = PageResponse.of(responsePage);
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
        var command = mapper.toConfirmCommand(request, authUser.getId());
        var result = paymentCommandUseCase.confirm(command);

        // 기존 PaymentConfirmResponse 형식 유지 (하위 호환성)
        PaymentConfirmResponse response = mapper.toConfirmationResponse(result);
        return CustomResponseHelper.ok(response);
    }

    private static Optional<ModuleType> resolveModuleType(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        ModuleType mt = ModuleType.from(value);
        if (mt == null || mt == ModuleType.UNKNOWN) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return Optional.of(mt);
    }

    private static Optional<ModuleType> parseModuleType(String value) {
        return resolveModuleType(value);
    }

    private static ModuleType parseModuleTypeRequired(String value) {
        return resolveModuleType(value)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT_VALUE));
    }
}
