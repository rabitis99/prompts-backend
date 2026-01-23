package org.example.sharedprompts.auth.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

import java.util.Arrays;
import java.util.List;

import static org.example.sharedprompts.auth.security.CorsConstants.ErrorMessages.ALLOWED_ORIGINS_NOT_SET;
import static org.example.sharedprompts.auth.security.CorsConstants.ErrorMessages.WILDCARD_NOT_ALLOWED_WITH_CREDENTIALS;
import static org.example.sharedprompts.auth.security.CorsConstants.ORIGIN_SEPARATOR;
import static org.example.sharedprompts.auth.security.CorsConstants.WILDCARD_ORIGIN;

/**
 * CORS Origin 검증 유틸리티
 * 
 * CORS 설정에서 사용할 origin 목록을 파싱하고 검증합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CorsOriginValidator {

    /**
     * Origin 설정 문자열을 파싱하여 목록으로 변환
     * 
     * @param originsString CSV 형식의 origin 문자열
     * @return 파싱된 origin 목록
     */
    public static List<String> parseOrigins(String originsString) {
        if (originsString == null || originsString.isBlank()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, ALLOWED_ORIGINS_NOT_SET);
        }

        return Arrays.stream(originsString.split(ORIGIN_SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Origin 목록에서 와일드카드("*") 포함 여부 검증
     * 
     * <p>allowCredentials(true) 사용 시 "*" 패턴이 포함되면 모든 origin을 매칭하며,
     * 사실상 모든 웹사이트에서 인증된 요청을 가능하게 하므로 보안상 위험합니다.
     * 따라서 "*"가 하나라도 포함되면 예외를 발생시킵니다.
     * 
     * @param origins 검증할 origin 목록
     * @param originalConfig 원본 설정 문자열 (에러 메시지용)
     * @throws ApiException "*" 패턴이 포함된 경우
     */
    public static void validateNoWildcard(List<String> origins, String originalConfig) {
        if (origins.contains(WILDCARD_ORIGIN)) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null,
                    String.format(WILDCARD_NOT_ALLOWED_WITH_CREDENTIALS, originalConfig));
        }
    }

    /**
     * Origin 설정 문자열을 파싱하고 검증
     * 
     * @param originsString CSV 형식의 origin 문자열
     * @return 검증된 origin 목록
     * @throws ApiException "*" 패턴이 포함되거나 설정이 비어있는 경우
     */
    public static List<String> parseAndValidate(String originsString) {
        List<String> origins = parseOrigins(originsString);
        validateNoWildcard(origins, originsString);
        return origins;
    }
}

