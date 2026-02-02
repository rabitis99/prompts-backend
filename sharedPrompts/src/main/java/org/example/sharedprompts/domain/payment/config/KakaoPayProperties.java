package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 카카오페이 설정 Properties (Immutable)
 */
@Getter
@Component
public class KakaoPayProperties {

    private final String secret;
    private final String cid;
    private final String approvalUrl;
    private final String cancelUrl;
    private final String failUrl;

    public KakaoPayProperties(
            @Value("${payment.kakao.secret:}") String secret,
            @Value("${payment.kakao.cid:TC0ONETIME}") String cid,
            @Value("${payment.kakao.approval-url:}") String approvalUrl,
            @Value("${payment.kakao.cancel-url:}") String cancelUrl,
            @Value("${payment.kakao.fail-url:}") String failUrl) {
        this.secret = secret;
        this.cid = cid;
        this.approvalUrl = approvalUrl;
        this.cancelUrl = cancelUrl;
        this.failUrl = failUrl;
    }
}


