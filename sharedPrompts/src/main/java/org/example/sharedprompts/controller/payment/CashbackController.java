package org.example.sharedprompts.controller.payment;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.domain.payment.repository.CashbackRepository;
import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캐시백 컨트롤러
 */
@RestController
@RequestMapping("/cashbacks")
@RequiredArgsConstructor
public class CashbackController {

    private final CashbackService cashbackService;
    private final CashbackRepository cashbackRepository;

    /**
     * 사용자의 캐시백 내역 조회
     * GET /cashbacks/history
     */
    @GetMapping("/history")
    public ResponseEntity<CustomResponse<List<CashbackResponseDto>>> getCashbackHistory(
            @CurrentUser AuthUser authUser
    ) {
        List<CashbackResponseDto> response = cashbackRepository.findByUser_IdOrderByCreatedAtDesc(authUser.getId())
                .stream()
                .map(CashbackResponseDto::from)
                .collect(Collectors.toList());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 미지급 캐시백 총액 조회
     * GET /cashbacks/unpaid-total
     */
    @GetMapping("/unpaid-total")
    public ResponseEntity<CustomResponse<BigDecimal>> getUnpaidCashbackTotal(
            @CurrentUser AuthUser authUser
    ) {
        BigDecimal total = cashbackService.getUnpaidCashbackTotal(authUser.getId());
        return CustomResponseHelper.ok(total);
    }

    /**
     * 미지급 캐시백 목록 조회
     * GET /cashbacks/unpaid
     */
    @GetMapping("/unpaid")
    public ResponseEntity<CustomResponse<List<CashbackResponseDto>>> getUnpaidCashbacks(
            @CurrentUser AuthUser authUser
    ) {
        List<CashbackResponseDto> response = cashbackRepository.findByUser_IdAndPaidFalseOrderByCreatedAtAsc(authUser.getId())
                .stream()
                .map(CashbackResponseDto::from)
                .collect(Collectors.toList());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 캐시백 지급 요청
     * POST /cashbacks/{cashbackId}/pay
     */
    @PostMapping("/{cashbackId}/pay")
    public ResponseEntity<CustomResponse<String>> payCashback(
            @PathVariable Long cashbackId,
            @CurrentUser AuthUser authUser
    ) {
        cashbackService.payCashback(authUser.getId(), cashbackId);
        return CustomResponseHelper.ok("캐시백 지급이 완료되었습니다.");
    }
}

