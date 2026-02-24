package org.example.sharedprompts.github.port.exception;

/**
 * Persistence (DB) 작업 실패.
 * JPA 쿼리, UPSERT, 트랜잭션 등에서 발생.
 */
public class PersistenceException extends RuntimeException {
  public PersistenceException(String message) {
    super(message);
  }

  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
