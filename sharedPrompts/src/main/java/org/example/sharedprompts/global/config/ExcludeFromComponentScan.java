package org.example.sharedprompts.global.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메인 애플리케이션 컴포넌트 스캔에서 제외되어야 하는 컴포넌트를 표시하는 마커 애노테이션.
 * 레거시 또는 선택적 컴포넌트에 사용되며, 패키지 경로에 의존하지 않도록 하여 리팩토링 시 스캔 설정 변경을 방지한다.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExcludeFromComponentScan {
}