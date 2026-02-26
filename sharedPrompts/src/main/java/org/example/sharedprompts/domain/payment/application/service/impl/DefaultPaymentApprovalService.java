package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentCreator;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentPreparationHandler;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.application.port.in.command.ApprovePaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentApprovalResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentApprovalUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 승인 유스케이스 구현
 *
 * - 일일 한도 검증
 * - 금액 처리 (환율 변환, 포인트)
 * - 결제 준비 처리 (PG 준비 API)
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultPaymentApprovalService implements PaymentApprovalUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentValidationService validationService;
    private final PaymentAmountProcessingService amountProcessingService;
    private final PaymentCreator paymentCreator;
    private final PaymentPreparationHandler preparationHandler;
    private final PaymentLoggingService loggingService;
    private final UserRepository userRepository;

    @Override
    public PaymentApprovalResult approve(ApprovePaymentCommand command) {
        log.info("결제 승인 시작: userId={}, amount={}, currency={}, method={}",
                command.getUserId(), command.getAmount(), command.getCurrency(), command.getPaymentMethod());

        Payment payment;
        AmountProcessingResult amountResult;

        try {
            // 1. 사용자 조회
            User user = userRepository.findById(command.getUserId())
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            // 2. 일일 한도 검증
            validationService.validateDailyLimit(command.getUserId(), user.getTier());

            // 3. 금액 처리 (환율 변환, 포인트 확인)
            amountResult = amountProcessingService.processPaymentAmount(
                    command.getUserId(),
                    command.getAmount(),
                    command.getCurrency(),
                    command.getUsePointAmount()
            );

            // 4. 결제 요청 DTO 생성
            PaymentRequestDto requestDto = PaymentRequestDto.builder()
                    .amount(command.getAmount())
                    .currency(command.getCurrency())
                    .paymentMethod(command.getPaymentMethod())
                    .usePointAmount(command.getUsePointAmount())
                    .userType(command.getUserType())
                    .metadata(command.getMetadata())
                    .build();

            // 5. 결제 엔티티 생성 및 저장
            payment = paymentCreator.createPayment(user, requestDto, amountResult);
            payment = paymentRepository.save(payment);
            log.info("결제 생성 완료: paymentId={}", payment.getId());

            // 6. 결제 준비 처리 (PG 준비 API)
            payment = preparationHandler.processPreparationIfNeeded(payment, amountResult, command.getUserId(), requestDto);
            payment = paymentRepository.save(payment);

            // 7. 로깅
            loggingService.logPaymentRequest(payment);

            log.info("결제 승인 완료: paymentId={}, status={}", payment.getId(), payment.getStatus());

            return PaymentApprovalResult.builder()
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .externalPaymentId(payment.getExternalPaymentId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .build();

        } catch (Exception e) {
            log.error("결제 승인 중 오류 발생: userId={}, 오류: {}", command.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
