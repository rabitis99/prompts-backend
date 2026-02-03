package org.example.sharedprompts.domain.payment.config;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 카카오페이 설정 Properties
 *
 * <p><strong>검증 전략:</strong>
 * - @ConfigurationProperties: 설정값을 자동으로 바인딩
 * - @ConditionalOnProperty: payment.enabled=true일 때만 빈 로드 (배포 안전성)
 * - @Validated: 부팅 시점에 필수값 검증 (fail-fast)
 * - @NotBlank: secret, approvalUrl, cancelUrl, failUrl는 필수
 *
 * <p>payment.enabled=false면 이 빈이 로드되지 않아 환경변수 없이도 부팅 가능.
 * payment.enabled=true이고 설정값 누락 시 BindException 발생하여 애플리케이션 부팅 실패
 * (런타임이 아닌 부팅 시점에 오류 감지)
 */
@Getter
@Setter
@Component
@Validated
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "payment.kakao")
@NoArgsConstructor
@AllArgsConstructor
public class KakaoPayProperties {

    /**
     * 카카오페이 서비스 가맹점 시크릿 키 (필수)
     */
    @NotBlank(message = "카카오페이 시크릿 키는 필수입니다 (payment.kakao.secret)")
    private String secret;

    /**
     * 카카오페이 가맹점 ID (기본값: TC0ONETIME)
     */
    private String cid = "TC0ONETIME";

    /**
     * 결제 승인 후 리다이렉트 URL (필수)
     */
    @NotBlank(message = "카카오페이 승인 콜백 URL은 필수입니다 (payment.kakao.approval-url)")
    private String approvalUrl;

    /**
     * 결제 취소 시 리다이렉트 URL (필수)
     */
    @NotBlank(message = "카카오페이 취소 콜백 URL은 필수입니다 (payment.kakao.cancel-url)")
    private String cancelUrl;

    /**
     * 결제 실패 시 리다이렉트 URL (필수)
     */
    @NotBlank(message = "카카오페이 실패 콜백 URL은 필수입니다 (payment.kakao.fail-url)")
    private String failUrl;
}






