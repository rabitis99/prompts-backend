package org.example.sharedprompts.module.exception.translator;

import org.example.sharedprompts.module.exception.BaseException;

/**
 * 예외 매핑(번역) 책임 분리.
 * <p>
 * - 외부(인프라/서드파티) 예외를 module의 {@link BaseException} + ErrorCode 체계로 통일합니다.
 * - "추측 기반" 매핑을 피하기 위해, 명확히 구분 가능한 예외 타입 위주로만 매핑합니다.
 */
public interface ExceptionTranslator {

    BaseException translate(Throwable t);
}
