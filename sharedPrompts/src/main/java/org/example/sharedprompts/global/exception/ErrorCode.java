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
    RATE_LIMIT_EXCEEDED("CM00403", HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INVALID_REQUEST_INFO("CM00429", HttpStatus.BAD_REQUEST, "요청 정보가 유효하지 않습니다."),
    UNAUTHORIZED("CM00501", HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인 후 다시 시도해주세요."),
    FORBIDDEN("CM00601", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("CM00701", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    NOT_FOUND_ENDPOINT("CM00702", HttpStatus.NOT_FOUND, "엔드포인트를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("CM00703", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다."),
    RATE_LIMIT_CHECK_FAILED("CM01002", HttpStatus.INTERNAL_SERVER_ERROR, "Rate Limit 체크에 실패했습니다."),
    DATA_INTEGRITY_VIOLATION("CM00901", HttpStatus.CONFLICT, "데이터 무결성 제약 조건을 위반했습니다."),
    INTERNAL_SERVER_ERROR("CM01001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // ==========================
    // 🔹 USER
    // ==========================
    USER_NOT_FOUND("US00701", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다. 입력한 정보를 확인해주세요."),
    USER_PROFILE_NOT_FOUND("US00702", HttpStatus.NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."),
    INVALID_PASSWORD("US00401", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다. 올바른 비밀번호를 입력해주세요."),
    INVALID_PASSWORD_STRENGTH("US00403", HttpStatus.BAD_REQUEST, "비밀번호는 최소 10자 이상이며, 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."),
    NOT_LOCAL_USER("US00601", HttpStatus.FORBIDDEN, "자체 회원가입만 가능한 서비스입니다."),
    SAME_AS_CURRENT_PASSWORD("US00402", HttpStatus.BAD_REQUEST, "현재 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요."),

    // ==========================
    // 🔹 Auth
    // ==========================
    LOGIN_FAILED("AU00403", HttpStatus.FORBIDDEN, "이메일 또는 비밀번호가 올바르지 않습니다."),
    CONFLICT_EMAIL("AU00901", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다. 다른 이메일을 사용해주세요."),
    UNAUTHORIZED_TOKEN_ACCESS("AU00503", HttpStatus.UNAUTHORIZED, "해당 토큰은 요청한 사용자의 것이 아닙니다."),
    INVALID_ACCESS_TOKEN("AU00504", HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다. 다시 로그인해주세요."),
    INVALID_REFRESH_TOKEN("AU00505", HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었거나 존재하지 않습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_SECURITY_MISMATCH("AU00506", HttpStatus.UNAUTHORIZED, "리프레시 토큰의 보안 검증에 실패했습니다. 보안을 위해 모든 세션이 무효화되었습니다. 다시 로그인해주세요."),
    TOKEN_VERSION_INCREMENT_FAILED("AU01001", HttpStatus.INTERNAL_SERVER_ERROR, "토큰 버전 증가에 실패했습니다."),

    // ==========================
    // 🔹 OAuth2
    // ==========================
    OAUTH2_INVALID_CODE("AU00401", HttpStatus.BAD_REQUEST, "유효하지 않은 OAuth2 인증 코드입니다. 인증 과정을 다시 시작해주세요."),
    OAUTH2_STATE_MISMATCH("AU00402", HttpStatus.BAD_REQUEST, "OAuth2 state 값이 일치하지 않습니다. 보안을 위해 인증 과정을 다시 시작해주세요."),
    OAUTH2_TOKEN_EXPIRED("AU00501", HttpStatus.UNAUTHORIZED, "OAuth2 임시 토큰이 만료되었습니다. 인증 과정을 다시 시작해주세요."),
    OAUTH2_TOKEN_INVALID("AU00502", HttpStatus.UNAUTHORIZED, "유효하지 않은 OAuth2 토큰입니다. 인증 과정을 다시 시작해주세요."),
    OAUTH2_AUTHENTICATION_FAILED("AU00404", HttpStatus.BAD_REQUEST, "OAuth2 인증에 실패했습니다. 다시 시도해주세요."),
    
    // OAuth2 사용자 정보 매핑 관련
    OAUTH2_PROVIDER_NOT_SUPPORTED("AU00405", HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 제공자입니다."),
    OAUTH2_USER_INFO_EMPTY("AU00406", HttpStatus.BAD_REQUEST, "OAuth2 사용자 정보가 비어있습니다."),
    OAUTH2_PROVIDER_ID_MISSING("AU00407", HttpStatus.BAD_REQUEST, "OAuth2 Provider ID가 없습니다."),
    OAUTH2_USER_INFO_MAPPING_FAILED("AU00408", HttpStatus.BAD_REQUEST, "OAuth2 사용자 정보 매핑에 실패했습니다."),
    OAUTH2_USER_ATTRIBUTES_INVALID("AU00409", HttpStatus.BAD_REQUEST, "OAuth2 사용자 속성이 유효하지 않습니다."),
    OAUTH2_PROVIDER_REQUIRED("AU00410", HttpStatus.BAD_REQUEST, "OAuth2 Provider는 필수입니다."),

    // ==========================
    // 🔹 PROMPT
    // ==========================
    PROMPT_NOT_FOUND("PR00701", HttpStatus.NOT_FOUND, "프롬프트를 찾을 수 없습니다."),
    PROMPT_FORBIDDEN("PR00601", HttpStatus.FORBIDDEN, "해당 프롬프트에 대한 접근 권한이 없습니다."),
    PROMPT_BLOCKED_VIEW("PR00602", HttpStatus.FORBIDDEN, "차단된 사용자의 프롬프트는 조회할 수 없습니다."),
    PROMPT_SEARCH_CONDITION_REQUIRED("PR00402", HttpStatus.BAD_REQUEST, "프롬프트 검색 조건은 필수입니다."),
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
    // 🔹 FAVORITE
    // ==========================
    PROMPT_ALREADY_FAVORITED("FV00401", HttpStatus.BAD_REQUEST, "이미 즐겨찾기에 추가한 프롬프트입니다."),
    PROMPT_FAVORITE_NOT_FOUND("FV00701", HttpStatus.NOT_FOUND, "즐겨찾기 기록이 없습니다."),

    // ==========================
    // 🔹 FOLLOW
    // ==========================
    FOLLOW_IDS_REQUIRED("FW00401", HttpStatus.BAD_REQUEST, "팔로워 ID와 팔로잉 ID는 필수입니다."),
    FOLLOW_ALREADY_EXISTS("FW00402", HttpStatus.BAD_REQUEST, "이미 팔로우 관계가 존재합니다."),
    CANNOT_FOLLOW_SELF("FW00403", HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),
    CANNOT_BLOCK_SELF("FW00407", HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다."),
    FOLLOW_NOT_BLOCKED("FW00404", HttpStatus.BAD_REQUEST, "차단 상태가 아닌 관계는 차단 해제할 수 없습니다."),
    FOLLOW_NOT_PENDING("FW00405", HttpStatus.BAD_REQUEST, "대기 상태가 아닌 관계는 거부할 수 없습니다."),
    FOLLOW_BLOCKED("FW00406", HttpStatus.FORBIDDEN, "차단된 관계는 작업할 수 없습니다."),
    FOLLOW_NOT_FOUND("FW00701", HttpStatus.NOT_FOUND, "팔로우 관계를 찾을 수 없습니다."),

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
    REPORT_TYPE_REQUIRED("RP00404", HttpStatus.BAD_REQUEST, "신고 타입은 필수입니다."),
    REPORT_REPORTER_REQUIRED("RP00405", HttpStatus.BAD_REQUEST, "신고자는 필수입니다."),
    REPORT_TARGET_CONFLICT("RP00406", HttpStatus.BAD_REQUEST, "신고는 프롬프트 또는 댓글 중 하나만 대상으로 해야 합니다."),
    REPORT_PROMPT_MISSING("RP00407", HttpStatus.BAD_REQUEST, "프롬프트 신고는 prompt가 필수입니다."),
    REPORT_COMMENT_MISSING("RP00408", HttpStatus.BAD_REQUEST, "댓글 신고는 comment가 필수입니다."),

    // ==========================
    // 🔹 NOTIFICATION
    // ==========================
    NOTIFICATION_NOT_FOUND("NT00701", HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_PUBLISH_FAILED("NT01001", HttpStatus.INTERNAL_SERVER_ERROR, "알림 발행에 실패했습니다."),

    // ==========================
    // 🔹 ADMIN
    // ==========================
    ADMIN_ONLY("AD00601", HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다."),
    CANNOT_BLOCK_ADMIN("AD00401", HttpStatus.BAD_REQUEST, "관리자는 차단할 수 없습니다."),
    CANNOT_CHANGE_ADMIN_ROLE("AD00402", HttpStatus.BAD_REQUEST, "관리자 권한은 변경할 수 없습니다."),
    CANNOT_DELETE_ADMIN("AD00403", HttpStatus.BAD_REQUEST, "관리자 계정은 삭제할 수 없습니다."),
    CANNOT_MODIFY_SELF("AD00404", HttpStatus.BAD_REQUEST, "자기 자신은 수정할 수 없습니다."),
    LAST_ADMIN_CANNOT_BE_MODIFIED("AD00405", HttpStatus.BAD_REQUEST, "마지막 관리자 계정은 수정할 수 없습니다."),
    INVALID_DATE_RANGE("AD00406", HttpStatus.BAD_REQUEST, "시작일은 종료일보다 이전이어야 합니다."),
    DATE_REQUIRED("AD00411", HttpStatus.BAD_REQUEST, "시작일과 종료일은 필수입니다."),
    STATISTICS_DATE_RANGE_EXCEEDED("AD00412", HttpStatus.BAD_REQUEST, "통계 조회 기간이 최대 허용 기간을 초과했습니다."),
    PAGE_SIZE_EXCEEDED("AD00407", HttpStatus.BAD_REQUEST, "페이징 사이즈는 최대 100까지 가능합니다."),
    SAME_ROLE("AD00408", HttpStatus.BAD_REQUEST, "현재 권한과 동일한 권한으로 변경할 수 없습니다."),
    SAME_VISIBILITY("AD00409", HttpStatus.BAD_REQUEST, "현재 공개 상태와 동일한 상태로 변경할 수 없습니다."),
    FOLLOW_STATUS_REQUIRED("AD00410", HttpStatus.BAD_REQUEST, "팔로우 상태는 필수입니다."),

    // ==========================
    // 🔹 PAYMENT
    // ==========================
    PAYMENT_NOT_FOUND("PY00701", HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    PAYMENT_FORBIDDEN("PY00601", HttpStatus.FORBIDDEN, "해당 결제에 대한 접근 권한이 없습니다."),
    PAYMENT_ALREADY_COMPLETED("PY00401", HttpStatus.BAD_REQUEST, "이미 완료된 결제입니다."),
    PAYMENT_ALREADY_CANCELED("PY00402", HttpStatus.BAD_REQUEST, "이미 취소된 결제입니다."),
    PAYMENT_ALREADY_REFUNDED("PY00403", HttpStatus.BAD_REQUEST, "이미 환불된 결제입니다."),
    PAYMENT_DAILY_LIMIT_EXCEEDED("PY00404", HttpStatus.BAD_REQUEST, "일일 결제 횟수 제한을 초과했습니다."),
    MODULE_DAILY_LIMIT_EXCEEDED("PY00422", HttpStatus.BAD_REQUEST, "해당 모듈의 일일 사용 횟수 제한을 초과했습니다."),
    UNSUPPORTED_MODULE_TYPE("PY00423", HttpStatus.BAD_REQUEST, "지원하지 않는 모듈 타입입니다."),
    PAYMENT_INVALID_STATUS("PY00405", HttpStatus.BAD_REQUEST, "유효하지 않은 결제 상태입니다."),
    PAYMENT_REFUND_AMOUNT_EXCEEDED("PY00406", HttpStatus.BAD_REQUEST, "환불 금액이 환불 가능 금액을 초과했습니다."),
    PAYMENT_PROVIDER_ERROR("PY01001", HttpStatus.INTERNAL_SERVER_ERROR, "결제사 연동 중 오류가 발생했습니다."),
    PAYMENT_WEBHOOK_VERIFICATION_FAILED("PY00407", HttpStatus.BAD_REQUEST, "Webhook 서명 검증에 실패했습니다."),
    PAYMENT_RETRY_EXCEEDED("PY00408", HttpStatus.BAD_REQUEST, "최대 재시도 횟수를 초과했습니다."),
    TIER_NOT_FOUND("PY00702", HttpStatus.NOT_FOUND, "티어를 찾을 수 없습니다."),
    TIER_CHANGE_FORBIDDEN("PY00602", HttpStatus.FORBIDDEN, "티어 변경 권한이 없습니다."),
    SAME_TIER("PY00409", HttpStatus.BAD_REQUEST, "현재 티어와 동일한 티어로 변경할 수 없습니다."),
    EXCHANGE_RATE_ERROR("PY01002", HttpStatus.INTERNAL_SERVER_ERROR, "환율 조회 중 오류가 발생했습니다."),
    POINT_INSUFFICIENT("PY00410", HttpStatus.BAD_REQUEST, "포인트 잔액이 부족합니다."),
    POINT_NOT_FOUND("PY00703", HttpStatus.NOT_FOUND, "포인트 내역을 찾을 수 없습니다."),

    FORBIDDEN_ACCESS("CM00602", HttpStatus.FORBIDDEN, "권한이 없습니다."),
    // ==========================
    // 🔹 PAYMENT 추가
    // ==========================
    PAYMENT_REFUND_AMOUNT_INVALID("PY00411", HttpStatus.BAD_REQUEST, "환불 금액이 올바르지 않습니다."), // 0 이하 또는 null
    PAYMENT_REFUND_NOT_ALLOWED("PY00603", HttpStatus.FORBIDDEN, "환불할 수 없는 결제입니다."),       // 결제 상태상 환불 불가
    PAYMENT_REFUND_ALREADY_PROCESSED("PY00412", HttpStatus.BAD_REQUEST, "이미 처리된 환불입니다."), // 이미 환불 완료
    PAYMENT_REFUND_PARTIALLY_ALLOWED("PY00413", HttpStatus.BAD_REQUEST, "일부 금액만 환불 가능합니다."), // 비즈니스 정책용
    PAYMENT_AMOUNT_MISMATCH("PY00414", HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_AMOUNT_INVALID("PY00420", HttpStatus.BAD_REQUEST, "결제 금액이 올바르지 않습니다."), // null 또는 0 이하
    PAYMENT_ORDER_ID_MISMATCH("PY00415", HttpStatus.BAD_REQUEST, "주문 ID가 일치하지 않습니다."),
    PAYMENT_CURRENCY_MISMATCH("PY00416", HttpStatus.BAD_REQUEST, "통화 코드가 일치하지 않습니다."),
    PAYMENT_WEBHOOK_SIGNATURE_INVALID("PY00417", HttpStatus.BAD_REQUEST, "Webhook 서명이 유효하지 않습니다."),
    PAYMENT_CANCEL_FAILED("PY00418", HttpStatus.BAD_REQUEST, "결제 취소에 실패했습니다."),
    PAYMENT_REFUND_FAILED("PY00419", HttpStatus.BAD_REQUEST, "결제 환불에 실패했습니다."),
    PAYMENT_PROVIDER_RESPONSE_INVALID("PY00421", HttpStatus.BAD_REQUEST, "결제사 응답이 유효하지 않습니다."),

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
