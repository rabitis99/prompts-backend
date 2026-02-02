package org.example.sharedprompts.domain.payment.provider.kakao.support;

import org.springframework.stereotype.Component;

@Component
public class KakaoPayTidParser {

    public ParsedTid parse(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("paymentKey는 비어 있을 수 없습니다.");
        }

        String tid = paymentKey;
        String pgToken = null;

        // pg_token 추출
        if (paymentKey.contains("pg_token=")) {
            int start = paymentKey.indexOf("pg_token=") + 9;
            int end = paymentKey.indexOf("&", start);
            pgToken = end == -1
                    ? paymentKey.substring(start)
                    : paymentKey.substring(start, end);
        }

        // tid 추출
        if (paymentKey.contains("tid=")) {
            int start = paymentKey.indexOf("tid=") + 4;
            int end = paymentKey.indexOf("&", start);
            tid = end == -1
                    ? paymentKey.substring(start)
                    : paymentKey.substring(start, end);
        } else if (pgToken != null) {
            int end = paymentKey.indexOf("pg_token=");
            if (end > 0) {
                tid = paymentKey.substring(0, end)
                        .replace("?", "")
                        .replace("&", "");
            }
        }

        return new ParsedTid(tid, pgToken);
    }

    /**
     * KakaoPay tid / pg_token 파싱 결과
     */
    public record ParsedTid(
            String tid,
            String pgToken
    ) {}
}

