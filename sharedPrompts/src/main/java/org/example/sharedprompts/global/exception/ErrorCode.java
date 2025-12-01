package org.example.sharedprompts.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==========================
    // 🔹 Common
    // ==========================
    BAD_REQUEST("CM00401", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE("CM00402", HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다."),
    UNAUTHORIZED("CM00501", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("CM00601", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("CM00701", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    NOT_FOUND_ENDPOINT("CM00702", HttpStatus.NOT_FOUND, "엔드포인트를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("CM00703", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다."),
    INTERNAL_SERVER_ERROR("CM01001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // ==========================
    // 🔹 USER
    // ==========================
    USER_NOT_FOUND("US00701", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ==========================
    // 🔹 Auth
    // ==========================
    CONFLICT_EMAIL("AU00901", HttpStatus.CONFLICT, "중복된 이메일 입니다."),
    UNAUTHORIZED_TOKEN_ACCESS("AU00503", HttpStatus.UNAUTHORIZED, "해당 토큰은 요청한 사용자의 것이 아닙니다."),
    INVALID_ACCESS_TOKEN("AU00403", HttpStatus.BAD_REQUEST, "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN("AU00403", HttpStatus.BAD_REQUEST, "유효하지 않은 액세스 토큰입니다."),
    // ==========================
    // 🔹 OAuth2
    // ==========================
    OAUTH2_INVALID_CODE("AU00401", HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    OAUTH2_STATE_MISMATCH("AU00402", HttpStatus.BAD_REQUEST, "state 값이 일치하지 않습니다."),
    OAUTH2_TOKEN_EXPIRED("AU00501", HttpStatus.UNAUTHORIZED, "임시 토큰이 만료되었습니다."),
    OAUTH2_TOKEN_INVALID("AU00502", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    private static final Map<String, ErrorCode> BY_CODE =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(ErrorCode::getCode, Function.identity()));

    public static ErrorCode fromCode(String code) {
        return BY_CODE.get(code);
    }
}
