package org.example.sharedprompts.domain.payment.provider.paypal.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PayPal API 응답 파서
 *
 * <p>단일 책임: PayPal API 응답에서 필요한 정보 추출
 */
@Slf4j
@Component
public class PayPalResponseParser {

    /**
     * 승인 URL 추출 (approve link)
     */
    public String extractApproveUrl(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> links = (List<Map<String, Object>>) body.get("links");
        if (links != null) {
            for (Map<String, Object> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    return (String) link.get("href");
                }
            }
        }
        return null;
    }

    /**
     * Capture 정보 추출 (금액, 통화)
     */
    public CaptureInfo extractCaptureInfo(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) body.get("purchase_units");
        if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
            if (payments != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                if (captures != null && !captures.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> amountObj = (Map<String, Object>) captures.get(0).get("amount");
                    if (amountObj != null) {
                        String currency = (String) amountObj.get("currency_code");
                        if (currency == null || currency.isBlank()) {
                            throw new RuntimeException("PayPal 응답에서 currency_code가 없습니다");
                        }
                        Object valueObj = amountObj.get("value");
                        if (valueObj == null) {
                            throw new RuntimeException("PayPal 응답에서 capture amount value가 null입니다");
                        }
                        BigDecimal amount = new BigDecimal(valueObj.toString());
                        return new CaptureInfo(amount, currency);
                    }
                }
            }
        }
        throw new RuntimeException("PayPal 응답에서 capture 정보를 찾을 수 없습니다");
    }

    /**
     * 주문 정보 추출 (금액, 통화)
     */
    public OrderInfo extractOrderInfo(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) body.get("purchase_units");
        if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> amount = (Map<String, Object>) purchaseUnits.get(0).get("amount");
            if (amount != null) {
                String currency = (String) amount.get("currency_code");
                if (currency == null || currency.isBlank()) {
                    throw new RuntimeException("PayPal 응답에서 currency_code가 없습니다");
                }
                Object valueObj = amount.get("value");
                if (valueObj == null) {
                    throw new RuntimeException("PayPal 응답에서 order amount value가 null입니다");
                }
                BigDecimal orderAmount = new BigDecimal(valueObj.toString());
                return new OrderInfo(orderAmount, currency);
            }
        }
        throw new RuntimeException("PayPal 응답에서 주문 정보를 찾을 수 없습니다");
    }

    /**
     * Authorization ID 추출
     */
    public String extractAuthorizationId(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) body.get("purchase_units");
        if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
            if (payments != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> authorizations = (List<Map<String, Object>>) payments.get("authorizations");
                if (authorizations != null && !authorizations.isEmpty()) {
                    return (String) authorizations.get(0).get("id");
                }
            }
        }
        return null;
    }

    /**
     * Capture ID 추출
     */
    public String extractCaptureId(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) body.get("purchase_units");
        if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
            if (payments != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                if (captures != null && !captures.isEmpty()) {
                    return (String) captures.get(0).get("id");
                }
            }
        }
        return null;
    }

    /**
     * 환불 금액 추출
     */
    public BigDecimal extractRefundedAmount(Map<String, Object> body, BigDecimal defaultAmount) {
        if (body.get("amount") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> refundAmountMap = (Map<String, Object>) body.get("amount");
            if (refundAmountMap != null && refundAmountMap.get("value") != null) {
                return new BigDecimal(refundAmountMap.get("value").toString());
            }
        }
        return defaultAmount;
    }

    /**
     * 승인 시간 파싱
     */
    public Instant parseApprovedAt(Map<String, Object> body) {
        try {
            Object createTimeObj = body.get("create_time");
            if (createTimeObj != null) {
                String createTimeStr = createTimeObj.toString();
                return OffsetDateTime.parse(createTimeStr, DateTimeFormatter.ISO_DATE_TIME).toInstant();
            }
        } catch (Exception e) {
            log.warn("승인 시간 파싱 실패: {}", e.getMessage());
        }
        return Instant.now();
    }

    /**
     * 환불 시간 파싱
     */
    public Instant parseRefundedAt(Map<String, Object> body) {
        try {
            // PayPal 환불 응답에서 create_time 또는 update_time 사용
            Object createTimeObj = body.get("create_time");
            if (createTimeObj != null) {
                String createTimeStr = createTimeObj.toString();
                return OffsetDateTime.parse(createTimeStr, DateTimeFormatter.ISO_DATE_TIME).toInstant();
            }
            // create_time이 없으면 update_time 시도
            Object updateTimeObj = body.get("update_time");
            if (updateTimeObj != null) {
                String updateTimeStr = updateTimeObj.toString();
                return OffsetDateTime.parse(updateTimeStr, DateTimeFormatter.ISO_DATE_TIME).toInstant();
            }
        } catch (Exception e) {
            log.warn("환불 시간 파싱 실패: {}", e.getMessage());
        }
        return Instant.now();
    }

    // Helper Records
    public record CaptureInfo(BigDecimal amount, String currency) {}
    public record OrderInfo(BigDecimal amount, String currency) {}
}
