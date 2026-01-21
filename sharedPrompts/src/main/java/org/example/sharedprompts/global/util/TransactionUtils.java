package org.example.sharedprompts.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 관련 유틸리티
 * - 트랜잭션 커밋 후 작업을 실행하기 위한 헬퍼 메서드 제공
 */
@Slf4j
public final class TransactionUtils {

    private TransactionUtils() {
        // 유틸리티 클래스는 인스턴스화 불가
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * 트랜잭션 커밋 후 실행할 작업을 등록
     * - 활성 트랜잭션과 동기화가 모두 활성화되어 있을 때만 등록
     * - 그렇지 않으면 즉시 실행
     *
     * @param runnable 트랜잭션 커밋 후 실행할 작업
     */
    public static void executeAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive() 
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            executeSafely(runnable);
                        }
                    }
            );
        } else {
            // 트랜잭션이 없거나 동기화가 비활성이면 즉시 실행
            executeSafely(runnable);
        }
    }

    /**
     * 예외가 발생해도 메인 트랜잭션에 영향을 주지 않도록 안전하게 실행
     *
     * @param runnable 실행할 작업
     */
    private static void executeSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Error executing after-commit task", e);
            // 예외를 다시 던지지 않아 메인 트랜잭션에 영향을 주지 않음
        }
    }
}

