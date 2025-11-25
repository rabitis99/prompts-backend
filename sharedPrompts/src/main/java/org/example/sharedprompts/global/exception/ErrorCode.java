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
    // 🔹 Auth
    // ==========================
    CONFLICT_EMAIL("AU00601", HttpStatus.FORBIDDEN, "중복된 이메일 입니다.")
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


