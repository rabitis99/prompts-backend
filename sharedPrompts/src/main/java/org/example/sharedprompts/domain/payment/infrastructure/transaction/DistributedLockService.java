package org.example.sharedprompts.domain.payment.infrastructure.transaction;

import java.time.Duration;
import java.util.function.Supplier;

public interface DistributedLockService {

    <T> T executeWithLock(String lockKey, Supplier<T> task);

    <T> T executeWithLock(String lockKey, Duration lockAtMostFor, Duration lockAtLeastFor, Supplier<T> task);

    default void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, () -> {
            task.run();
            return null;
        });
    }

    boolean tryLock(String lockKey, Duration lockAtMostFor);

    void unlock(String lockKey);

    default String createLockKey(String prefix, Long id) {
        return prefix + ":" + id;
    }

    class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String lockKey) {
            super("Failed to acquire lock: " + lockKey);
        }

        public LockAcquisitionException(String lockKey, Throwable cause) {
            super("Failed to acquire lock: " + lockKey, cause);
        }
    }
}
