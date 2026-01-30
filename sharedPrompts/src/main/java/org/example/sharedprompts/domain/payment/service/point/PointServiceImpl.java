package org.example.sharedprompts.domain.payment.service.point;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.Point;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.PointRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PointUseRequestDto;
import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포인트 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;
    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void accumulatePoints(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 포인트 적립률 적용
        BigDecimal pointAmount = paymentAmount.multiply(BigDecimal.valueOf(paymentProperties.getPointRate()))
                .setScale(0, RoundingMode.DOWN); // 소수점 버림

        if (pointAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            
            // 현재 잔액 조회
            BigDecimal currentBalance = pointRepository.getCurrentBalance(userId)
                    .orElse(BigDecimal.ZERO);

            Point point = Point.builder()
                    .user(user)
                    .paymentId(paymentId)
                    .amount(pointAmount)
                    .type("PAYMENT")
                    .description("결제 포인트 적립")
                    .balance(currentBalance.add(pointAmount))
                    .expired(false)
                    .build();

            pointRepository.save(point);
        }
    }

    @Override
    @Transactional
    public void usePoints(Long userId, BigDecimal amount, String description) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        BigDecimal currentBalance = pointRepository.getCurrentBalance(userId)
                .orElse(BigDecimal.ZERO);

        if (currentBalance.compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.POINT_INSUFFICIENT);
        }

        // PointUseRequestDto의 mapper를 사용하여 엔티티 생성
        // (인터페이스 호환성을 위해 내부적으로 DTO를 생성)
        PointUseRequestDto requestDto = PointUseRequestDto.builder()
                .amount(amount)
                .description(description)
                .build();

        Point point = requestDto.toPointBuilder(user, currentBalance.subtract(amount))
                .build();

        pointRepository.save(point);
    }

    @Override
    public BigDecimal getCurrentBalance(Long userId) {
        return pointRepository.getCurrentBalance(userId)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public PointBalanceResponseDto getBalanceDetail(Long userId) {
        // 현재 잔액 조회 (만료되지 않은 포인트의 amount 합계)
        BigDecimal currentBalance = getCurrentBalance(userId);
        
        // 만료되지 않은 포인트만 조회 (적립된 포인트만)
        BigDecimal availableBalance = pointRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(p -> !p.isExpired() && p.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Point::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 곧 만료될 포인트 (30일 이내) - 적립된 포인트만
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
        List<Point> expiringPoints = pointRepository.findExpiringPoints(userId, expiryDate);
        BigDecimal expiringSoon = expiringPoints.stream()
                .filter(p -> !p.isExpired() && p.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Point::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return PointBalanceResponseDto.from(userId, currentBalance, availableBalance, expiringSoon);
    }

    @Override
    public List<PointResponseDto> getPointHistory(Long userId) {
        return pointRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PointResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<PointResponseDto> getPointsByPayment(Long paymentId, Long userId) {
        List<Point> points = pointRepository.findByPaymentIdAndUserId(paymentId, userId);

        if (points.isEmpty()) {
            // 권한 없음 또는 결제 없음 판단 가능
            throw new ApiException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return points.stream()
                .map(PointResponseDto::from)
                .collect(Collectors.toList());
    }
}

