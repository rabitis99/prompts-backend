package org.example.sharedprompts.github.port.exception;

/**
 * 템플릿 해석/로드 실패.
 * Prompt ID 미존재, 템플릿 내용 null/blank 등에서 발생.
 */
public class TemplateException extends RuntimeException {
  public TemplateException(String message) {
    super(message);
  }

  public TemplateException(String message, Throwable cause) {
    super(message, cause);
  }
}
