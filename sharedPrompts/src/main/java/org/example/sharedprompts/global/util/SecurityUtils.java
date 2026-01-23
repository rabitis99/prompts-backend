package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
     * 이 메서드는 MessageDigest.isEqual()을 사용하여 검증된 상수 시간 비교를 제공합니다.
     * 
     * <p><strong>보안 고려사항:</strong>
     * <ul>
     *   <li>null 입력은 무조건 false를 반환합니다. 인증 우회를 방지하기 위해 null은 불일치로 처리합니다.</li>
     *   <li>호출부에서 null 유효성을 사전에 확인하는 것을 권장합니다.</li>
     *   <li>MessageDigest.isEqual()은 null이나 길이 0일 때 상수-시간 보장이 적용되지 않으므로, null은 비교 전에 걸러야 합니다.</li>
     * </ul>
     * 
     * <p><strong>주의사항:</strong>
     * <ul>
     *   <li>MessageDigest.isEqual()도 길이가 다르면 즉시 반환하므로, 완벽한 상수 시간 비교는 아닙니다.</li>
     *   <li>HMAC, 해시 값 등 고정 길이 값 비교에 적합합니다.</li>
     *   <li>가변 길이 토큰 비교 시에는 길이 정보가 타이밍 공격으로 누출될 수 있습니다.</li>
     * </ul>
     * 
     * <p>사용 사례:
     * <ul>
     *   <li>HMAC 값 비교 (권장)</li>
     *   <li>해시 값 비교</li>
     *   <li>고정 길이 인증 토큰 비교</li>
     * </ul>
     * 
     * @param a 첫 번째 문자열 (null이면 false 반환)
     * @param b 두 번째 문자열 (null이면 false 반환)
     * @return 두 문자열이 동일하면 true, 그렇지 않으면 false (null 입력은 항상 false)
     */
    public static boolean constantTimeEquals(String a, String b) {
        // null 입력은 무조건 false 반환 (보안 우회 방지)
        // OWASP 지침: 인증 비교에서 null 입력은 불일치로 처리해야 함
        if (a == null || b == null) {
            return false;
        }
        
        // MessageDigest.isEqual()을 사용하여 검증된 상수 시간 비교 수행
        // UTF-8 인코딩을 사용하여 byte[]로 변환
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}

