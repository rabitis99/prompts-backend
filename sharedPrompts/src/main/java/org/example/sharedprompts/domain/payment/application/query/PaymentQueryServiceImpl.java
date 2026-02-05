package org.example.sharedprompts.domain.payment.application.query;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.application.query.PaymentStatusSyncService;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentStatusSyncService statusSyncService;

    @Override
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId) {
        return statusSyncService.syncPaymentStatus(paymentId, userId);
    }

    @Override
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)
                .map(PaymentResponseDto::from);
    }
}

