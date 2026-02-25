package org.example.sharedprompts.domain.payment.config;

import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentCreator;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentPreparationHandler;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.*;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentQueryRepositoryPort;
import org.example.sharedprompts.domain.payment.application.port.out.webhook.PaymentWebhookProcessingPort;
import org.example.sharedprompts.domain.payment.application.service.impl.*;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 도메인 UseCase 빈 설정
 * 각 UseCase의 구현체를 생성하고 의존성을 주입합니다.
 */
@Configuration
public class PaymentBeanConfiguration {

    /**
     * 결제 승인 UseCase
     * 새로운 포트/어댑터 아키텍처와 기존 업무 로직(레거시 서비스)를 통합합니다.
     */
    @Bean
    public PaymentApprovalUseCase paymentApprovalUseCase(
            PaymentCommandRepositoryPort paymentRepository,
            PaymentValidationService validationService,
            PaymentAmountProcessingService amountProcessingService,
            PaymentCreator paymentCreator,
            PaymentPreparationHandler preparationHandler,
            PaymentLoggingService loggingService,
            UserRepository userRepository
    ) {
        return new DefaultPaymentApprovalService(
                paymentRepository,
                validationService,
                amountProcessingService,
                paymentCreator,
                preparationHandler,
                loggingService,
                userRepository
        );
    }

    /**
     * 결제 확인 UseCase
     */
    @Bean
    public PaymentConfirmationUseCase paymentConfirmationUseCase(
            PaymentCommandRepositoryPort paymentRepository,
            PaymentGatewayPort paymentGateway,
            PaymentEventPublisherPort eventPublisher
    ) {
        return new DefaultPaymentConfirmationService(paymentRepository, paymentGateway, eventPublisher);
    }

    /**
     * 결제 취소 UseCase
     */
    @Bean
    public PaymentCancellationUseCase paymentCancellationUseCase(
            PaymentCommandRepositoryPort paymentRepository,
            PaymentGatewayPort paymentGateway,
            PaymentEventPublisherPort eventPublisher
    ) {
        return new DefaultPaymentCancellationService(paymentRepository, paymentGateway, eventPublisher);
    }

    /**
     * 결제 환불 UseCase
     */
    @Bean
    public PaymentRefundUseCase paymentRefundUseCase(
            PaymentCommandRepositoryPort paymentRepository,
            PaymentGatewayPort paymentGateway,
            PaymentEventPublisherPort eventPublisher
    ) {
        return new DefaultPaymentRefundService(paymentRepository, paymentGateway, eventPublisher);
    }

    /**
     * 결제 상태 조회 UseCase
     */
    @Bean
    public PaymentStatusCheckUseCase paymentStatusCheckUseCase(
            PaymentQueryRepositoryPort paymentRepository
    ) {
        return new DefaultPaymentStatusCheckService(paymentRepository);
    }

    /**
     * 결제 내역 조회 UseCase
     */
    @Bean
    public PaymentHistoryQueryUseCase paymentHistoryQueryUseCase(
            PaymentQueryRepositoryPort paymentRepository
    ) {
        return new DefaultPaymentHistoryQueryService(paymentRepository);
    }

    /**
     * 결제 웹훅 처리 UseCase
     */
    @Bean
    public PaymentWebhookUseCase paymentWebhookUseCase(
            PaymentWebhookProcessingPort webhookProcessingPort
    ) {
        return new DefaultPaymentWebhookHandlingService(webhookProcessingPort);
    }

    @Bean
    public PaymentCommandUseCase paymentCommandUseCase(
            PaymentApprovalUseCase approvalUseCase,
            PaymentCancellationUseCase cancellationUseCase,
            PaymentRefundUseCase refundUseCase,
            PaymentConfirmationUseCase confirmationUseCase
    ) {
        return new PaymentCommandUseCaseImpl(
                approvalUseCase,
                cancellationUseCase,
                refundUseCase,
                confirmationUseCase
        );
    }

    @Bean
    public PaymentQueryUseCase paymentQueryUseCase(
            PaymentStatusCheckUseCase statusCheckUseCase,
            PaymentHistoryQueryUseCase historyQueryUseCase
    ) {
        return new PaymentQueryUseCaseImpl(statusCheckUseCase, historyQueryUseCase);
    }
}
