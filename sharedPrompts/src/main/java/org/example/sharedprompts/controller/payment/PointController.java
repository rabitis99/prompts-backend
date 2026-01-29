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
     * 포인트 잔액 조회
     * GET /points/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<CustomResponse<PointBalanceResponseDto>> getBalance(
            @CurrentUser AuthUser authUser
    ) {
        PointBalanceResponseDto response = pointService.getBalanceDetail(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 포인트 내역 조회
     * GET /points/history
     */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<List<PointResponseDto>>> getPointHistory(
            @CurrentUser AuthUser authUser
    ) {
        List<PointResponseDto> response = pointService.getPointHistory(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 포인트 사용
     * POST /points/use
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
     * 결제와 연관된 포인트 조회
     * GET /points/payment/{paymentId}
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

