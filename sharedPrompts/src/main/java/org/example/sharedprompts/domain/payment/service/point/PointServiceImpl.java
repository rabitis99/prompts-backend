package org.example.sharedprompts.domain.payment.service.point;

import lombok.RequiredArgsConstructor;
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

    /**
     * Accumulates loyalty points for a payment by calculating points from the payment amount and persisting a Point record.
     *
     * If the computed point amount (paymentAmount × configured point rate, rounded down to an integer) is greater than zero,
     * the method loads the user, computes the new balance, creates a Point entry with type "PAYMENT" and description "결제 포인트 적립",
     * and saves it. If the computed point amount is zero or less, the method does nothing.
     *
     * @param userId        the ID of the user who should receive points
     * @param paymentId     the ID of the payment that generated the points
     * @param paymentAmount the total payment amount used to calculate points
     * @throws ApiException if the user with the given ID does not exist (ErrorCode.USER_NOT_FOUND)
     */
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

    /**
     * Consume the specified amount of points from a user's balance and persist a deduction record.
     *
     * @param userId      the identifier of the user whose points will be used
     * @param amount      the amount of points to deduct
     * @param description a description stored with the deduction record
     * @throws ApiException if the user is not found (ErrorCode.USER_NOT_FOUND)
     * @throws ApiException if the user's current balance is less than {@code amount} (ErrorCode.POINT_INSUFFICIENT)
     */
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

    /**
     * Retrieve the user's current point balance.
     *
     * @param userId the identifier of the user
     * @return the user's current point balance, or {@code BigDecimal.ZERO} if no balance exists
     */
    @Override
    public BigDecimal getCurrentBalance(Long userId) {
        return pointRepository.getCurrentBalance(userId)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Provides detailed point balances for a user including current, available, and soon-to-expire amounts.
     *
     * @param userId the id of the user whose balances are requested
     * @return a PointBalanceResponseDto containing the current balance, the available (non-expired positive) balance, and the amount expiring within 30 days
     */
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

    /**
     * Retrieve the point transaction history for a user in reverse-chronological order.
     *
     * @param userId the identifier of the user whose point history to retrieve
     * @return a list of PointResponseDto representing the user's point transactions ordered by creation time descending; empty if the user has no points
     */
    @Override
    public List<PointResponseDto> getPointHistory(Long userId) {
        return pointRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PointResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve point records associated with a specific payment.
     *
     * @param paymentId the identifier of the payment whose points should be retrieved
     * @return a list of PointResponseDto representing points linked to the payment; empty if none exist
     */
    @Override
    public List<PointResponseDto> getPointsByPayment(Long paymentId) {
        return pointRepository.findByPaymentId(paymentId)
                .stream()
                .map(PointResponseDto::from)
                .collect(Collectors.toList());
    }
}
