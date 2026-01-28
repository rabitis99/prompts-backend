package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * 보안 관련 유틸리티 클래스
 *
 * 보안에 중요한 연산들을 제공합니다.
 * 모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtils {

    /**
     * 상수 시간 문자열 비교 (타이밍 공격 방지)
     *
     * <p>String.equals()는 첫 번째 불일치 문자에서 즉시 반환하므로 타이밍 공격에 취약합니다.
     * 이 메서드는 길이 불일치 여부와 관계없이 동일한 연산 경로를 유지하여
     * 길이 정보 및 비교 결과에 대한 타이밍 정보 누출을 방지합니다.
     *
     * <p><strong>보안 고려사항:</strong>
     * <ul>
     *   <li>null 입력은 무조건 false를 반환합니다. 인증 우회를 방지하기 위해 null은 불일치로 처리합니다.</li>
     *   <li>길이가 다른 경우에도 더미 비교를 수행하여 조기 반환을 방지합니다.</li>
     *   <li>HMAC, 해시 값, 고정 길이 인증 토큰 비교에 적합합니다.</li>
     * </ul>
     *
     * <p><strong>구현 상세:</strong>
     * <ul>
     *   <li>두 입력의 최대 길이를 기준으로 루프를 수행합니다.</li>
     *   <li>길이가 짧은 쪽은 0 바이트로 패딩하여 비교합니다.</li>
     *   <li>길이 차이 및 바이트 차이를 XOR 누적 방식으로 계산합니다.</li>
     * </ul>
     *
     * @param a 첫 번째 문자열 (null이면 false 반환)
     * @param b 두 번째 문자열 (null이면 false 반환)
     * @return 두 문자열이 동일하면 true, 그렇지 않으면 false (null 입력은 항상 false)
     */
    public static boolean constantTimeEquals(String a, String b) {
        // null 입력은 무조건 false 반환 (보안 우회 방지)
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        int maxLen = Math.max(aBytes.length, bBytes.length);

        // 길이 차이를 결과에 반영 (길이 정보도 비교에 포함)
        int result = aBytes.length ^ bBytes.length;

        // 길이가 달라도 동일한 횟수로 비교 수행
        for (int i = 0; i < maxLen; i++) {
            byte x = i < aBytes.length ? aBytes[i] : 0;
            byte y = i < bBytes.length ? bBytes[i] : 0;
            result |= x ^ y;
        }

        return result == 0;
    }
}
