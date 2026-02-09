package org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Point;
import org.example.sharedprompts.domain.payment.domain.enums.PointType;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.point.PointRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PointJpaAdapter {

    private final PointRepository pointRepository;

    public Point save(Point point) {
        return pointRepository.save(point);
    }

    public Optional<Point> findById(Long id) {
        return pointRepository.findById(id);
    }

    public BigDecimal getCurrentBalance(Long userId) {
        return pointRepository.getCurrentBalance(userId);
    }

    public List<BigDecimal> findLatestBalances(Long userId) {
        return pointRepository.findLatestBalances(userId);
    }

    public List<Point> findExpiringPoints(Long userId, LocalDateTime expiryDate) {
        return pointRepository.findExpiringPoints(userId, expiryDate);
    }

    public List<Point> findLatestPointByUserId(Long userId) {
        return pointRepository.findLatestPointByUserId(userId);
    }

    public boolean existsByPaymentIdAndType(Long paymentId, PointType type) {
        return pointRepository.existsByPaymentIdAndType(paymentId, type);
    }

    public boolean existsByPaymentIdAndUserIdAndType(Long paymentId, Long userId, PointType type) {
        return pointRepository.existsByPaymentIdAndUserIdAndType(paymentId, userId, type);
    }

    public Page<Point> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        return pointRepository.findByUserIdWithFetchJoin(userId, pageable);
    }

    public Page<Point> findByPaymentIdAndUserIdWithFetchJoin(Long paymentId, Long userId, Pageable pageable) {
        return pointRepository.findByPaymentIdAndUserIdWithFetchJoin(paymentId, userId, pageable);
    }

    public Page<Point> findByPaymentIdWithFetchJoin(Long paymentId, Pageable pageable) {
        return pointRepository.findByPaymentIdWithFetchJoin(paymentId, pageable);
    }
}




