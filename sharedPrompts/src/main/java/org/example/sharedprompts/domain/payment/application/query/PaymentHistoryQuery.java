package org.example.sharedprompts.domain.payment.application.query;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
@Builder
public class PaymentHistoryQuery {
    private final Long userId;
    private final Pageable pageable;
}

