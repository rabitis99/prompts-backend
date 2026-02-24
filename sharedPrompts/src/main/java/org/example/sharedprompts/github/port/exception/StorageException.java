package org.example.sharedprompts.github.port.exception;

/**
 * Storage 작업 실패.
 * S3 업로드, 다운로드, URL 생성 등에서 발생.
 */
public class StorageException extends RuntimeException {
  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
