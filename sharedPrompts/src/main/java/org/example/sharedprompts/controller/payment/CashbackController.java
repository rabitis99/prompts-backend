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
     * Retrieve the authenticated user's cashback history.
     *
     * @param authUser the authenticated user whose cashback records are requested
     * @return a CustomResponse containing a list of CashbackResponseDto ordered by creation date descending
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
     * Retrieve the total unpaid cashback amount for the authenticated user.
     *
     * @param authUser the authenticated user whose unpaid cashback total will be computed
     * @return the total unpaid cashback amount for the user
     */
    @GetMapping("/unpaid-total")
    public ResponseEntity<CustomResponse<BigDecimal>> getUnpaidCashbackTotal(
            @CurrentUser AuthUser authUser
    ) {
        BigDecimal total = cashbackService.getUnpaidCashbackTotal(authUser.getId());
        return CustomResponseHelper.ok(total);
    }

    /**
     * Retrieve unpaid cashback records for the authenticated user.
     *
     * @param authUser the currently authenticated user
     * @return a CustomResponse containing a list of unpaid CashbackResponseDto objects for the user, ordered by creation time ascending
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
     * Request payment of the specified cashback for the authenticated user.
     *
     * @param cashbackId the ID of the cashback to be paid
     * @param authUser the authenticated user initiating the payment
     * @return a CustomResponse containing the success message "캐시백 지급이 완료되었습니다."
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
