package org.example.sharedprompts.global.util;

import java.util.regex.Pattern;

/**
 * 민감 정보 마스킹 유틸리티
 *
 * <p>로그에 민감 정보가 노출되지 않도록 마스킹 처리
 * - 카드번호, 이메일, 전화번호, 계좌번호 등
 */
public class SensitiveDataMasker {

    private static final String MASK_CHAR = "*";

    // 이메일 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );

    // 전화번호 패턴 (한국 형식: 010-1234-5678, 01012345678 등)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\d{2,3})[-.]?(\\d{3,4})[-.]?(\\d{4})"
    );

    // 카드번호 패턴 (16자리 숫자, 하이픈 포함 가능)
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}"
    );

    // 계좌번호 패턴 (10-14자리 숫자)
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
            "\\d{10,14}"
    );

    /**
     * 이메일 마스킹
     * 예: user@example.com → u***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskString(email, 0, email.length());
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        // 로컬 파트 마스킹 (첫 글자만 표시)
        String maskedLocal = maskString(localPart, 1, localPart.length());
        return maskedLocal + "@" + domain;
    }

    /**
     * 전화번호 마스킹
     * 예: 010-1234-5678 → 010-****-5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        // 숫자만 추출
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) {
            return maskString(phone, 0, phone.length());
        }

        // 중간 부분 마스킹
        int visibleStart = Math.min(3, digits.length() / 3);
        int visibleEnd = digits.length() - 4;

        StringBuilder masked = new StringBuilder();
        int digitIndex = 0;
        for (char c : phone.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digitIndex < visibleStart || digitIndex >= visibleEnd) {
                    masked.append(c);
                } else {
                    masked.append(MASK_CHAR);
                }
                digitIndex++;
            } else {
                masked.append(c);
            }
        }

        return masked.toString();
    }

    /**
     * 카드번호 마스킹
     * 예: 1234-5678-9012-3456 → 1234-****-****-3456
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return cardNumber;
        }

        // 숫자만 추출
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return maskString(cardNumber, 0, cardNumber.length());
        }

        // 원본 형식 유지 (하이픈 등)
        StringBuilder masked = new StringBuilder();
        int digitIndex = 0;
        for (char c : cardNumber.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digitIndex < 4 || digitIndex >= digits.length() - 4) {
                    masked.append(c);
                } else {
                    masked.append(MASK_CHAR);
                }
                digitIndex++;
            } else {
                masked.append(c);
            }
        }

        return masked.toString();
    }

    /**
     * 계좌번호 마스킹
     * 예: 1234567890123 → 1234-****-****-3
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return accountNumber;
        }

        // 숫자만 추출
        String digits = accountNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 6) {
            return maskString(accountNumber, 0, accountNumber.length());
        }

        // 앞 4자리와 뒤 1자리만 표시
        int visibleStart = 4;
        int visibleEnd = digits.length() - 1;

        StringBuilder masked = new StringBuilder();
        int digitIndex = 0;
        for (char c : accountNumber.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digitIndex < visibleStart || digitIndex >= visibleEnd) {
                    masked.append(c);
                } else {
                    masked.append(MASK_CHAR);
                }
                digitIndex++;
            } else {
                masked.append(c);
            }
        }

        return masked.toString();
    }

    /**
     * 일반 문자열 마스킹
     * 
     * @param str 마스킹할 문자열
     * @param visibleStart 표시할 시작 위치
     * @param visibleEnd 표시할 끝 위치
     * @return 마스킹된 문자열
     */
    public static String maskString(String str, int visibleStart, int visibleEnd) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (visibleStart >= str.length() || visibleEnd <= visibleStart) {
            return MASK_CHAR.repeat(Math.min(str.length(), 8));
        }

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (i >= visibleStart && i < visibleEnd) {
                masked.append(MASK_CHAR);
            } else {
                masked.append(str.charAt(i));
            }
        }

        return masked.toString();
    }

    /**
     * 문자열에서 민감 정보를 자동으로 감지하여 마스킹
     * 
     * @param text 원본 텍스트
     * @return 마스킹된 텍스트
     */
    public static String maskSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // 카드번호 마스킹
        result = CARD_PATTERN.matcher(result).replaceAll(matchResult -> 
            maskCardNumber(matchResult.group())
        );

        // 이메일 마스킹
        result = EMAIL_PATTERN.matcher(result).replaceAll(matchResult -> 
            maskEmail(matchResult.group())
        );

        // 전화번호 마스킹
        result = PHONE_PATTERN.matcher(result).replaceAll(matchResult -> 
            maskPhone(matchResult.group())
        );

        // 계좌번호 마스킹
        result = ACCOUNT_PATTERN.matcher(result).replaceAll(matchResult -> 
            maskAccountNumber(matchResult.group())
        );

        return result;
    }

    /**
     * 사용자 ID 마스킹 (로그용)
     * 예: 12345 → 12***
     */
    public static String maskUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        String userIdStr = String.valueOf(userId);
        if (userIdStr.length() <= 2) {
            return MASK_CHAR.repeat(userIdStr.length());
        }
        return userIdStr.substring(0, 2) + MASK_CHAR.repeat(Math.min(userIdStr.length() - 2, 3));
    }

    /**
     * 결제 금액 마스킹 (선택적, 필요시 사용)
     * 일반적으로 금액은 마스킹하지 않지만, 필요시 사용 가능
     */
    public static String maskAmount(String amount) {
        // 금액은 일반적으로 마스킹하지 않음
        // 필요시 구현
        return amount;
    }

    /**
     * 토큰 마스킹
     * 예: Bearer abc123xyz → Bearer abc***xyz
     * 예: token123456789 → token***789
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }

        // "Bearer " 접두사 처리
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            String prefix = token.substring(0, 7);
            String tokenValue = token.substring(7);
            if (tokenValue.length() <= 6) {
                return prefix + MASK_CHAR.repeat(Math.min(tokenValue.length(), 6));
            }
            return prefix + tokenValue.substring(0, 3) + MASK_CHAR.repeat(Math.min(tokenValue.length() - 6, 6)) + tokenValue.substring(tokenValue.length() - 3);
        }

        // 일반 토큰 마스킹 (앞 4자리, 뒤 3자리만 표시)
        if (token.length() <= 7) {
            return MASK_CHAR.repeat(Math.min(token.length(), 8));
        }
        return token.substring(0, 4) + MASK_CHAR.repeat(Math.min(token.length() - 7, 6)) + token.substring(token.length() - 3);
    }

    /**
     * 일반 문자열 마스킹 (간단한 버전)
     * 긴 문자열의 경우 앞뒤 일부만 표시
     */
    public static String mask(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.length() <= 10) {
            return MASK_CHAR.repeat(Math.min(str.length(), 8));
        }

        // 앞 4자리, 뒤 4자리만 표시
        return str.substring(0, 4) + MASK_CHAR.repeat(Math.min(str.length() - 8, 8)) + str.substring(str.length() - 4);
    }
}
