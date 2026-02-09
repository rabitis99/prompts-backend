package org.example.sharedprompts.infra.delivery.email;

import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DefaultDeliveryResult;
import org.example.sharedprompts.domain.delivery.api.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 이메일을 전송하는 Sender 구현체.
 * Delivery 계층에서 사용되며, 외부 이메일 서비스 API 호출을 담당한다.
 */
@Component
public class EmailSenderImpl implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderImpl.class);
    
    /**
     * 이메일을 전송한다.
     */
    public DeliveryResult send(String emailContent, DeliveryContext context) {
        if (emailContent == null) {
            log.warn("이메일 내용이 null입니다: userId={}", context.getUserId());
            return DefaultDeliveryResult.failure("Email content is null");
        }
        
        String recipient = context.getAttribute("recipient", String.class);
        String subject = context.getAttribute("subject", String.class);
        
        log.info("이메일 전송 시도: userId={}, recipient={}, subject={}, contentLength={}", 
                context.getUserId(), recipient, subject, emailContent.length());
        
        // TODO: 실제 이메일 서비스 API 연동 구현
        // - SMTP 서버 연동
        // - SendGrid, AWS SES 등 외부 서비스 연동
        
        // 현재는 시뮬레이션으로 성공 반환
        log.info("이메일 전송 완료 (시뮬레이션): recipient={}", recipient);
        return DefaultDeliveryResult.success();
    }
}

