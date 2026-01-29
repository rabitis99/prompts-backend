package org.example.sharedprompts.controller.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.dto.payment.request.PointUseRequestDto;
import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 포인트 컨트롤러
 */
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * Retrieves the authenticated user's point balance details.
     *
     * @param authUser the currently authenticated user
     * @return a CustomResponse containing the user's PointBalanceResponseDto
     */
    @GetMapping("/balance")
    public ResponseEntity<CustomResponse<PointBalanceResponseDto>> getBalance(
            @CurrentUser AuthUser authUser
    ) {
        PointBalanceResponseDto response = pointService.getBalanceDetail(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * Retrieves the authenticated user's point transaction history.
     *
     * @return a CustomResponse containing a list of PointResponseDto for the authenticated user's point transactions.
     */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<List<PointResponseDto>>> getPointHistory(
            @CurrentUser AuthUser authUser
    ) {
        List<PointResponseDto> response = pointService.getPointHistory(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * Use points for the authenticated user and return the updated balance.
     *
     * @param request  contains the amount of points to use and an optional description of the operation
     * @param authUser the authenticated user performing the point usage
     * @return the updated point balance details wrapped in a CustomResponse
     */
    @PostMapping("/use")
    public ResponseEntity<CustomResponse<PointBalanceResponseDto>> usePoints(
            @Valid @RequestBody PointUseRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        pointService.usePoints(authUser.getId(), request.getAmount(), request.getDescription());
        PointBalanceResponseDto response = pointService.getBalanceDetail(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * Retrieve points associated with a specific payment.
     *
     * @param paymentId the identifier of the payment whose associated points should be fetched
     * @return a CustomResponse containing a list of PointResponseDto objects linked to the specified payment
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<CustomResponse<List<PointResponseDto>>> getPointsByPayment(
            @PathVariable Long paymentId,
            @CurrentUser AuthUser authUser
    ) {
        List<PointResponseDto> response = pointService.getPointsByPayment(paymentId);
        return CustomResponseHelper.ok(response);
    }
}
