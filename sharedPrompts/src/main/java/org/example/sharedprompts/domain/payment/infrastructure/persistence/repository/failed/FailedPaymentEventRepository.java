package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.failed;

import org.example.sharedprompts.domain.payment.infrastructure.persistence.entity.FailedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedPaymentEventRepository extends JpaRepository<FailedPaymentEvent, Long> {
}

