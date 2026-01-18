package org.example.sharedprompts.global.annotation;

import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 로깅을 자동으로 수행하는 어노테이션
 * 
 * 이 어노테이션이 적용된 메서드는 AOP를 통해 자동으로 감사 로그가 기록됩니다.
 * 메서드 실행 전후 상태를 추적하여 변경 이력을 기록할 수 있습니다.
 * 
 * 사용 예시:
 * <pre>
 * {@code
 * @AuditLogging(
 *     entityType = AuditEntityType.USER,
 *     action = AuditAction.BLOCK,
 *     entityIdParam = "userId",
 *     description = "사용자 차단: userId={userId}"
 * )
 * public void blockUser(Long userId, UserBlockRequestDto requestDto) {
 *     // 메서드 구현
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogging {

    /**
     * 대상 엔티티 타입
     */
    AuditEntityType entityType();

    /**
     * 수행된 작업 타입
     */
    AuditAction action();

    /**
     * 엔티티 ID를 추출할 파라미터 이름
     * 메서드 파라미터 중 엔티티 ID가 포함된 파라미터의 이름을 지정합니다.
     * 예: "userId", "promptId", "reportId" 등
     */
    String entityIdParam() default "";

    /**
     * 로그 설명 템플릿
     * {paramName} 형식의 플레이스홀더를 사용하여 메서드 파라미터 값을 참조할 수 있습니다.
     * {entityId}는 자동으로 엔티티 ID 값으로 치환됩니다.
     * 예: "사용자 차단: userId={userId}", "프롬프트 삭제: promptId={promptId}, entityId={entityId}"
     */
    String description() default "";

    /**
     * 변경 전 상태를 추출할 파라미터 이름
     * 이 파라미터의 값이 beforeState로 기록됩니다.
     */
    String beforeStateParam() default "";

    /**
     * 변경 후 상태를 추출할 파라미터 이름 또는 반환값 사용 여부
     * 파라미터 이름을 지정하면 해당 파라미터를 사용하고,
     * "return"으로 지정하면 메서드 반환값을 사용합니다.
     */
    String afterStateParam() default "";

    /**
     * adminId를 추출할 파라미터 이름
     * 관리자 작업의 경우 관리자 ID를 지정할 수 있습니다.
     * 지정하지 않으면 SecurityContext에서 현재 사용자를 사용합니다.
     */
    String adminIdParam() default "";
}

