package org.example.sharedprompts.global.config.async.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.util.SensitiveDataMasker;

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
     * 단일 파라미터를 포맷팅하며 민감 정보를 마스킹
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
        
        // 토큰 마스킹 (Bearer 토큰 또는 token 키워드 포함)
        if (str.length() > TOKEN_DETECTION_LENGTH && 
            (str.contains("Bearer") || str.contains("token"))) {
            return SensitiveDataMasker.maskToken(str);
        }
        
        // 긴 문자열 마스킹
        if (str.length() > LONG_STRING_THRESHOLD) {
            return SensitiveDataMasker.mask(str);
        }
        
        return str;
    }
}

