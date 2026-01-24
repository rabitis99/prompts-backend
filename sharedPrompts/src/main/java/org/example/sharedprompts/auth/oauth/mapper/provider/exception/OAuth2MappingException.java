package org.example.sharedprompts.auth.oauth.mapper.provider.exception;

import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * OAuth2 매핑 예외
 * Provider별 특수 케이스 처리 중 발생하는 예외
 * 상황에 맞는 세분화된 ErrorCode를 사용합니다.
 */
public class OAuth2MappingException extends ApiException {

    /**
     * 사용자 정보가 비어있는 경우
     */
    public static OAuth2MappingException userInfoEmpty(Provider provider) {
        return new OAuth2MappingException(
                ErrorCode.OAUTH2_USER_INFO_EMPTY,
                provider,
                "사용자 정보가 비어있습니다."
        );
    }

    /**
     * Provider ID가 없는 경우
     */
    public static OAuth2MappingException providerIdMissing(Provider provider) {
        return new OAuth2MappingException(
                ErrorCode.OAUTH2_PROVIDER_ID_MISSING,
                provider,
                "사용자 ID가 없습니다."
        );
    }

    /**
     * 사용자 정보 매핑 실패
     */
    public static OAuth2MappingException mappingFailed(Provider provider, String message) {
        return new OAuth2MappingException(
                ErrorCode.OAUTH2_USER_INFO_MAPPING_FAILED,
                provider,
                message
        );
    }

    /**
     * 사용자 정보 매핑 실패 (원인 포함)
     */
    public static OAuth2MappingException mappingFailed(Provider provider, String message, Throwable cause) {
        return new OAuth2MappingException(
                ErrorCode.OAUTH2_USER_INFO_MAPPING_FAILED,
                provider,
                message,
                cause
        );
    }

    /**
     * Provider별 특수 케이스 실패 (일반적인 매핑 실패)
     */
    public OAuth2MappingException(Provider provider, String message) {
        this(ErrorCode.OAUTH2_USER_INFO_MAPPING_FAILED, provider, message);
    }

    /**
     * Provider별 특수 케이스 실패 (원인 포함)
     */
    public OAuth2MappingException(Provider provider, String message, Throwable cause) {
        this(ErrorCode.OAUTH2_USER_INFO_MAPPING_FAILED, provider, message, cause);
    }

    /**
     * 세분화된 ErrorCode를 사용하는 생성자
     */
    private OAuth2MappingException(ErrorCode errorCode, Provider provider, String message) {
        super(
                errorCode,
                String.format("[%s] %s", provider, message)
        );
    }

    /**
     * 세분화된 ErrorCode를 사용하는 생성자 (원인 포함)
     */
    private OAuth2MappingException(ErrorCode errorCode, Provider provider, String message, Throwable cause) {
        super(
                errorCode,
                String.format("[%s] %s - 원인: %s", provider, message, cause != null ? cause.getMessage() : "알 수 없음")
        );
        if (cause != null) {
            initCause(cause);
        }
    }
}

