package org.example.sharedprompts.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 전용 메서드/클래스에 사용하는 어노테이션
 * 이 어노테이션이 적용된 메서드는 ROLE_ADMIN 권한을 가진 사용자만 접근할 수 있습니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
}

