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

    public KakaoPayProperties(
            @Value("${payment.kakao.secret:}") String secret,
            @Value("${payment.kakao.cid:TC0ONETIME}") String cid) {
        this.secret = secret;
        this.cid = cid;
    }
}

