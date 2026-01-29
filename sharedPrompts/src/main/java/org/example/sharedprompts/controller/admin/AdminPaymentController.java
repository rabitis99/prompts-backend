package org.example.sharedprompts.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.payment.service.user.UserTierService;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
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

    private final UserTierService userTierService;

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

        userTierService.changeTier(userId, request, authUser.getId());
        return CustomResponseHelper.ok(null);
    }
}

