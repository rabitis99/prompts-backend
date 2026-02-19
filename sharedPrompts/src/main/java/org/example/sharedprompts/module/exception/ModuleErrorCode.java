package org.example.sharedprompts.module.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ModuleErrorCode {
    JOB_NOT_FOUND("PD00701", HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다."),
    JOB_ALREADY_COMPLETED("PD00401", HttpStatus.BAD_REQUEST, "이미 완료된 작업입니다."),
    JOB_ALREADY_FAILED("PD00402", HttpStatus.BAD_REQUEST, "이미 실패한 작업입니다."),
    JOB_INVALID_STATUS("PD00403", HttpStatus.BAD_REQUEST, "유효하지 않은 작업 상태입니다."),
    JOB_FORBIDDEN("PD00601", HttpStatus.FORBIDDEN, "해당 작업에 대한 접근 권한이 없습니다."),
    PRODUCTION_NOT_FOUND("PD00703", HttpStatus.NOT_FOUND, "Production 결과를 찾을 수 없습니다."),
    PRODUCTION_FORBIDDEN("PD00602", HttpStatus.FORBIDDEN, "해당 Production 결과에 대한 접근 권한이 없습니다."),
    AI_SERVICE_ERROR("PD01001", HttpStatus.INTERNAL_SERVER_ERROR, "AI 서비스 호출 중 오류가 발생했습니다."),
    AI_PARSE_ERROR("PD01002", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱 중 오류가 발생했습니다."),
    CONTENT_RENDER_ERROR("PD01003", HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠 렌더링 중 오류가 발생했습니다."),
    STORAGE_ERROR("PD01004", HttpStatus.INTERNAL_SERVER_ERROR, "저장소 저장 중 오류가 발생했습니다."),
    FORMAT_CONVERSION_ERROR("PD01005", HttpStatus.INTERNAL_SERVER_ERROR, "형식 변환 중 오류가 발생했습니다."),
    RECOVERY_ERROR("PD01006", HttpStatus.INTERNAL_SERVER_ERROR, "작업 복구 중 오류가 발생했습니다."),
    VALIDATION_ERROR("PD00404", HttpStatus.BAD_REQUEST, "검증에 실패했습니다."),
    COMMAND_FACTORY_NOT_FOUND("PD00702", HttpStatus.NOT_FOUND, "지원하지 않는 Production 타입입니다."),
    IDEMPOTENCY_KEY_ERROR("PD01007", HttpStatus.INTERNAL_SERVER_ERROR, "멱등성 키 생성 중 오류가 발생했습니다."),
    AI_CLIENT_ERROR("PD01008", HttpStatus.INTERNAL_SERVER_ERROR, "AI 클라이언트 호출 중 오류가 발생했습니다."),
    UNSUPPORTED_CONTENT_TYPE("PD00405", HttpStatus.BAD_REQUEST, "지원하지 않는 콘텐츠 타입입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}

