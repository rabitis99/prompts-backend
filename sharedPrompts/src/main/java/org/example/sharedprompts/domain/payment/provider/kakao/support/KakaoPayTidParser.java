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

        // pg_token 추출 (빈 값은 null로 통일)
        if (paymentKey.contains("pg_token=")) {
            int start = paymentKey.indexOf("pg_token=") + 9;
            int end = paymentKey.indexOf("&", start);
            String extracted = end == -1
                    ? paymentKey.substring(start)
                    : paymentKey.substring(start, end);
            pgToken = extracted.isBlank() ? null : extracted;
        }

        // tid 추출
        if (paymentKey.contains("tid=")) {
            int start = paymentKey.indexOf("tid=") + 4;
            int end = paymentKey.indexOf("&", start);
            tid = end == -1
                    ? paymentKey.substring(start)
                    : paymentKey.substring(start, end);
        } else if (pgToken != null) {
            // tid 파라미터가 없고 pg_token만 있는 경우, tid가 누락된 것으로 간주
            // fallback 로직은 제거하고 tid를 paymentKey 전체로 유지 (또는 명시적 오류 처리)
            // 실제 tid가 없으면 파싱 실패로 처리하는 것이 안전함
            throw new IllegalArgumentException("tid 파라미터가 누락되었습니다. paymentKey에 tid= 파라미터가 필요합니다.");
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

