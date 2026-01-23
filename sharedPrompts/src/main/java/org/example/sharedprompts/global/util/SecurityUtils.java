package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
     * 이 메서드는 모든 문자를 비교하여 응답 시간이 비교 결과에 의존하지 않도록 합니다.
     * 
     * <p>사용 사례:
     * <ul>
     *   <li>HMAC 값 비교</li>
     *   <li>인증 토큰 비교</li>
     *   <li>기타 보안에 중요한 문자열 비교</li>
     * </ul>
     * 
     * @param a 첫 번째 문자열
     * @param b 두 번째 문자열
     * @return 두 문자열이 동일하면 true, 그렇지 않으면 false
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
}

