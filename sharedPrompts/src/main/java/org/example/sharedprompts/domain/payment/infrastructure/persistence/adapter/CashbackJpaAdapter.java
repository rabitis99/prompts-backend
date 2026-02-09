package org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Cashback;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.cashback.CashbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CashbackJpaAdapter {

    private final CashbackRepository cashbackRepository;

    public Cashback save(Cashback cashback) {
        return cashbackRepository.save(cashback);
    }

    public Optional<Cashback> findById(Long id) {
        return cashbackRepository.findById(id);
    }

    public BigDecimal getUnpaidCashbackTotal(Long userId) {
        return cashbackRepository.getUnpaidCashbackTotal(userId);
    }

    public Optional<Cashback> findByPaymentId(Long paymentId) {
        return cashbackRepository.findByPaymentId(paymentId);
    }

    public BigDecimal getAllUnpaidCashbackTotal() {
        return cashbackRepository.getAllUnpaidCashbackTotal();
    }

    public Page<Cashback> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        return cashbackRepository.findByUserIdWithFetchJoin(userId, pageable);
    }

    public Page<Cashback> findUnpaidByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        return cashbackRepository.findUnpaidByUserIdWithFetchJoin(userId, pageable);
    }

    public Page<Cashback> findAllUnpaidWithFetchJoin(Pageable pageable) {
        return cashbackRepository.findAllUnpaidWithFetchJoin(pageable);
    }
}




