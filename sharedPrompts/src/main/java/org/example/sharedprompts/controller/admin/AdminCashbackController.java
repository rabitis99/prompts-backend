package org.example.sharedprompts.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 관리자 캐시백 컨트롤러
 */
@AdminOnly
@RestController
@RequestMapping("/admin/cashbacks")
@RequiredArgsConstructor
public class AdminCashbackController {

    private final CashbackService cashbackService;

    /**
     * 사용자 캐시백 내역 조회 (관리자용)
     * GET /admin/cashbacks/users/{userId}/history
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<CustomResponse<PageResponse<CashbackResponseDto>>> getCashbackHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        PageResponse<CashbackResponseDto> response = PageResponse.of(
                cashbackService.getCashbackHistory(userId, pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 미지급 캐시백 총액 조회 (관리자용)
     * GET /admin/cashbacks/users/{userId}/unpaid-total
     */
    @GetMapping("/users/{userId}/unpaid-total")
    public ResponseEntity<CustomResponse<BigDecimal>> getUnpaidCashbackTotal(
            @PathVariable Long userId
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        BigDecimal total = cashbackService.getUnpaidCashbackTotal(userId);
        return CustomResponseHelper.ok(total);
    }

    /**
     * 사용자 미지급 캐시백 목록 조회 (관리자용)
     * GET /admin/cashbacks/users/{userId}/unpaid
     */
    @GetMapping("/users/{userId}/unpaid")
    public ResponseEntity<CustomResponse<PageResponse<CashbackResponseDto>>> getUnpaidCashbacks(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // 사용자 존재 여부 검증은 서비스 레이어에서 처리
        PageResponse<CashbackResponseDto> response = PageResponse.of(
                cashbackService.getUnpaidCashbacks(userId, pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 전체 미지급 캐시백 총액 조회 (관리자용)
     * GET /admin/cashbacks/unpaid-total
     */
    @GetMapping("/unpaid-total")
    public ResponseEntity<CustomResponse<BigDecimal>> getAllUnpaidCashbackTotal() {
        BigDecimal total = cashbackService.getAllUnpaidCashbackTotal();
        return CustomResponseHelper.ok(total);
    }

    /**
     * 전체 미지급 캐시백 목록 조회 (관리자용)
     * GET /admin/cashbacks/unpaid
     */
    @GetMapping("/unpaid")
    public ResponseEntity<CustomResponse<PageResponse<CashbackResponseDto>>> getAllUnpaidCashbacks(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<CashbackResponseDto> response = PageResponse.of(
                cashbackService.getAllUnpaidCashbacks(pageable)
        );
        return CustomResponseHelper.ok(response);
    }

    /**
     * 캐시백 지급 (관리자용)
     * POST /admin/cashbacks/{cashbackId}/pay
     */
    @PostMapping("/{cashbackId}/pay")
    public ResponseEntity<CustomResponse<String>> payCashback(
            @PathVariable Long cashbackId
    ) {
        // 관리자는 모든 캐시백 지급 가능 (소유권 검증 없음)
        cashbackService.payCashbackForAdmin(cashbackId);
        return CustomResponseHelper.ok("캐시백 지급이 완료되었습니다.");
    }
}

