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
     * Initiates a payment for the authenticated user.
     *
     * @param request the payment details required to create the payment
     * @param authUser the authenticated user performing the request
     * @return a `CustomResponse` containing the created `PaymentResponseDto`, returned with HTTP 201 Created
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
         * Retrieve the current status of a payment.
         *
         * @param paymentId the identifier of the payment to query
         * @return the payment status information wrapped in a CustomResponse
         */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<CustomResponse<PaymentStatusResponseDto>> checkPaymentStatus(
            @PathVariable String paymentId
    ) {
        PaymentStatusResponseDto response = paymentService.checkPaymentStatus(paymentId);
        return CustomResponseHelper.ok(response);
    }

    /**
     * Cancels a previously created payment for the authenticated user.
     *
     * @param request details required to identify and cancel the payment
     * @param authUser the authenticated user performing the cancellation
     * @return a CustomResponse containing the PaymentResponseDto reflecting the cancellation result
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
     * Process a partial or full refund for the authenticated user's payment.
     *
     * @param request  details of the refund to perform (amounts, payment identifiers, etc.)
     * @param authUser the authenticated user initiating the refund
     * @return a CustomResponse wrapping a PaymentResponseDto with updated payment details after the refund
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
     * Retrieve the tier for the specified user.
     *
     * @param userId the ID of the user whose tier is retrieved
     * @return the user's tier information wrapped in a CustomResponse
     */
    @GetMapping("/users/{userId}/tier")
    public ResponseEntity<CustomResponse<UserTier>> getTier(
            @PathVariable Long userId
    ) {
        UserTier tier = userTierService.getTier(userId);
        return CustomResponseHelper.ok(tier);
    }

    /**
         * Retrieve the authenticated user's tier overview, including tier, daily limit, uses today, and remaining uses.
         *
         * @param userId   the ID of the user whose tier info is requested; must equal the authenticated user's ID
         * @param authUser the authenticated user making the request
         * @return         a CustomResponse containing a TierInfoResponseDto with tier details, daily limit, today's usage, and remaining uses
         * @throws ApiException with ErrorCode.FORBIDDEN if the authenticated user is not the same as the requested user
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
         * Retrieves the authenticated user's payment history.
         *
         * @param authUser the currently authenticated user
         * @return a CustomResponse wrapping a list of PaymentResponseDto representing the user's payment history
         */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<List<PaymentResponseDto>>> getPaymentHistory(
            @CurrentUser AuthUser authUser
    ) {
        List<PaymentResponseDto> response = paymentService.getPaymentHistory(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * Retrieve a user's tier change history; only the requesting user may access their own history.
     *
     * @param userId the ID of the user whose tier history is requested; must equal the authenticated user's ID
     * @return a ResponseEntity containing a CustomResponse with a list of UserTierHistoryResponseDto entries
     * @throws ApiException with ErrorCode.FORBIDDEN if the authenticated user is not the same as {@code userId}
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
