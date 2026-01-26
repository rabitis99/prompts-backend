package org.example.sharedprompts.global.config.async.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.util.SensitiveDataMasker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 비동기 메서드 파라미터 포맷팅 유틸리티
 * 
 * <p>단일 책임: 파라미터 포맷팅 및 민감 정보 마스킹만 담당
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AsyncParamFormatter {
    
    private static final int TOKEN_DETECTION_LENGTH = 20;
    private static final int LONG_STRING_THRESHOLD = 50;
    
    /**
     * Format a Method into the "ClassName.methodName" representation.
     *
     * @return the formatted method name in the form "ClassName.methodName"
     */
    public static String formatMethodName(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
    
    /**
     * 파라미터 배열을 포맷팅하며 민감 정보를 마스킹
     * 
     * @param params 파라미터 배열
     * @return 포맷팅된 문자열 (예: "[param1, param2]")
     */
    public static String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "[]";
        }
        
        return Arrays.stream(params)
            .map(AsyncParamFormatter::formatSingleParam)
            .collect(Collectors.joining(", ", "[", "]"));
    }
    
    /**
     * Format a single parameter value and mask sensitive information found in its string form.
     *
     * <p>Null parameters produce the literal string "null". For string representations:
     * emails are masked, values that appear to be tokens (long and containing "bearer" or "token")
     * are masked as tokens, and other long strings are masked. Otherwise the parameter's
     * toString() value is returned unchanged.
     *
     * @param param the parameter to format; may be null
     * @return the formatted string with sensitive parts masked where applicable
     */
    private static String formatSingleParam(Object param) {
        if (param == null) {
            return "null";
        }
        
        String str = param.toString();
        
        // 이메일 마스킹
        if (str.contains("@")) {
            return SensitiveDataMasker.maskEmail(str);
        }

        String lowerStr = str.toLowerCase();
        // 토큰 마스킹 (Bearer 토큰 또는 token 키워드 포함)
        if (str.length() > TOKEN_DETECTION_LENGTH &&
                (lowerStr.contains("bearer") || lowerStr.contains("token"))) {
            return SensitiveDataMasker.maskToken(str);
        }
        
        // 긴 문자열 마스킹
        if (str.length() > LONG_STRING_THRESHOLD) {
            return SensitiveDataMasker.mask(str);
        }
        
        return str;
    }
}
