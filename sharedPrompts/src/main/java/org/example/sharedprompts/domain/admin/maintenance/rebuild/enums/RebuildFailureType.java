package org.example.sharedprompts.domain.admin.maintenance.rebuild.enums;

import lombok.Getter;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

/**
 * 재빌드 작업 실패 타입을 나타내는 enum
 * 예외 타입을 문자열이 아닌 타입 안전한 방식으로 관리
 */
@Getter
public enum RebuildFailureType {

    LOCK("Lock", "락 획득 실패"),
    DATA_ACCESS("DataAccess", "데이터 접근 오류"),
    TRANSACTION("Transaction", "트랜잭션 오류"),
    INTERRUPTED("Interrupted", "작업 중단"),
    ILLEGAL_STATE("IllegalState", "잘못된 상태"),
    ILLEGAL_ARGUMENT("IllegalArgument", "잘못된 인자"),
    NULL_POINTER("NullPointer", "널 포인터 예외"),
    UNKNOWN("Unknown", "알 수 없는 오류");

    private final String typeName;
    private final String description;

    RebuildFailureType(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    /**
     * 예외로부터 RebuildFailureType을 결정
     * 예외 클래스 타입을 기반으로 매핑하여 타입 안전성 확보
     */
    public static RebuildFailureType from(Exception e) {
        if (e == null) {
            return UNKNOWN;
        }

        // 락 관련 예외
        if (e instanceof IllegalStateException) {
            return LOCK;
        }

        // 데이터 접근 예외
        if (e instanceof DataAccessException) {
            return DATA_ACCESS;
        }

        // 트랜잭션 예외
        if (e instanceof TransactionException) {
            return TRANSACTION;
        }

        // 중단 예외
        if (e instanceof InterruptedException) {
            return INTERRUPTED;
        }

        // 잘못된 인자 예외
        if (e instanceof IllegalArgumentException) {
            return ILLEGAL_ARGUMENT;
        }

        // 널 포인터 예외
        if (e instanceof NullPointerException) {
            return NULL_POINTER;
        }

        // 알 수 없는 예외
        return UNKNOWN;
    }

    /**
     * 예외 메시지와 타입을 결합한 에러 메시지 생성
     */
    public String buildErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return String.format("[%s] %s", typeName, message);
        }
        return String.format("[%s] %s", typeName, description);
    }
}
