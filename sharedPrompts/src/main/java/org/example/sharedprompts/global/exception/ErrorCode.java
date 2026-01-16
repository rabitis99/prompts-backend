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
    INVALID_PASSWORD("US00401", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    NOT_LOCAL_USER("US00601", HttpStatus.FORBIDDEN, "자체 회원가입만 가능한 서비스입니다."),
    SAME_AS_CURRENT_PASSWORD("US00402", HttpStatus.BAD_REQUEST, "현재 비밀번호와 동일합니다."),

    // ==========================
    // 🔹 Auth
    // ==========================
    CONFLICT_EMAIL("AU00901", HttpStatus.CONFLICT, "중복된 이메일 입니다."),
    UNAUTHORIZED_TOKEN_ACCESS("AU00503", HttpStatus.UNAUTHORIZED, "해당 토큰은 요청한 사용자의 것이 아닙니다."),
    INVALID_ACCESS_TOKEN("AU00504", HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN("AU00505", HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),

    // ==========================
    // 🔹 OAuth2
    // ==========================
    OAUTH2_INVALID_CODE("AU00401", HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    OAUTH2_STATE_MISMATCH("AU00402", HttpStatus.BAD_REQUEST, "state 값이 일치하지 않습니다."),
    OAUTH2_TOKEN_EXPIRED("AU00501", HttpStatus.UNAUTHORIZED, "임시 토큰이 만료되었습니다."),
    OAUTH2_TOKEN_INVALID("AU00502", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    // ==========================
    // 🔹 PROMPT
    // ==========================
    PROMPT_NOT_FOUND("PR00701", HttpStatus.NOT_FOUND, "프롬프트를 찾을 수 없습니다."),
    PROMPT_FORBIDDEN("PR00601", HttpStatus.FORBIDDEN, "해당 프롬프트에 대한 접근 권한이 없습니다."),
    AI_GENERATION_FAILED("PR00401", HttpStatus.BAD_REQUEST, "프롬프트 생성에 실패하였습니다."),
    AI_RESPONSE_NO_CANDIDATES("PR01001", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답에 후보가 없습니다."),
    AI_RESPONSE_CANDIDATE_NULL("PR01002", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답의 후보가 null입니다."),
    AI_RESPONSE_NO_CONTENT_PARTS("PR01003", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답에 콘텐츠 파트가 없습니다."),
    AI_RESPONSE_PART_TEXT_NULL("PR01004", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답의 파트 텍스트가 null입니다."),

    // ==========================
    // 🔹 COMMENT
    // ==========================
    COMMENT_NOT_FOUND("CO00702", HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_BELONG_TO_PROMPT("CO00703", HttpStatus.FORBIDDEN, "댓글이 해당 프롬프트에 속하지 않습니다."),
    COMMENT_FORBIDDEN("CO00602", HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다."),

    // ==========================
    // 🔹 LIKE
    // ==========================
    PROMPT_ALREADY_LIKED("LK00401", HttpStatus.BAD_REQUEST, "이미 게시글을 좋아요 했습니다."),
    PROMPT_LIKE_NOT_FOUND("LK00701", HttpStatus.NOT_FOUND, "게시글 좋아요 기록이 없습니다."),

    COMMENT_ALREADY_LIKED("LK00402", HttpStatus.BAD_REQUEST, "이미 댓글을 좋아요 했습니다."),
    COMMENT_LIKE_NOT_FOUND("LK00702", HttpStatus.NOT_FOUND, "댓글 좋아요 기록이 없습니다."),

    // ==========================
    // 🔹 TAG
    // ==========================
    TAG_CREATION_FAILED("TG01001", HttpStatus.INTERNAL_SERVER_ERROR, "태그 생성 중 내부 오류가 발생했습니다."),

    // ==========================
    // 🔹 REPORT
    // ==========================
    REPORT_NOT_FOUND("RP00701", HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."),
    REPORT_ALREADY_EXISTS("RP00401", HttpStatus.BAD_REQUEST, "이미 신고한 콘텐츠입니다."),
    REPORT_ALREADY_PROCESSED("RP00402", HttpStatus.BAD_REQUEST, "이미 처리된 신고입니다."),
    CANNOT_REPORT_OWN_CONTENT("RP00403", HttpStatus.BAD_REQUEST, "자신의 콘텐츠는 신고할 수 없습니다."),

    ;
    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    private static final Map<String, ErrorCode> BY_CODE =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(ErrorCode::getCode, Function.identity()));

    public static ErrorCode fromCode(String code) {
        return BY_CODE.get(code);
    }
}
